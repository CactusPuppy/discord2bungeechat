package com.github.cactuspuppy.d2bc.account;

import dev.aura.bungeechat.api.account.BungeeChatAccount;
import lombok.Data;

import java.util.UUID;

public interface D2BCAccount extends BungeeChatAccount {

    /**
     * @return Whether this account wants to send messages
     */
    boolean isRelaying();

    long getDiscordID();
}
