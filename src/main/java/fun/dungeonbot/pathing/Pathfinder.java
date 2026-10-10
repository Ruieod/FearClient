package fun.dungeonbot.pathing;

import net.minecraft.util.math.BlockPos;

public interface Pathfinder {
    void goNear(BlockPos target);

    boolean isActive();

    void stop();
}
