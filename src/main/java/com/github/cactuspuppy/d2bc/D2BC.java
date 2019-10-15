package com.github.cactuspuppy.d2bc;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class D2BC extends JavaPlugin {
    @Getter private static D2BC plugin;
    @Getter private static Logger logger;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        plugin = this;
        logger = Logger.getLogger("D2BC");
        long elapsedMS = System.currentTimeMillis() - start;

    }

    private void loadConfig() {

    }

    @Override
    public void saveDefaultConfig() {
        // Do nothing to avoid breaking custom config
    }

    @Override
    public void onDisable() {

    }
}
