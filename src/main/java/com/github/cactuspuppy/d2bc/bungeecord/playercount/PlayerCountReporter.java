package com.github.cactuspuppy.d2bc.bungeecord.playercount;

public interface PlayerCountReporter {
    /**
     * @return the number of players online in the proxy
     */
    int numPlayersOnline();
}
