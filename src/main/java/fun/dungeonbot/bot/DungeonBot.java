package fun.dungeonbot.bot;

import fun.dungeonbot.config.BotConfig;
import fun.dungeonbot.inventory.ContainerTransfer;
import fun.dungeonbot.inventory.ItemFilter;
import fun.dungeonbot.inventory.TransferMode;
import fun.dungeonbot.pathing.Pathfinder;
import fun.dungeonbot.pathing.Pathfinders;
import fun.dungeonbot.player.ChatSender;
import fun.dungeonbot.player.Rotations;
import fun.dungeonbot.world.ChestGeometry;
import fun.dungeonbot.world.Dungeon;
import fun.dungeonbot.world.WorldScanner;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class DungeonBot {
    private static final DungeonBot instance = new DungeonBot();

    private final Set<BlockPos> lootedChests = new HashSet<>();
    private final Deque<BlockPos> containerQueue = new ArrayDeque<>();

    private BotState state = BotState.idle;
    private Runnable afterWait;
    private int stateTicks;
    private int waitTicks;
    private Vec3d teleportOrigin;
    private String teleportDimension;
    private Runnable afterTeleport;
    private boolean teleportingHome;
    private boolean respawnRequested;
    private long lastRtpMillis;
    private TransferMode mode;
    private ContainerTransfer transfer;
    private BlockPos currentContainer;
    private int pathAttempts;
    private int openAttempts;
    private int dungeonsLooted;
    private int cycles;

    public static DungeonBot get() {
        return instance;
    }

    public boolean isRunning() {
        return state != BotState.idle;
    }

    public BotState state() {
        return state;
    }

    public String status() {
        if (!isRunning()) return "DungeonBot: " + state.label;
        String status = "DungeonBot: " + state.label + " | данжей: " + dungeonsLooted + " | циклов: " + cycles;
        int remainingTicks = rtpCooldownRemainingTicks();
        if (remainingTicks <= 0) return status;
        return status + " | rtp через " + remainingTicks / 20 + "с";
    }

    public void toggle() {
        if (isRunning()) stop();
        else start();
    }

    public void start() {
        if (isRunning()) return;

        if (Pathfinders.get() == null) {
            notify(Text.literal("Baritone не найден. Поставь Baritone (или Meteor) для 1.21.4.").formatted(Formatting.RED));
            return;
        }

        dungeonsLooted = 0;
        cycles = 0;
        lastRtpMillis = 0;
        notify(Text.literal("Запущен.").formatted(Formatting.GREEN));
        beginRtp();
    }

    public void stop() {
        if (!isRunning()) return;

        Pathfinder pathfinder = Pathfinders.get();
        if (pathfinder != null) pathfinder.stop();

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && ContainerTransfer.openContainer(player) != null) player.closeHandledScreen();

        containerQueue.clear();
        setState(BotState.idle);
        notify(Text.literal("Остановлен.").formatted(Formatting.YELLOW));
    }

    public void forgetLooted() {
        lootedChests.clear();
    }

    public void tick(MinecraftClient client) {
        if (!isRunning()) return;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            containerQueue.clear();
            setState(BotState.idle);
            return;
        }

        if (player.isDead()) {
            if (!respawnRequested) handleDeath(client, player);
            return;
        }

        respawnRequested = false;

        stateTicks++;

        switch (state) {
            case cooldown -> tickCooldown();
            case teleporting -> tickTeleporting(player, world);
            case settling -> tickCooldown();
            case scanning -> tickScanning(player, world);
            case exploring -> tickExploring(player, world);
            case next_container -> tickNextContainer(player, world);
            case walking -> tickWalking(player, world);
            case opening -> tickOpening(client, player);
            case transferring -> tickTransferring(client, player);
            default -> {
            }
        }
    }

    private void beginRtp() {
        int remainingTicks = rtpCooldownRemainingTicks();
        if (remainingTicks > 0) {
            waitThen(BotState.cooldown, remainingTicks, this::beginRtp);
            return;
        }

        cycles++;
        lastRtpMillis = System.currentTimeMillis();
        teleportingHome = false;
        sendTeleport(BotConfig.get().rtpCommand, () -> setState(BotState.scanning));
    }

    private void beginHome() {
        teleportingHome = true;
        sendTeleport(BotConfig.get().homeCommand, this::beginDeposit);
    }

    private void sendTeleport(String command, Runnable onArrival) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        teleportOrigin = player.getPos();
        teleportDimension = client.world.getRegistryKey().getValue().toString();
        afterTeleport = onArrival;
        ChatSender.send(command);
        setState(BotState.teleporting);
    }

    private void tickTeleporting(ClientPlayerEntity player, ClientWorld world) {
        BotConfig config = BotConfig.get();
        boolean changedDimension = !world.getRegistryKey().getValue().toString().equals(teleportDimension);
        boolean movedFar = player.getPos().squaredDistanceTo(teleportOrigin) > (double) config.teleportMinDistance * config.teleportMinDistance;

        if (changedDimension || movedFar) {
            waitThen(BotState.settling, config.settleSeconds * 20, afterTeleport);
            return;
        }

        if (stateTicks < config.teleportTimeoutSeconds * 20) return;

        if (teleportingHome) {
            afterTeleport.run();
            return;
        }

        notify(Text.literal("Телепорт не сработал, повторю после кд.").formatted(Formatting.GRAY));
        beginRtp();
    }

    private void tickCooldown() {
        if (stateTicks < waitTicks) return;

        Runnable next = afterWait;
        afterWait = null;
        if (next != null) next.run();
    }

    private void tickScanning(ClientPlayerEntity player, ClientWorld world) {
        List<Dungeon> dungeons = scanDungeons(player, world);

        if (dungeons.isEmpty()) {
            beginExploring();
            return;
        }

        queueDungeons(dungeons);
    }

    private void beginExploring() {
        setState(BotState.exploring);
        exploreStep(MinecraftClient.getInstance().player);
    }

    private void tickExploring(ClientPlayerEntity player, ClientWorld world) {
        Pathfinder pathfinder = Pathfinders.get();

        if (rtpCooldownRemainingTicks() <= 0) {
            pathfinder.stop();
            beginRtp();
            return;
        }

        if (stateTicks % Math.max(1, BotConfig.get().rescanIntervalTicks) == 0) {
            List<Dungeon> dungeons = scanDungeons(player, world);
            if (!dungeons.isEmpty()) {
                pathfinder.stop();
                queueDungeons(dungeons);
                return;
            }
        }

        if (stateTicks > 20 && !pathfinder.isActive()) exploreStep(player);
    }

    private void exploreStep(ClientPlayerEntity player) {
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
        int distance = BotConfig.get().exploreStepBlocks;
        int targetX = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int targetZ = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        Pathfinders.get().goTowards(targetX, targetZ);
    }

    private List<Dungeon> scanDungeons(ClientPlayerEntity player, ClientWorld world) {
        int chunkRadius = MinecraftClient.getInstance().options.getClampedViewDistance();
        return WorldScanner.findDungeons(world, player.getBlockPos(), chunkRadius, BotConfig.get().dungeonChestRadius, lootedChests);
    }

    private int rtpCooldownRemainingTicks() {
        long elapsedMillis = System.currentTimeMillis() - lastRtpMillis;
        long remainingMillis = BotConfig.get().rtpCooldownSeconds * 1000L - elapsedMillis;
        return remainingMillis <= 0 ? 0 : (int) (remainingMillis / 50L) + 1;
    }

    private void queueDungeons(List<Dungeon> dungeons) {
        containerQueue.clear();
        for (Dungeon dungeon : dungeons) containerQueue.addAll(dungeon.chests());

        notify(Text.literal("Найдено данжей: " + dungeons.size() + ", сундуков: " + containerQueue.size()).formatted(Formatting.AQUA));
        mode = TransferMode.take;
        setState(BotState.next_container);
    }

    private void beginDeposit() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        List<BlockPos> storage = WorldScanner.findStorage(world, player.getBlockPos(), BotConfig.get().depositRadius);

        if (storage.isEmpty()) {
            notify(Text.literal("Рядом с домом нет сундуков. Стоп.").formatted(Formatting.RED));
            stop();
            return;
        }

        containerQueue.clear();
        containerQueue.addAll(storage);
        mode = TransferMode.put;
        setState(BotState.next_container);
    }

    private void tickNextContainer(ClientPlayerEntity player, ClientWorld world) {
        if (mode == TransferMode.put && ItemFilter.countInInventory(player.getInventory()) == 0) {
            finishDeposit();
            return;
        }

        currentContainer = pollValidContainer(world);
        if (currentContainer == null) {
            finishQueue(player);
            return;
        }

        pathAttempts = 0;
        openAttempts = 0;

        if (isInReach(player, currentContainer)) {
            setState(BotState.opening);
            return;
        }

        Pathfinders.get().goNear(currentContainer);
        setState(BotState.walking);
    }

    private BlockPos pollValidContainer(ClientWorld world) {
        while (!containerQueue.isEmpty()) {
            BlockPos candidate = containerQueue.poll();
            BlockEntity blockEntity = world.getBlockEntity(candidate);
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) return candidate;
        }
        return null;
    }

    private void finishQueue(ClientPlayerEntity player) {
        if (mode == TransferMode.put) {
            notify(Text.literal("Сундуки дома забиты. Стоп.").formatted(Formatting.RED));
            stop();
            return;
        }

        dungeonsLooted++;

        if (ItemFilter.countInInventory(player.getInventory()) > 0) {
            beginHome();
            return;
        }

        notify(Text.literal("В данжах пусто, ищу дальше.").formatted(Formatting.GRAY));
        beginExploring();
    }

    private void finishDeposit() {
        notify(Text.literal("Лут сложен, rtp после кд.").formatted(Formatting.GREEN));
        beginRtp();
    }

    private void tickWalking(ClientPlayerEntity player, ClientWorld world) {
        Pathfinder pathfinder = Pathfinders.get();

        if (world.getBlockEntity(currentContainer) == null) {
            pathfinder.stop();
            skipContainer();
            return;
        }

        if (isInReach(player, currentContainer)) {
            pathfinder.stop();
            setState(BotState.opening);
            return;
        }

        if (stateTicks > BotConfig.get().pathTimeoutSeconds * 20) {
            pathfinder.stop();
            skipContainer();
            return;
        }

        if (stateTicks < 20 || pathfinder.isActive()) return;

        if (++pathAttempts > 3) {
            skipContainer();
            return;
        }

        pathfinder.goNear(currentContainer);
    }

    private void tickOpening(MinecraftClient client, ClientPlayerEntity player) {
        BotConfig config = BotConfig.get();

        if (stateTicks == 1) {
            Vec3d center = Vec3d.ofCenter(currentContainer);
            Rotations.lookAt(player, center);
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(center, Direction.UP, currentContainer, false));
            player.swingHand(Hand.MAIN_HAND);
            return;
        }

        if (ContainerTransfer.openContainer(player) != null) {
            if (stateTicks < config.containerSyncTicks) return;

            transfer = new ContainerTransfer(mode);
            setState(BotState.transferring);
            return;
        }

        if (stateTicks < 40) return;

        if (++openAttempts > 3) {
            skipContainer();
            return;
        }

        stateTicks = 0;
    }

    private void tickTransferring(MinecraftClient client, ClientPlayerEntity player) {
        GenericContainerScreenHandler container = ContainerTransfer.openContainer(player);
        if (container == null) {
            completeContainer();
            return;
        }

        if (stateTicks % Math.max(1, BotConfig.get().clickDelayTicks) != 0) return;
        if (transfer.step(client, container)) return;

        player.closeHandledScreen();
        completeContainer();
    }

    private void completeContainer() {
        if (mode == TransferMode.take) markLooted(currentContainer);
        setState(BotState.next_container);
    }

    private void skipContainer() {
        if (mode == TransferMode.take) markLooted(currentContainer);
        setState(BotState.next_container);
    }

    private void markLooted(BlockPos pos) {
        lootedChests.add(pos);
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) ChestGeometry.partner(world, pos).ifPresent(lootedChests::add);
    }

    private void handleDeath(MinecraftClient client, ClientPlayerEntity player) {
        respawnRequested = true;
        Pathfinders.get().stop();
        containerQueue.clear();
        player.requestRespawn();
        client.setScreen(null);
        notify(Text.literal("Умер, начинаю заново.").formatted(Formatting.RED));
        waitThen(BotState.cooldown, 60, this::beginRtp);
    }

    private boolean isInReach(ClientPlayerEntity player, BlockPos pos) {
        double reach = BotConfig.get().reach;
        return player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= reach * reach;
    }

    private void waitThen(BotState waitState, int ticks, Runnable next) {
        waitTicks = ticks;
        afterWait = next;
        setState(waitState);
    }

    private void setState(BotState next) {
        state = next;
        stateTicks = 0;
    }

    private static void notify(Text text) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(Text.literal("[DungeonBot] ").formatted(Formatting.DARK_AQUA).append(text), false);
    }
}
