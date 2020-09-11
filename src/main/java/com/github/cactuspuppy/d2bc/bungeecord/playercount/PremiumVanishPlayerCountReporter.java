package com.github.cactuspuppy.d2bc.bungeecord.playercount;

import com.github.cactuspuppy.d2bc.D2BC;
import de.myzelyam.api.vanish.BungeeVanishAPI;

public class PremiumVanishPlayerCountReporter implements PlayerCountReporter {

    @Override
    public int numPlayersOnline() {
        return D2BC.getPlugin().getProxy().getOnlineCount() - BungeeVanishAPI.getInvisiblePlayers().size();
    }
}
