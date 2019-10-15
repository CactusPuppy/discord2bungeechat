package com.github.cactuspuppy.d2bc.discord.command;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class CommandHub implements EventListener {
    private static Map<String, D2BCCommand> handlerMap = new HashMap<>();

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
    public void onEvent(@Nonnull GenericEvent genericEvent) {
        if (genericEvent instanceof MessageReceivedEvent) {

        }
    }
}
