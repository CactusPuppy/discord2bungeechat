package com.github.cactuspuppy.d2bc.discord.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.Test;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.*;

public class CommandHubTest {

    @Test
    public void registerNewCommand() {
        CommandHub hub = new CommandHub();
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