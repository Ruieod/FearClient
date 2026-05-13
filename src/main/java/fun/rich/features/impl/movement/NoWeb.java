package fun.Fear.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.Fear.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;

import fun.Fear.utils.client.managers.event.EventHandler;
import fun.Fear.features.module.Module;
import fun.Fear.features.module.ModuleCategory;
import fun.Fear.features.module.setting.implement.SelectSetting;
import fun.Fear.utils.interactions.simulate.Simulations;
import fun.Fear.utils.client.Instance;
import fun.Fear.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoWeb extends Module {
    public static NoWeb getInstance() {
        return Instance.get(NoWeb.class);
    }

    public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода").value("Grim");

    public NoWeb() {
        super("NoWeb", "No Web", ModuleCategory.MOVEMENT);
        setup(webMode);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = Simulations.calculateDirection(0.35);
            mc.player.addVelocity(speed[0], 0, speed[1]);
            mc.player.velocity.y = mc.options.jumpKey.isPressed() ? 0.65f : mc.options.sneakKey.isPressed() ? -0.65f : 0;
        }
    }
}