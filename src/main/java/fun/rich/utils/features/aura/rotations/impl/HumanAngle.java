package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.Rich;
import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HumanAngle extends RotateConstructor {
    private final SecureRandom random = new SecureRandom();

    public HumanAngle() {
        super("Human");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
        if (rotationDifference < 0.01F) return new Turns(currentAngle.getYaw(), currentAngle.getPitch());

        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        // ease-out: big flicks move fast, close to target it slows down like a hand settling
        float ease = MathHelper.clamp(rotationDifference / 60F, 0.18F, 1F);
        float speed = (canAttack ? randomLerp(0.75F, 0.9F) : randomLerp(0.35F, 0.55F)) * ease;
        float maxStep = canAttack ? randomLerp(90F, 140F) : randomLerp(25F, 45F);

        float lineYaw = Math.abs(yawDelta / rotationDifference) * maxStep;
        float linePitch = Math.abs(pitchDelta / rotationDifference) * maxStep * 0.7F;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw) * speed;
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) * speed * randomLerp(0.8F, 1F);

        // hand tremor, scaled down while hitting so it doesn't miss
        float noise = canAttack ? 0.15F : 0.6F;
        if (!aura.isState() || entity == null) noise = 0;
        moveYaw += (float) random.nextGaussian() * noise;
        movePitch += (float) random.nextGaussian() * noise * 0.5F;

        return new Turns(currentAngle.getYaw() + moveYaw, MathHelper.clamp(currentAngle.getPitch() + movePitch, -90F, 90F));
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.05, 0.05, 0.05);
    }
}
