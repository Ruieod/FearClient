package fun.dungeonbot.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class ChatSender {
    private ChatSender() {
    }

    public static void send(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        String trimmed = message.trim();
        if (player == null || trimmed.isEmpty()) return;

        client.inGameHud.getChatHud().addToMessageHistory(trimmed);

        if (trimmed.startsWith("/")) player.networkHandler.sendChatCommand(trimmed.substring(1));
        else player.networkHandler.sendChatMessage(trimmed);
    }
}
