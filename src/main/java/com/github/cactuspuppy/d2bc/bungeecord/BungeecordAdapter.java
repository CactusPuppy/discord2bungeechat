package com.github.cactuspuppy.d2bc.bungeecord;

import com.github.cactuspuppy.d2bc.D2BC;
import com.github.cactuspuppy.d2bc.bungeecord.playercount.PlayerCount;
import com.github.cactuspuppy.d2bc.discord.relay.DiscordAdapter;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class BungeecordAdapter implements Listener {
    /**
     * The next point in time at which the channel topic may be updated,
     * represented in seconds in the UNIX epoch.
     */
    @Getter @Setter
    private long nextUpdate;
    /**
     * The number of seconds to wait between updates, in seconds.
     */
    @Getter
    private long rateLimit;
    /**
     * Whether the updater is already waiting to update the player count.
     */
    private boolean isWaiting = false;

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
//        D2BC.getPlugin().getProxy().getScheduler().runAsync(D2BC.getPlugin(), this::updatePlayerCount);
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
//        D2BC.getPlugin().getProxy().getScheduler().runAsync(D2BC.getPlugin(), this::updatePlayerCount);
    }

    private synchronized void updatePlayerCount() {
        long now = Instant.now().getEpochSecond();
        if (now < nextUpdate) {
            if (isWaiting) {
                return;
            }
            D2BC.getPlugin().getDiscordAdapter().updateDiscordPlayerCount(-1);
            D2BC.getPlugin().getProxy().getScheduler().schedule(D2BC.getPlugin(),
                    this::updatePlayerCount, nextUpdate - now, TimeUnit.SECONDS);
            isWaiting = true;
            return;
        }
        D2BC.getPlugin().getDiscordAdapter().updateDiscordPlayerCount(PlayerCount.getReporter().numPlayersOnline());
        nextUpdate = Instant.now().getEpochSecond() + rateLimit;
        isWaiting = false;
    }

    public void setRateLimit(long duration) {
        if (duration < 600) {
            duration = 600;
        }
        rateLimit = duration;
    }
}
