// external read-only CS2 ESP: RPM + GDI layered overlay
// offsets are loaded at startup from cs2-dumper output (offsets.json, client_dll.json) next to the exe
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <tlhelp32.h>
#include <cstdint>
#include <cstdio>
#include <cmath>
#include <string>
#include <fstream>
#include <sstream>

namespace off {
    // offsets.json
    uintptr_t dwEntityList, dwLocalPlayerPawn, dwViewMatrix;
    // client_dll.json
    uintptr_t m_iHealth, m_iTeamNum, m_pGameSceneNode;   // C_BaseEntity
    uintptr_t m_vecAbsOrigin;                            // CGameSceneNode
    uintptr_t m_hPlayerPawn, m_iszPlayerName;            // CCSPlayerController / CBasePlayerController
    // entity list layout: chunks of 0x200 identities, identity stride below
    constexpr uintptr_t kIdentityStride = 0x78;          // FILL: re-check after major patches (CEntityIdentity size)
}

struct Vec3 { float x, y, z; };
struct Vec2 { float x, y; };

static HANDLE    g_proc;
static uintptr_t g_client;
static bool      g_box = true, g_health = true, g_name = true, g_dist = true, g_team = true;

template <class T> T rd(uintptr_t a) {
    T v{};
    ReadProcessMemory(g_proc, reinterpret_cast<LPCVOID>(a), &v, sizeof(T), nullptr);
    return v;
}

// --- dumper json lookup: finds "key": <number> after an optional scope string ---
static std::string slurp(const char* path) {
    std::ifstream f(path, std::ios::binary);
    std::stringstream ss; ss << f.rdbuf();
    return ss.str();
}

static uintptr_t jsonFind(const std::string& j, const char* scope, const char* key) {
    size_t pos = 0;
    if (scope) {
        pos = j.find(std::string("\"") + scope + "\"");
        if (pos == std::string::npos) return 0;
    }
    pos = j.find(std::string("\"") + key + "\"", pos);
    if (pos == std::string::npos) return 0;
    pos = j.find(':', pos);
    if (pos == std::string::npos) return 0;
    return static_cast<uintptr_t>(std::strtoull(j.c_str() + pos + 1, nullptr, 10));
}

static bool loadOffsets() {
    std::string o = slurp("offsets.json"), c = slurp("client_dll.json");
    if (o.empty() || c.empty()) return false;
    off::dwEntityList      = jsonFind(o, "client.dll", "dwEntityList");
    off::dwLocalPlayerPawn = jsonFind(o, "client.dll", "dwLocalPlayerPawn");
    off::dwViewMatrix      = jsonFind(o, "client.dll", "dwViewMatrix");
    off::m_iHealth         = jsonFind(c, "C_BaseEntity", "m_iHealth");
    off::m_iTeamNum        = jsonFind(c, "C_BaseEntity", "m_iTeamNum");
    off::m_pGameSceneNode  = jsonFind(c, "C_BaseEntity", "m_pGameSceneNode");
    off::m_vecAbsOrigin    = jsonFind(c, "CGameSceneNode", "m_vecAbsOrigin");
    off::m_hPlayerPawn     = jsonFind(c, "CCSPlayerController", "m_hPlayerPawn");
    off::m_iszPlayerName   = jsonFind(c, "CBasePlayerController", "m_iszPlayerName");
    uintptr_t all[] = { off::dwEntityList, off::dwLocalPlayerPawn, off::dwViewMatrix, off::m_iHealth,
                        off::m_iTeamNum, off::m_pGameSceneNode, off::m_vecAbsOrigin, off::m_hPlayerPawn,
                        off::m_iszPlayerName };
    for (auto v : all) if (!v) return false;
    return true;
}

// --- process ---
static DWORD findPid(const wchar_t* name) {
    DWORD pid = 0;
    HANDLE s = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    PROCESSENTRY32W e{ sizeof(e) };
    for (BOOL ok = Process32FirstW(s, &e); ok; ok = Process32NextW(s, &e))
        if (!_wcsicmp(e.szExeFile, name)) { pid = e.th32ProcessID; break; }
    CloseHandle(s);
    return pid;
}

static uintptr_t findModule(DWORD pid, const wchar_t* name) {
    uintptr_t base = 0;
    HANDLE s = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    MODULEENTRY32W e{ sizeof(e) };
    for (BOOL ok = Module32FirstW(s, &e); ok; ok = Module32NextW(s, &e))
        if (!_wcsicmp(e.szModule, name)) { base = reinterpret_cast<uintptr_t>(e.modBaseAddr); break; }
    CloseHandle(s);
    return base;
}

// --- entity list ---
static uintptr_t entityByIndex(uintptr_t list, uint32_t idx) {
    uintptr_t chunk = rd<uintptr_t>(list + 8 * ((idx & 0x7FFF) >> 9) + 16);
    if (!chunk) return 0;
    return rd<uintptr_t>(chunk + off::kIdentityStride * (idx & 0x1FF));
}

// view matrix is row-major; clip = M * [x y z 1], NDC y is up so screen y is flipped
static bool w2s(const float m[16], const Vec3& p, int w, int h, Vec2& out) {
    float cw = m[12] * p.x + m[13] * p.y + m[14] * p.z + m[15];
    if (cw < 0.01f) return false;
    float cx = m[0] * p.x + m[1] * p.y + m[2] * p.z + m[3];
    float cy = m[4] * p.x + m[5] * p.y + m[6] * p.z + m[7];
    out.x = (w * 0.5f) * (1.f + cx / cw);
    out.y = (h * 0.5f) * (1.f - cy / cw);
    return true;
}

// --- overlay ---
static HWND g_wnd;

static LRESULT CALLBACK wndProc(HWND h, UINT m, WPARAM w, LPARAM l) {
    if (m == WM_DESTROY) { PostQuitMessage(0); return 0; }
    return DefWindowProcW(h, m, w, l);
}

static void drawFrame(HDC dc, int w, int h) {
    uintptr_t list  = rd<uintptr_t>(g_client + off::dwEntityList);
    uintptr_t local = rd<uintptr_t>(g_client + off::dwLocalPlayerPawn);
    if (!list || !local) return;

    float vm[16];
    ReadProcessMemory(g_proc, reinterpret_cast<LPCVOID>(g_client + off::dwViewMatrix), vm, sizeof(vm), nullptr);

    uint8_t localTeam = rd<uint8_t>(local + off::m_iTeamNum);
    Vec3 localPos = rd<Vec3>(rd<uintptr_t>(local + off::m_pGameSceneNode) + off::m_vecAbsOrigin);

    SetBkMode(dc, TRANSPARENT);
    HBRUSH nullBrush = static_cast<HBRUSH>(GetStockObject(NULL_BRUSH));
    HPEN enemyPen = CreatePen(PS_SOLID, 1, RGB(255, 60, 60));
    HPEN teamPen  = CreatePen(PS_SOLID, 1, RGB(60, 160, 255));
    HPEN outline  = CreatePen(PS_SOLID, 3, RGB(1, 1, 1));   // RGB(0,0,0) is the color key
    SelectObject(dc, nullBrush);

    for (uint32_t i = 1; i <= 64; ++i) {
        uintptr_t ctrl = entityByIndex(list, i);
        if (!ctrl) continue;
        uint32_t pawnHandle = rd<uint32_t>(ctrl + off::m_hPlayerPawn);
        if (!pawnHandle || pawnHandle == 0xFFFFFFFF) continue;
        uintptr_t pawn = entityByIndex(list, pawnHandle & 0x7FFF);
        if (!pawn || pawn == local) continue;

        int hp = rd<int>(pawn + off::m_iHealth);
        if (hp <= 0 || hp > 100) continue;
        uint8_t team = rd<uint8_t>(pawn + off::m_iTeamNum);
        bool enemy = team != localTeam;
        if (g_team && !enemy) continue;

        Vec3 feet = rd<Vec3>(rd<uintptr_t>(pawn + off::m_pGameSceneNode) + off::m_vecAbsOrigin);
        Vec3 head{ feet.x, feet.y, feet.z + 72.f };
        Vec2 sf, sh;
        if (!w2s(vm, feet, w, h, sf) || !w2s(vm, head, w, h, sh)) continue;

        float bh = sf.y - sh.y;
        if (bh < 2.f) continue;
        float bw = bh * 0.5f;
        int l = int(sh.x - bw / 2), t = int(sh.y), r = int(sh.x + bw / 2), b = int(sf.y);

        if (g_box) {
            SelectObject(dc, outline);
            Rectangle(dc, l, t, r, b);
            SelectObject(dc, enemy ? enemyPen : teamPen);
            Rectangle(dc, l, t, r, b);
        }
        if (g_health) {
            int fill = int(bh * hp / 100.f);
            RECT bg{ l - 6, t - 1, l - 2, b + 1 };
            RECT fg{ l - 5, b - fill, l - 3, b };
            HBRUSH bgb = CreateSolidBrush(RGB(1, 1, 1));
            HBRUSH fgb = CreateSolidBrush(RGB(255 - hp * 255 / 100, hp * 255 / 100, 0));
            FillRect(dc, &bg, bgb);
            FillRect(dc, &fg, fgb);
            DeleteObject(bgb); DeleteObject(fgb);
        }
        SetTextColor(dc, RGB(255, 255, 255));
        if (g_name) {
            char name[128]{};
            ReadProcessMemory(g_proc, reinterpret_cast<LPCVOID>(ctrl + off::m_iszPlayerName), name, sizeof(name) - 1, nullptr);
            SIZE sz; GetTextExtentPoint32A(dc, name, int(strlen(name)), &sz);
            TextOutA(dc, int(sh.x) - sz.cx / 2, t - sz.cy - 2, name, int(strlen(name)));
        }
        if (g_dist) {
            float dx = feet.x - localPos.x, dy = feet.y - localPos.y, dz = feet.z - localPos.z;
            char d[16]; int n = snprintf(d, sizeof(d), "%.0fm", std::sqrt(dx * dx + dy * dy + dz * dz) * 0.0254f);
            SIZE sz; GetTextExtentPoint32A(dc, d, n, &sz);
            TextOutA(dc, int(sh.x) - sz.cx / 2, b + 2, d, n);
        }
    }

    DeleteObject(enemyPen); DeleteObject(teamPen); DeleteObject(outline);
}

static void toggle(int vk, bool& flag, const char* name) {
    if (GetAsyncKeyState(vk) & 1) { flag = !flag; printf("%s: %s\n", name, flag ? "on" : "off"); }
}

int main() {
    if (!loadOffsets()) { puts("offsets.json / client_dll.json missing or incomplete (cs2-dumper output)"); return 1; }

    DWORD pid = 0;
    puts("waiting for cs2.exe...");
    while (!(pid = findPid(L"cs2.exe"))) Sleep(1000);
    g_proc = OpenProcess(PROCESS_VM_READ | PROCESS_QUERY_LIMITED_INFORMATION, FALSE, pid);
    if (!g_proc) { puts("OpenProcess failed"); return 1; }
    while (!(g_client = findModule(pid, L"client.dll"))) Sleep(1000);
    printf("client.dll @ %p\n", reinterpret_cast<void*>(g_client));
    puts("F1 box | F2 health | F3 name | F4 distance | F5 team check | END exit");

    WNDCLASSEXW wc{ sizeof(wc) };
    wc.lpfnWndProc = wndProc;
    wc.hInstance = GetModuleHandleW(nullptr);
    wc.lpszClassName = L"ov";
    RegisterClassExW(&wc);

    int sw = GetSystemMetrics(SM_CXSCREEN), sh = GetSystemMetrics(SM_CYSCREEN);
    g_wnd = CreateWindowExW(WS_EX_TOPMOST | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOOLWINDOW,
                            wc.lpszClassName, L"", WS_POPUP, 0, 0, sw, sh, nullptr, nullptr, wc.hInstance, nullptr);
    SetLayeredWindowAttributes(g_wnd, RGB(0, 0, 0), 0, LWA_COLORKEY);
    ShowWindow(g_wnd, SW_SHOW);

    HDC wdc = GetDC(g_wnd);
    HDC mem = CreateCompatibleDC(wdc);
    HBITMAP bmp = CreateCompatibleBitmap(wdc, sw, sh);
    SelectObject(mem, bmp);
    HBRUSH clear = static_cast<HBRUSH>(GetStockObject(BLACK_BRUSH));
    RECT full{ 0, 0, sw, sh };

    MSG msg{};
    while (!(GetAsyncKeyState(VK_END) & 1)) {
        while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) goto done;
            TranslateMessage(&msg); DispatchMessageW(&msg);
        }
        DWORD code = 0;
        if (!GetExitCodeProcess(g_proc, &code) || code != STILL_ACTIVE) break;

        toggle(VK_F1, g_box, "box");
        toggle(VK_F2, g_health, "health");
        toggle(VK_F3, g_name, "name");
        toggle(VK_F4, g_dist, "distance");
        toggle(VK_F5, g_team, "team check");

        FillRect(mem, &full, clear);
        HWND fg = GetForegroundWindow();
        if (fg == FindWindowW(nullptr, L"Counter-Strike 2")) drawFrame(mem, sw, sh);
        BitBlt(wdc, 0, 0, sw, sh, mem, 0, 0, SRCCOPY);
        Sleep(5);
    }
done:
    DeleteObject(bmp); DeleteDC(mem); ReleaseDC(g_wnd, wdc);
    DestroyWindow(g_wnd);
    CloseHandle(g_proc);
    return 0;
}
