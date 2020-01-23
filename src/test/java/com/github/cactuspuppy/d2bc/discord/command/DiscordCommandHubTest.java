package com.github.cactuspuppy.d2bc.discord.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DiscordCommandHubTest {

    @Test
    public void registerNewCommand() {
        DiscordCommandHub hub = new DiscordCommandHub();
        assertFalse(hub.registerCommand(new Ping()));
        assertTrue(hub.registerCommand(new FakeD2BCCommand()));
    }

    class FakeD2BCCommand extends BaseD2BCCommand {

        @Override
        public void processMessage(MessageReceivedEvent event) {
            return;
        }

        @Override
        public String getMainName() {
            return "yural";
        }
    }
}