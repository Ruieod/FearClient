package fun.dungeonbot.world;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public record Dungeon(BlockPos spawner, List<BlockPos> chests) {
}
