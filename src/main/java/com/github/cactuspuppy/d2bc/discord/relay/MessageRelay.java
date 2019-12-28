package com.github.cactuspuppy.d2bc.discord.relay;

import com.github.cactuspuppy.d2bc.D2BC;
import com.github.cactuspuppy.d2bc.account.AccountManager;
import com.github.cactuspuppy.d2bc.utils.Pair;
import dev.aura.bungeechat.api.BungeeChatApi;
import dev.aura.bungeechat.api.account.BungeeChatAccount;
import dev.aura.bungeechat.api.placeholder.BungeeChatContext;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.logging.Level;

import javax.annotation.Nonnull;

public class MessageRelay extends ListenerAdapter {
    private String channelID;

    public MessageRelay() {
        super();
        this.channelID = D2BC.getPlugin().getConfig().getOrDefault("discord.channelID", null);
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().info("Failed to validate Discord Channel ID: " + result.getSecond());
        }
    }

    /**
     * Validates and fetches the text channel which the bot
     * @return A pair of the TextChannel on success,
     * or a String explaining the problem
     */
    private Pair<TextChannel, String> fetchChannel() {
        TextChannel channel = D2BC.getPlugin().getJda().getTextChannelById(channelID);
        if (channel == null) {
            return new Pair<>(null, "No text channel found with that ID");
        } else if (channel.canTalk()) {
            return new Pair<>(null, "Cannot send messages to this channel");
        }
        return new Pair<>(channel, "");
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().log(Level.FINE, "Could not relay Discord message: " + result.getSecond());
            return;
        }
        // AccountManager
    }
}
