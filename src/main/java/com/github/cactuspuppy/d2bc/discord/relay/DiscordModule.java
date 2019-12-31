package com.github.cactuspuppy.d2bc.discord.relay;

import dev.aura.bungeechat.api.module.BungeeChatModule;
import net.md_5.bungee.api.ProxyServer;

public class DiscordModule implements BungeeChatModule {

    boolean isEnabled = true;
    MessageRelay msgRelay;

    @Override
    public String getName() {
        return "DiscordBridge";
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void onEnable() {
        msgRelay = new MessageRelay();
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().getPluginManager().registerListener(msgRelay);
    }
}
