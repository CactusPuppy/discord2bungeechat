package com.github.cactuspuppy.d2bc.discord.relay;

import com.github.cactuspuppy.d2bc.D2BC;
import com.github.cactuspuppy.d2bc.bungeecord.playercount.PlayerCount;
import com.github.cactuspuppy.d2bc.discord.command.DiscordCommandHub;
import com.github.cactuspuppy.d2bc.utils.Pair;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.md_5.bungee.api.ChatColor;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            D2BC.getPlugin().getLogger().severe("Channel ID not provided in config! " +
                    "Please set discord.channelID to the channel that you would like to relay chat to.");
            this.channelID = null;
        }
    }

    /**
     * Validates and fetches the text channel which the bot uses
     * @return A pair of the TextChannel on success or null ,
     * and a String explaining the error if any
     */
    public Pair<TextChannel, String> fetchChannel() {
        if (channelID == null) {
            return new Pair<>(null, "No Channel ID set");
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
     * Updates the Discord channel to display the current player count.
     * <b>If rate limited, this is a blocking operation!</b>
     * @param playerCount Number of players to display, or -1 if rate limited.
     */
    public void updateDiscordPlayerCount(int playerCount) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().log(Level.WARNING, "Could not update Discord channel topic: " + result.getSecond());
            return;
        }
        //TODO: Figure out where to put this info
//        result.getFirst().getManager()
//                .setTopic(String.format("Players Online: %s", playerCount == -1 ? "?" : playerCount)).complete();
    }

    private CompletableFuture<Void> channelTopicUpdate = null;

    /**
     * Updates the Discord channel to report server status.
     */
    public void updateDiscordServerStatus(boolean online) {
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().log(Level.WARNING, "Could not update Discord channel topic: " + result.getSecond());
            return;
        }
        String topic = String.format("Server %s | IP: %s", online ? "Online" : "Offline",
                D2BC.getPlugin().getConfig().getOrDefault("discord.server-ip", "????"));
        synchronized (this) {
            updateOnlineChannelTopic(online, topic, result.getFirst());
        }
        try {
            result.getFirst().sendMessage(
                    new MessageBuilder(String.format("%s ", online ? ":green_circle:" : ":red_circle:"))
                            .append(String.format("Server %s", online ? "Online" : "Offline"), MessageBuilder.Formatting.BOLD)
                            .build()
            ).timeout(5, TimeUnit.SECONDS).complete();
        } catch (Exception e) {
            D2BC.getPlugin().getLogger().log(Level.WARNING, "Issue sending server power status message to Discord", e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateOnlineChannelTopic(boolean online, String topic, TextChannel channel) {
        if (channelTopicUpdate != null) {
            channelTopicUpdate.cancel(true);
            channelTopicUpdate = null;
        }
        if (channel.getTopic() != null && channel.getTopic().equals(topic)) {
            return;
        }
        if (online) {
            channelTopicUpdate = channel.getManager().setTopic(topic)
                    .timeout(15, TimeUnit.SECONDS).submit();
            channelTopicUpdate.whenComplete((v, error) -> {
                if (error instanceof TimeoutException) {
                    D2BC.getPlugin().getLogger().warning("Could not update channel topic within 15 seconds, deferring...");
                    D2BC.getPlugin().getProxy().getScheduler().schedule(D2BC.getPlugin(),
                            () -> this.updateOnlineChannelTopic(online, topic, channel), 30, TimeUnit.SECONDS);
                }
                channelTopicUpdate = null;
            });
        } else {
            try {
                channel.getManager().setTopic(topic)
                        .timeout(5, TimeUnit.SECONDS).complete();
            } catch (Exception e) {
                D2BC.getPlugin().getLogger().log(Level.WARNING, "Issue updating Discord to indicate server offline", e);
            }
        }
    }

    /**
     * Relays a BungeeChat message extracted from logs.
     * Performs all sanitization before passing to handlers.
     * @param type The type of the event, usually LOCAL/GLOBAL/JOIN/LEAVE
     * @param server The server this event originated from
     * @param user The user who caused this event to happen
     * @param message The resulting chat message from the event
     */
    public void relayBungeeChatMessage(String type, String server, String user, String message) {
        message = MarkdownSanitizer.escape(message);
        Pair<TextChannel, String> result = fetchChannel();
        if (result.getFirst() == null) {
            D2BC.getPlugin().getLogger().log(Level.WARNING, "Could not relay Discord message: " + result.getSecond());
            return;
        }
        String finalMessage = new MessageBuilder(message)
                .setAllowedMentions(Arrays.asList(Message.MentionType.EMOTE, Message.MentionType.CHANNEL))
                .build().getContentRaw();
        switch (type) {
            case "LOCAL":
                if (relayLocalChat) {
                    relayChatMessage(server, user, finalMessage);
                }
                break;
            case "GLOBAL":
                relayChatMessage(server, user, finalMessage);
                break;
            case "JOIN":
                D2BC.getPlugin().getProxy().getScheduler().schedule(D2BC.getPlugin(), () -> relayJoinEvent(server, user), 500, TimeUnit.MILLISECONDS);
                break;
            case "LEAVE":
                D2BC.getPlugin().getProxy().getScheduler().schedule(D2BC.getPlugin(), () -> relayLeaveEvent(user), 500, TimeUnit.MILLISECONDS);
                break;
            case "SWITCH":
                relaySwitchEvent(server, user);
                break;
            case "COMMAND":
                //Do nothing
                break;
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
        builder.append(ChatColor.stripColor(message));
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
        builder.append("server. ");
        int playerCount = PlayerCount.getReporter().numPlayersOnline();
        builder.appendFormat("[%d %s online]", playerCount, playerCount == 1 ? "player" : "players");
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
        builder.append("has left the server. ");
        int playerCount = PlayerCount.getReporter().numPlayersOnline();
        builder.appendFormat("[%d %s online]", playerCount, playerCount == 1 ? "player" : "players");
        result.getFirst().sendMessage(builder.build()).queue();
    }
}
