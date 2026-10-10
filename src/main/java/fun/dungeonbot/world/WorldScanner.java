package fun.dungeonbot.world;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class WorldScanner {
    private WorldScanner() {
    }

    public static List<Dungeon> findDungeons(ClientWorld world, BlockPos origin, int chunkRadius, int chestRadius, Set<BlockPos> ignored) {
        List<BlockPos> spawners = new ArrayList<>();
        List<BlockPos> chests = new ArrayList<>();

        forEachBlockEntity(world, origin, chunkRadius, blockEntity -> {
            if (blockEntity instanceof MobSpawnerBlockEntity) spawners.add(blockEntity.getPos());
            else if (blockEntity instanceof ChestBlockEntity) chests.add(blockEntity.getPos());
        });

        List<Dungeon> dungeons = new ArrayList<>();
        Set<BlockPos> claimed = new HashSet<>(ignored);

        for (BlockPos spawner : spawners) {
            List<BlockPos> roomChests = new ArrayList<>();
            for (BlockPos chest : chests) {
                if (claimed.contains(chest) || !isInsideRoom(spawner, chest, chestRadius)) continue;

                claimed.add(chest);
                ChestGeometry.partner(world, chest).ifPresent(claimed::add);
                roomChests.add(chest);
            }
            if (!roomChests.isEmpty()) dungeons.add(new Dungeon(spawner, roomChests));
        }

        dungeons.sort(Comparator.comparingDouble(dungeon -> dungeon.spawner().getSquaredDistance(origin)));
        return dungeons;
    }

    public static List<BlockPos> findStorage(ClientWorld world, BlockPos origin, int radius) {
        List<BlockPos> storage = new ArrayList<>();
        Set<BlockPos> claimed = new HashSet<>();
        double radiusSquared = (double) radius * radius;
        int chunkRadius = (radius >> 4) + 1;

        forEachBlockEntity(world, origin, chunkRadius, blockEntity -> {
            BlockPos pos = blockEntity.getPos();
            if (pos.getSquaredDistance(origin) > radiusSquared || claimed.contains(pos)) return;
            if (!(blockEntity instanceof ChestBlockEntity) && !(blockEntity instanceof BarrelBlockEntity)) return;

            claimed.add(pos);
            ChestGeometry.partner(world, pos).ifPresent(claimed::add);
            storage.add(pos);
        });

        storage.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(origin)));
        return storage;
    }

    private static boolean isInsideRoom(BlockPos spawner, BlockPos chest, int radius) {
        return Math.abs(chest.getX() - spawner.getX()) <= radius && Math.abs(chest.getZ() - spawner.getZ()) <= radius && Math.abs(chest.getY() - spawner.getY()) <= 2;
    }

    private static void forEachBlockEntity(ClientWorld world, BlockPos origin, int chunkRadius, Consumer<BlockEntity> consumer) {
        ChunkPos center = new ChunkPos(origin);

        for (int chunkX = center.x - chunkRadius; chunkX <= center.x + chunkRadius; chunkX++) {
            for (int chunkZ = center.z - chunkRadius; chunkZ <= center.z + chunkRadius; chunkZ++) {
                if (!world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;

                WorldChunk chunk = world.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : List.copyOf(chunk.getBlockEntities().values())) consumer.accept(blockEntity);
            }
        }
    }
}
