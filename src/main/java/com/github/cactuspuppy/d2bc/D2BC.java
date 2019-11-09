package com.github.cactuspuppy.d2bc;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.cactuspuppy.d2bc.utils.Config;

public class D2BC extends JavaPlugin {
    @Getter private static D2BC plugin;
    @Getter private Config config;
    @Getter private JDA jda;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        plugin = this;
        long elapsedMS = System.currentTimeMillis() - start;
        getLogger().info("");
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
