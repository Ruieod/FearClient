package fun.dungeonbot.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public final class ChestGeometry {
    private ChestGeometry() {
    }

    public static Optional<BlockPos> partner(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return Optional.empty();
        if (state.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) return Optional.empty();
        return Optional.of(pos.offset(ChestBlock.getFacing(state)));
    }
}
