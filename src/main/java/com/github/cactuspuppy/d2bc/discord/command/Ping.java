package com.github.cactuspuppy.d2bc.discord.command;

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
            //TODO: Logging
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        long responseTime = event.getMessage().getTimeCreated().until(now, ChronoUnit.MILLIS);
        MessageBuilder msgBuilder = new MessageBuilder("Pong! Response time: " + responseTime + " ms");
        MessageAction action = msgBuilder.sendTo(response);
        action.queue();
    }

    @Override
    public String getMainName() {
        return "ping";
    }
}
