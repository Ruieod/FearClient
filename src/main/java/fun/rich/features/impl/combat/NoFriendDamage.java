package fun.Fear.features.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.Fear.common.repository.friend.FriendUtils;
import fun.Fear.features.module.Module;
import fun.Fear.features.module.ModuleCategory;
import fun.Fear.utils.client.managers.event.EventHandler;
import fun.Fear.events.player.AttackEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFriendDamage extends Module {
    public NoFriendDamage() {
        super("NoFriendDamage", "No Friend Damage", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        e.setCancelled(FriendUtils.isFriend(e.getEntity()));
    }
}

