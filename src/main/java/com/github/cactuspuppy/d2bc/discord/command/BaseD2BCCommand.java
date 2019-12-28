package com.github.cactuspuppy.d2bc.discord.command;

import net.dv8tion.jda.api.entities.User;

public abstract class BaseD2BCCommand implements D2BCCommand {

    @Override
    public boolean hasPermission(User user) {
        return false;
    }
}
