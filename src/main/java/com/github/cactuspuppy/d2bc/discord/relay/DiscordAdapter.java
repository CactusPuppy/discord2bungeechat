package com.github.cactuspuppy.d2bc.discord.relay;

import com.github.cactuspuppy.d2bc.D2BC;
import com.github.cactuspuppy.d2bc.discord.command.DiscordCommandHub;
import com.github.cactuspuppy.d2bc.utils.Pair;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class DiscordAdapter extends ListenerAdapter {
    /**
     * The ID of the Discord channel to listen to
     */
    private String channelID;

    /**
     * Whether local chat should get relayed to Discord
     */
    @Getter @Setter
    private boolean relayLocalChat;

    public DiscordAdapter() {
        super();
        this.channelID = D2BC.getPlugin().getConfig().getOrDefault("discord.channelID", null);
        if (this.channelID == null || this.channelID.equals("<CHANGE ME>")) {
            D2BC.getPlugin().getLogger().warning("Channel ID not provided in config! " +
                    "Please set discord.channelID to the channel that you would like to relay chat to.");
            this.channelID = null;
            return;
        }
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().info("Failed to validate Discord Channel ID: " + result.getSecond());
        }
    }

    /**
     * Validates and fetches the text channel which the bot uses
     * @return A pair of the TextChannel on success or null ,
     * and a String explaining the error if any
     */
    private Pair<TextChannel, String> fetchChannel() {
        if (channelID == null) {
            D2BC.getPlugin().getLogger().warning("No Channel ID set");
        }
        TextChannel channel = D2BC.getPlugin().getJda().getTextChannelById(channelID);
        if (channel == null) {
            return new Pair<>(null, "No text channel found with that ID");
        } else if (!channel.canTalk()) {
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
        if (message.startsWith(DiscordCommandHub.getPrefix())) {
            // Let command handler handle it
            return;
        }
        if (event.getAuthor().isBot()) {
            //Ignore bots
            return;
        }
        if (!result.getFirst().getId().equals(event.getChannel().getId())) {
            // Not the channel to listen to
            return;
        }
        // Relay message
        D2BC.getPlugin().getBungeeChatAdapter().relayDiscordMessage(event);
    }

    /**
     * Relays a BungeeChat message extracted from logs
     * @param type The type of the event, usually LOCAL/GLOBAL/JOIN/LEAVE
     * @param server The server this event originated from
     * @param user The user who caused this event to happen
     * @param message The resulting chat message from the event
     */
    public void relayBungeeChatMessage(String type, String server, String user, String message) {
        switch (type) {
            case "LOCAL":
                if (relayLocalChat) {
                    relayChatMessage(server, user, message);
                }
                break;
            case "GLOBAL":
                relayChatMessage(server, user, message);
                break;
            case "JOIN":
                relayJoinEvent(server, user);
                break;
            case "LEAVE":
                relayLeaveEvent(user);
                break;
            case "SWITCH":
                relaySwitchEvent(server, user);
            default:
                D2BC.getPlugin().getLogger().warning(String.format("Unknown BungeeChat event: %s", type));
        }
    }

    private void relaySwitchEvent(String server, String user) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().warning("Could not relay BungeeChat server switch message due to JDA error: " + result.getSecond());
            return;
        }
        MessageBuilder builder = new MessageBuilder();
        builder.append("➡ ");
        builder.append(String.format("%s ", user), MessageBuilder.Formatting.BOLD);
        builder.append("has moved to the ");
        builder.append(String.format("%s ", server), MessageBuilder.Formatting.BOLD);
        builder.append("server");
        builder.stripMentions(D2BC.getPlugin().getJda());
        result.getFirst().sendMessage(builder.build()).queue();
    }

    private void relayChatMessage(String server, String user, String message) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().warning("Could not relay BungeeChat chat message due to JDA error: " + result.getSecond());
            return;
        }
        MessageBuilder builder = new MessageBuilder();
        builder.append(String.format("[%1$s] %2$s > ", server, user), MessageBuilder.Formatting.BOLD);
        builder.append(message);
        builder.stripMentions(D2BC.getPlugin().getJda());
        result.getFirst().sendMessage(builder.build()).queue();
    }

    private void relayJoinEvent(String server, String user) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().warning("Could not relay BungeeChat join message due to JDA error: " + result.getSecond());
            return;
        }
        MessageBuilder builder = new MessageBuilder();
        builder.append("\uD83C\uDF10 ");
        builder.append(String.format("%s ", user), MessageBuilder.Formatting.BOLD);
        builder.append("has joined the ");
        builder.append(String.format("%s ", server), MessageBuilder.Formatting.BOLD);
        builder.append(" server");
        builder.stripMentions(D2BC.getPlugin().getJda());
        result.getFirst().sendMessage(builder.build()).queue();
    }

    private void relayLeaveEvent(String user) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().warning("Could not relay BungeeChat leave message due to JDA error: " + result.getSecond());
            return;
        }
        MessageBuilder builder = new MessageBuilder();
        builder.append("\uD83D\uDCED ");
        builder.append(String.format("%s ", user), MessageBuilder.Formatting.BOLD);
        builder.append("has left the server");
        builder.stripMentions(D2BC.getPlugin().getJda());
        result.getFirst().sendMessage(builder.build()).queue();
    }
}
