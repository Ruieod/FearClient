package fun.Fear.common.discord.utils;

import com.sun.jna.Structure;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordFearPresence extends Structure {
    public String largeImageKey;
    public String largeImageText;
    public String smallImageText;
    public String partyPrivacy;
    public long startTimestamp;
    public int instance;
    public String partyId;
    public int partySize;
    public long endTimestamp;
    public String details;
    public String joinSecret;
    public String spectateSecret;
    public String smallImageKey;
    public String matchSecret;
    public String state;
    public int partyMax;
    public String button_url_1;
    public String button_label_1;
    public String button_url_2;
    public String button_label_2;

    public DiscordFearPresence() {
        this.setStringEncoding("UTF-8");
    }

    protected List<String> getFieldOrder() {
        return Arrays.asList("state", "details", "startTimestamp", "endTimestamp", "largeImageKey", "largeImageText", "smallImageKey", "smallImageText", "partyId", "partySize", "partyMax", "partyPrivacy", "matchSecret", "joinSecret", "spectateSecret", "button_label_1", "button_url_1", "button_label_2", "button_url_2", "instance");
    }

    public static class Builder {
        private final DiscordFearPresence FearPresence = new DiscordFearPresence();

        public Builder setSmallImage(String var1) {
            return this.setSmallImage(var1, "");
        }

        public Builder setDetails(String var1) {
            if (var1 != null && !var1.isEmpty()) {
                this.FearPresence.details = var1.substring(0, Math.min(var1.length(), 128));
            }

            return this;
        }

        public Builder setLargeImage(String var1, String var2) {
            this.FearPresence.largeImageKey = var1;
            this.FearPresence.largeImageText = var2;
            return this;
        }

        public Builder setState(String var1) {
            if (var1 != null && !var1.isEmpty()) {
                this.FearPresence.state = var1.substring(0, Math.min(var1.length(), 128));
            }

            return this;
        }

        public Builder setInstance(boolean var1) {
            if ((this.FearPresence.button_label_1 == null || !this.FearPresence.button_label_1.isEmpty()) && (this.FearPresence.button_label_2 == null || !this.FearPresence.button_label_2.isEmpty())) {
                this.FearPresence.instance = var1 ? 1 : 0;
            }
            return this;
        }

        public Builder setButtons(RPCButton var1) {
            return this.setButtons(Collections.singletonList(var1));
        }

        public Builder setSmallImage(String var1, String var2) {
            this.FearPresence.smallImageKey = var1;
            this.FearPresence.smallImageText = var2;
            return this;
        }


        public Builder setButtons(List<RPCButton> buttons) {
            if (buttons != null && !buttons.isEmpty()) {
                int var2 = Math.min(buttons.size(), 2);
                this.FearPresence.button_label_1 = buttons.get(0).getLabel();
                this.FearPresence.button_url_1 = buttons.get(0).getUrl();
                if (var2 == 2) {
                    this.FearPresence.button_label_2 = buttons.get(1).getLabel();
                    this.FearPresence.button_url_2 = buttons.get(1).getUrl();
                }
            }

            return this;
        }

        public Builder setStartTimestamp(OffsetDateTime var1) {
            this.FearPresence.startTimestamp = var1.toEpochSecond();
            return this;
        }

        public Builder setSecrets(String var1, String var2, String var3) {
            if ((this.FearPresence.button_label_1 == null || !this.FearPresence.button_label_1.isEmpty()) && (this.FearPresence.button_label_2 == null || !this.FearPresence.button_label_2.isEmpty())) {
                this.FearPresence.matchSecret = var1;
                this.FearPresence.joinSecret = var2;
                this.FearPresence.spectateSecret = var3;
            }
            return this;
        }

        public Builder setButtons(RPCButton var1, RPCButton var2) {
            this.setButtons(Arrays.asList(var1, var2));
            return this;
        }

        public Builder setStartTimestamp(long var1) {
            this.FearPresence.startTimestamp = var1;
            return this;
        }

        public Builder setSecrets(String var1, String var2) {
            if ((this.FearPresence.button_label_1 == null || !this.FearPresence.button_label_1.isEmpty()) && (this.FearPresence.button_label_2 == null || !this.FearPresence.button_label_2.isEmpty())) {
                this.FearPresence.joinSecret = var1;
                this.FearPresence.spectateSecret = var2;
            }
            return this;
        }

        public Builder setEndTimestamp(long var1) {
            this.FearPresence.endTimestamp = var1;
            return this;
        }

        public Builder setEndTimestamp(OffsetDateTime var1) {
            this.FearPresence.endTimestamp = var1.toEpochSecond();
            return this;
        }

        public Builder setLargeImage(String var1) {
            return this.setLargeImage(var1, "");
        }

        public DiscordFearPresence build() {
            return this.FearPresence;
        }
    }
}