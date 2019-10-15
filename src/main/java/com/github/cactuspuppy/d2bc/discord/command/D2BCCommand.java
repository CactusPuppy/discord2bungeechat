package com.github.cactuspuppy.d2bc.discord.command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface D2BCCommand {
    /**
     * @param user User to check permissions for
     * @return Whether the user has permission to use this command
     */
    boolean hasPermission(User user);

    /**
     * Take action based on the provided messasge. This method should provide
     * @param event MessageReceivedEvent to process
     * @return Whether the command successfully executed, or whether there was a syntax error (wrong type of argument, invalid state, etc.)
     */
    boolean processMessage(MessageReceivedEvent event);

}
