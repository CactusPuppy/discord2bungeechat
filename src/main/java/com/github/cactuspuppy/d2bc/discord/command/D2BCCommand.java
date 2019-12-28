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
     * Take action based on the provided messasge. All feedback
     * and second-hand effects (including deleting messages) except
     * permission checking should be generated within this method.
     * @param event MessageReceivedEvent to process
     */
    void processMessage(MessageReceivedEvent event);

    /**
     * Get the main name of this command. The main name of the command is
     * used as follows: "/discord &lt;name&gt; &lt;args&gt;"
     * @return the main name of the command
     */
    String getMainName();
}
