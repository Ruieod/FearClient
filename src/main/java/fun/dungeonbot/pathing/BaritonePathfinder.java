package fun.dungeonbot.pathing;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.util.math.BlockPos;

public final class BaritonePathfinder implements Pathfinder {
    private IBaritone baritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    @Override
    public void goNear(BlockPos target) {
        baritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(target));
    }

    @Override
    public void goTowards(int x, int z) {
        baritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
    }

    @Override
    public boolean isActive() {
        IBaritone baritone = baritone();
        return baritone.getCustomGoalProcess().isActive() || baritone.getPathingBehavior().isPathing();
    }

    @Override
    public void stop() {
        baritone().getPathingBehavior().cancelEverything();
    }
}
