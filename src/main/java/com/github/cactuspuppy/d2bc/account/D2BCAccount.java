package com.github.cactuspuppy.d2bc.account;

import lombok.Data;

import java.util.UUID;

public interface D2BCAccount {

    void setMinecraftID();

    UUID getMinecraftID();

    void setDiscordID();

    long getDiscordID();

    boolean isRelaying();
}
