package fun.dungeonbot;

import fun.dungeonbot.bot.DungeonBot;
import fun.dungeonbot.config.BotConfig;
import fun.dungeonbot.screen.BotScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class DungeonBotMod implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;
    private static boolean openMenuNextTick;

    @Override
    public void onInitializeClient() {
        BotConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dungeonbot.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.dungeonbot"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dungeonbot.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.dungeonbot"));

        ClientTickEvents.END_CLIENT_TICK.register(DungeonBotMod::onTick);
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!BotConfig.get().showHud || client.options.hudHidden) return;
            context.drawTextWithShadow(client.textRenderer, Text.literal(DungeonBot.get().status()), 4, 4, DungeonBot.get().isRunning() ? 0x55FF55 : 0xAAAAAA);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("dbot")
            .then(ClientCommandManager.literal("start").executes(context -> {
                DungeonBot.get().start();
                return 1;
            }))
            .then(ClientCommandManager.literal("stop").executes(context -> {
                DungeonBot.get().stop();
                return 1;
            }))
            .then(ClientCommandManager.literal("status").executes(context -> {
                context.getSource().sendFeedback(Text.literal(DungeonBot.get().status()));
                return 1;
            }))
            .then(ClientCommandManager.literal("reload").executes(context -> {
                BotConfig.load();
                context.getSource().sendFeedback(Text.literal("[DungeonBot] конфиг перезагружен"));
                return 1;
            }))
            .then(ClientCommandManager.literal("forget").executes(context -> {
                DungeonBot.get().forgetLooted();
                context.getSource().sendFeedback(Text.literal("[DungeonBot] список пройденных сундуков очищен"));
                return 1;
            }))
            .then(ClientCommandManager.literal("menu").executes(context -> {
                openMenuNextTick = true;
                return 1;
            }))));
    }

    private static void onTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) DungeonBot.get().toggle();
        while (menuKey.wasPressed()) openMenuNextTick = true;

        if (openMenuNextTick) {
            openMenuNextTick = false;
            client.setScreen(new BotScreen());
        }

        DungeonBot.get().tick(client);
    }
}
