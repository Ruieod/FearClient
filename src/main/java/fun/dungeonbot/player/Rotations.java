package fun.dungeonbot.player;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Rotations {
    private Rotations() {
    }

    public static void lookAt(ClientPlayerEntity player, Vec3d target) {
        Vec3d delta = target.subtract(player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        player.setYaw(MathHelper.wrapDegrees(yaw));
        player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }
}
