package com.github.cactuspuppy.d2bc.bungeecord.playercount;

import com.github.cactuspuppy.d2bc.D2BC;

public class DefaultPlayerCountReporter implements PlayerCountReporter {
    @Override
    public int numPlayersOnline() {
        return D2BC.getPlugin().getProxy().getOnlineCount();
    }
}
