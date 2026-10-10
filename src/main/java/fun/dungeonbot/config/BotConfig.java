package fun.dungeonbot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BotConfig {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path path = FabricLoader.getInstance().getConfigDir().resolve("dungeonbot.json");
    private static BotConfig instance = new BotConfig();

    public String rtpCommand = "/rtpfar";
    public String homeCommand = "/home q";
    public List<String> itemIds = new ArrayList<>(List.of("minecraft:golden_apple", "minecraft:enchanted_golden_apple"));
    public List<String> nameKeywords = new ArrayList<>(List.of("ключ"));
    public int dungeonChestRadius = 6;
    public int depositRadius = 12;
    public double reach = 4.0;
    public int clickDelayTicks = 3;
    public int containerSyncTicks = 6;
    public int teleportTimeoutSeconds = 25;
    public int teleportMinDistance = 48;
    public int settleSeconds = 4;
    public int rtpCooldownSeconds = 80;
    public int exploreStepBlocks = 96;
    public int rescanIntervalTicks = 40;
    public int pathTimeoutSeconds = 120;
    public boolean showHud = true;

    public static BotConfig get() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            BotConfig loaded = gson.fromJson(reader, BotConfig.class);
            if (loaded != null) instance = loaded;
        } catch (IOException | RuntimeException exception) {
            instance = new BotConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                gson.toJson(instance, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
