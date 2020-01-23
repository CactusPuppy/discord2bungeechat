package com.github.cactuspuppy.d2bc.discord.command;

import com.github.cactuspuppy.d2bc.D2BC;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DiscordCommandHub extends ListenerAdapter {
    private Map<String, D2BCCommand> handlerMap = new HashMap<>();
    @Getter @Setter
    private static String prefix = ";";

    public DiscordCommandHub() {
        reinitialize();
    }

    /**
     * Clears all current command handlers then adds all base commands.
     */
    void reinitialize() {
        handlerMap.clear();
        registerBaseCommand(new Ping());
    }

    /**
     * Register a command to the hub. Returns false if a name is already taken.
     * @param handler The command handler
     * @return Whether the handler was successfully registered
     */
    public boolean registerCommand(D2BCCommand handler) {
        return handlerMap.putIfAbsent(handler.getMainName().toLowerCase(), handler) == null;
    }

    void registerBaseCommand(BaseD2BCCommand command) {
        boolean result = registerCommand(command);
        if (!result) {
            D2BC.getPlugin().getLogger().warning("Could not register base command " + command.getMainName());
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        // Ignore other bots
        if (e.getAuthor().isBot() || e.getAuthor().isFake()) {
            return;
        }
        String message = e.getMessage().getContentStripped();
        if (!message.startsWith(prefix)) {
            return;
        }
        String commandName = message.substring(prefix.length(), (message.contains(" ") ? message.indexOf(" ") : message.length()));
        D2BCCommand handler = handlerMap.get(commandName);
        if (handler == null) {
            // Unrecognized command
            MessageBuilder messageBuilder = new MessageBuilder();
            messageBuilder.append(e.getAuthor());
            messageBuilder.appendCodeLine(";" + message.replaceFirst(";", ""));
            messageBuilder.append(" is not a known command");
            e.getChannel().sendMessage(messageBuilder.build()).queue(msg -> {
                msg.delete().queueAfter(5L, TimeUnit.SECONDS);
            });
            return;
        }
        if (!handler.hasPermission(e.getAuthor())) {
            MessageBuilder msgBuilder = new MessageBuilder();
            msgBuilder.append(e.getAuthor());
            msgBuilder.append(" You do not have permission to use this command!");
            e.getChannel().sendMessage(msgBuilder.build()).queue(msg -> {
                msg.delete().queueAfter(5L, TimeUnit.SECONDS);
            });
            return;
        }
        handler.processMessage(e);
    }
}
