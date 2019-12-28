package com.github.cactuspuppy.d2bc;

import com.github.cactuspuppy.d2bc.discord.command.CommandHub;
import com.github.cactuspuppy.d2bc.utils.Config;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;

public class D2BC extends Plugin {
    @Getter private static D2BC plugin;
    @Getter private Config config;
    @Getter private JDA jda;
    @Getter private CommandHub commandHub;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        plugin = this;
        long elapsedMS = System.currentTimeMillis() - start;
        getLogger().info(ChatColor.LIGHT_PURPLE + "Time elapsed: " + ChatColor.BLUE + elapsedMS + " ms");
    }

    private void loadConfig() {

    }



    @Override
    public void onDisable() {

    }
}
