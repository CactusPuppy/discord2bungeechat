package com.github.cactuspuppy.d2bc.bungeecord.playercount;

import lombok.Getter;
import lombok.Setter;

/**
 * This class is responsible for actually maintaining the current {@link PlayerCountReporter}
 */
public class PlayerCount {
    @Getter @Setter private static PlayerCountReporter reporter;
}
