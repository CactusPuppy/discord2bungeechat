package com.github.cactuspuppy.d2bc.discord.command;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;

public class CommandHub extends ListenerAdapter {
    private static Map<String, D2BCCommand> handlerMap = new HashMap<>();
    @Getter @Setter
    private static String prefix = ";";

    /**
     * Register a command to the hub. Returns false if a name is already taken
     * @param name Name to call this command by (case-insensitive)
     * @param handler The command handler
     * @return Whether the handler was successfully registered
     */
    public static boolean registerCommand(String name, D2BCCommand handler) {
        return handlerMap.putIfAbsent(name.toLowerCase(), handler) == null;
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        // Ignore other bots
        if (e.getAuthor().isBot() || e.getAuthor().isFake()) {

        }
        String message = e.getMessage().getContentStripped();
        if (!message.startsWith(prefix)) {
            return;
        }
        String commandName = message.substring(prefix.length(), (!message.contains(" ") ? message.length() : message.indexOf(" ")));
        D2BCCommand handler = handlerMap.get(commandName);
        if (handler == null) {
            MessageBuilder messageBuilder = new MessageBuilder();
            messageBuilder.append(e.getAuthor());
            messageBuilder.appendCodeLine(";" + message);
        }
    }
}
