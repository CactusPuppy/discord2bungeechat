package com.github.cactuspuppy.d2bc.discord.relay;

import com.github.cactuspuppy.d2bc.D2BC;
import com.github.cactuspuppy.d2bc.discord.command.CommandHub;
import com.github.cactuspuppy.d2bc.utils.Pair;
import dev.aura.bungeechat.api.account.AccountManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class MessageRelay extends ListenerAdapter {
    /**
     * The ID of the Discord channel to listen to
     */
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
            D2BC.getPlugin().getLogger().log(Level.WARNING, "Could not relay Discord message: " + result.getSecond());
            return;
        }
        String message = event.getMessage().getContentStripped();
        if (message.startsWith(CommandHub.getPrefix())) {
            // Let command handler handle it
            return;
        }
        if (!result.getFirst().getId().equals(channelID)) {
            // Not the channel to listen to
            return;
        }
        // MessageRelay
    }
}
