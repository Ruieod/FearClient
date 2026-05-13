package fun.Fear.features.impl.misc;

import fun.Fear.features.module.Module;
import fun.Fear.features.module.ModuleCategory;
import fun.Fear.Fear;
import fun.Fear.utils.client.chat.ChatMessage;

public class IRC extends Module {
    public IRC() {
        super("IRC", ModuleCategory.MISC);
    }

    @Override
    public void setState(boolean state) {
        super.setState(state);
        if (state) {
            activate();
        } else {
            deactivate();
        }
    }

    @Override
    public void activate() {
        Fear.getInstance().setShowIrcMessages(true);
        Fear.getInstance().getIrcManager().connect();
    }

    @Override
    public void deactivate() {
        Fear.getInstance().setShowIrcMessages(false);
        Fear.getInstance().getIrcManager().disconnect();
    }

    public void sendMessage(String message) {
        if (!isState()) {
            ChatMessage.ircmessageWithRed("Модуль IRC выключен");
            return;
        }
        if (Fear.getInstance().getIrcManager().getClient() != null && Fear.getInstance().getIrcManager().getClient().isOpen()) {
            Fear.getInstance().getIrcManager().getClient().sendMessage(message);
        }
    }
}