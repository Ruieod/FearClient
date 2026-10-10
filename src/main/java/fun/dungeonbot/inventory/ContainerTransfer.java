package fun.dungeonbot.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashSet;
import java.util.Set;

public final class ContainerTransfer {
    private final TransferMode mode;
    private final Set<Integer> touchedSlots = new HashSet<>();

    public ContainerTransfer(TransferMode mode) {
        this.mode = mode;
    }

    public static GenericContainerScreenHandler openContainer(ClientPlayerEntity player) {
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == player.playerScreenHandler) return null;
        if (handler instanceof GenericContainerScreenHandler container) return container;
        return null;
    }

    public boolean step(MinecraftClient client, GenericContainerScreenHandler container) {
        int containerSize = container.getRows() * 9;
        int first = mode == TransferMode.take ? 0 : containerSize;
        int last = mode == TransferMode.take ? containerSize : container.slots.size();

        for (int slot = first; slot < last; slot++) {
            if (touchedSlots.contains(slot)) continue;
            if (!ItemFilter.matches(container.getSlot(slot).getStack())) continue;

            touchedSlots.add(slot);
            client.interactionManager.clickSlot(container.syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
            return true;
        }
        return false;
    }
}
