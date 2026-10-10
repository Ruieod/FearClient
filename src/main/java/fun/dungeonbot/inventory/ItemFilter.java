package fun.dungeonbot.inventory;

import fun.dungeonbot.config.BotConfig;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Locale;

public final class ItemFilter {
    private ItemFilter() {
    }

    public static boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;

        BotConfig config = BotConfig.get();
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (config.itemIds.contains(itemId)) return true;

        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        for (String keyword : config.nameKeywords) {
            if (!keyword.isBlank() && name.contains(keyword.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public static int countInInventory(PlayerInventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.main.size(); slot++) {
            ItemStack stack = inventory.main.get(slot);
            if (matches(stack)) count += stack.getCount();
        }
        return count;
    }
}
