package com.github.cactuspuppy.d2bc.discord.command;

import com.github.cactuspuppy.d2bc.D2BC;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class Ping extends BaseD2BCCommand {

    @Override
    public boolean hasPermission(User user) {
        return true;
    }

    @Override
    public void processMessage(MessageReceivedEvent event) {
        TextChannel response = event.getTextChannel();
        if (!response.canTalk()) {
            D2BC.getPlugin().getLogger().fine("Cannot respond to ping command in channel");
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        long responseTime = event.getMessage().getTimeCreated().until(now, ChronoUnit.MILLIS);
        MessageBuilder msgBuilder = new MessageBuilder("Pong! Latency: " + responseTime + "ms");
        MessageAction action = msgBuilder.sendTo(response);
        long sent = System.currentTimeMillis();
        action.queue(message -> message.editMessageFormat("Pong! Latency: %dms | RTT: %dms",
                responseTime, System.currentTimeMillis() - sent
        ).queue());
    }

    @Override
    public String getMainName() {
        return "ping";
    }
}
