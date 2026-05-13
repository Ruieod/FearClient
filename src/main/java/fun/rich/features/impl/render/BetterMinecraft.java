package fun.Fear.features.impl.render;

import antidaunleak.api.annotation.Native;
import fun.Fear.events.packet.PacketEvent;
import fun.Fear.features.impl.combat.Aura;
import fun.Fear.features.module.Module;
import fun.Fear.features.module.ModuleCategory;
import fun.Fear.features.module.setting.implement.BooleanSetting;
import fun.Fear.utils.client.Instance;
import fun.Fear.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetterMinecraft extends Module {

    public static BetterMinecraft getInstance() {
        return Instance.get(BetterMinecraft.class);
    }

    BooleanSetting betterButton = new BooleanSetting("Кастомные кнопки", "язаипалсяэтопаститьспасите")
            .setValue(true);
    BooleanSetting tabVanishButton = new BooleanSetting("Спектаторы в табе", "язаипалсяэтопаститьспасите")
            .setValue(true);

    public BetterMinecraft() {
        super("BetterMinecraft", "Better Minecraft", ModuleCategory.RENDER);
        setup(betterButton, tabVanishButton);
    }

}
