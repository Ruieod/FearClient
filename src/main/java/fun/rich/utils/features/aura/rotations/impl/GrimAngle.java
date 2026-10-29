package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.Rich;
import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.math.calc.Calculate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class GrimAngle extends RotateConstructor {
    private final SecureRandom random = new SecureRandom();

    public GrimAngle() {
        super("Grim");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
        float gcd = (float) Calculate.computeGcd();
        if (rotationDifference < gcd) return new Turns(currentAngle.getYaw(), currentAngle.getPitch());

        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        // step stays well under 180 so yaw never wraps (AimModulo360), pitch lags yaw like a real mouse
        float maxStep = canAttack ? randomLerp(55F, 80F) : randomLerp(18F, 32F);
        float speed = canAttack ? randomLerp(0.7F, 0.85F) : randomLerp(0.3F, 0.45F);

        float lineYaw = Math.abs(yawDelta / rotationDifference) * maxStep;
        float linePitch = Math.abs(pitchDelta / rotationDifference) * maxStep * 0.65F;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw) * speed;
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) * speed;

        // sub-gcd step would round to a duplicate look after sensitivity fix — push one full gcd instead
        if (Math.abs(moveYaw) < gcd && Math.abs(yawDelta) >= gcd) moveYaw = Math.copySign(gcd, yawDelta);
        if (Math.abs(movePitch) < gcd && Math.abs(pitchDelta) >= gcd) movePitch = Math.copySign(gcd, pitchDelta);

        return new Turns(currentAngle.getYaw() + moveYaw, MathHelper.clamp(currentAngle.getPitch() + movePitch, -90F, 90F));
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.03, 0.03, 0.03);
    }
}
