package fun.Fear.main.listener.impl;

import fun.Fear.utils.interactions.inv.InventoryFlowManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import fun.Fear.utils.client.managers.event.EventHandler;
import fun.Fear.utils.client.managers.api.draggable.AbstractDraggable;
import fun.Fear.utils.client.packet.network.Network;
import fun.Fear.Fear;
import fun.Fear.main.listener.Listener;
import fun.Fear.events.item.UsingItemEvent;
import fun.Fear.events.packet.PacketEvent;
import fun.Fear.events.player.TickEvent;

public class EventListener implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        Network.tick();
        Fear.getInstance().getAttackPerpetrator().tick();
        InventoryFlowManager.tick();
        Fear.getInstance().getDraggableRepository().draggable().forEach(AbstractDraggable::tick);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClientCommandC2SPacket command -> serverSprint = switch (command.getMode()) {
                case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                default -> serverSprint;
            };
            case UpdateSelectedSlotC2SPacket slot -> selectedSlot = slot.getSelectedSlot();
            default -> {}
        }
        Network.packet(e);
        Fear.getInstance().getAttackPerpetrator().onPacket(e);
        Fear.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.packet(e));
    }

    @EventHandler
    public void onUsingItemEvent(UsingItemEvent e) {
        Fear.getInstance().getAttackPerpetrator().onUsingItem(e);
    }
}
