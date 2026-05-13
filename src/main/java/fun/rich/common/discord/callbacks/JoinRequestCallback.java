package fun.Fear.common.discord.callbacks;

import com.sun.jna.Callback;
import fun.Fear.common.discord.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
    void apply(DiscordUser var1);
}