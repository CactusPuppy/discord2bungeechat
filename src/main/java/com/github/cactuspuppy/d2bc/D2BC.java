package com.github.cactuspuppy.d2bc;

import com.github.cactuspuppy.d2bc.bungeechat.relay.BungeeChatAdapter;
import com.github.cactuspuppy.d2bc.bungeecord.BungeecordAdapter;
import com.github.cactuspuppy.d2bc.bungeecord.playercount.DefaultPlayerCountReporter;
import com.github.cactuspuppy.d2bc.bungeecord.playercount.PlayerCount;
import com.github.cactuspuppy.d2bc.bungeecord.playercount.PremiumVanishPlayerCountReporter;
import com.github.cactuspuppy.d2bc.discord.command.DiscordCommandHub;
import com.github.cactuspuppy.d2bc.discord.relay.DiscordAdapter;
import com.github.cactuspuppy.d2bc.utils.FileConfig;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import org.apache.commons.io.FileUtils;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Scanner;
import java.util.logging.Level;

public class D2BC extends Plugin {
    @Getter private static D2BC plugin;
    @Getter private static DiscordCommandHub discordCommandHub;
    @Getter private FileConfig config;
    @Getter private File logsFolder;
    @Getter private JDA jda;
    @Getter private BungeeChatAdapter bungeeChatAdapter;
    @Getter private DiscordAdapter discordAdapter;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        plugin = this;
        loadConfig();
        initLogs();
        initPlayerCounter();
        startJDA();
        registerListeners();
        getProxy().getScheduler().runAsync(this, () -> discordAdapter.updateDiscordServerStatus(true));
        long elapsedMS = System.currentTimeMillis() - start;
        getLogger().info(ChatColor.LIGHT_PURPLE + "Time elapsed: " + ChatColor.BLUE + elapsedMS + " ms");
    }

    private void startJDA() {
        try {
            getLogger().info("Loading JDA Bot...");
//            getLogger().info(String.format("Token: %s", getToken(getResourceAsStream("token.dat"))));
            discordAdapter = new DiscordAdapter();
            discordCommandHub = new DiscordCommandHub();
            jda = JDABuilder.createLight(getToken(getResourceAsStream("token.dat")), GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(discordAdapter, discordCommandHub)
                .build();
            jda.awaitReady();
            getLogger().info("Bot loaded!");
        } catch (LoginException | InterruptedException e) {
            getLogger().log(Level.SEVERE, "Could not start JDA bot", e);
        }
    }

    private static String getToken(InputStream iS) {
        Scanner scan = new Scanner(iS);
        String line = scan.nextLine();
        scan.close();
        return line.trim();
    }

    /**
     * Check that there is a BungeeChat logs folder to hook into
     */
    private Path getBungeeChatLogsPath() {
        Path bungeeChatLogs = Paths.get(this.getDataFolder().getParent()
                + File.separator + "BungeeChat" + File.separator + "logs");
        if (!Files.isDirectory(bungeeChatLogs)) {
            getLogger().severe("Could not find BungeeChat logs folder.\n" +
                    "Looked for path: " + bungeeChatLogs.toString());
            return null;
        }
        return bungeeChatLogs;
    }

    private void loadConfig() {
        //Get/create main config
        File dataFolder = getDataFolder();
        if (!dataFolder.isDirectory() && !dataFolder.mkdirs()) {
            getLogger().severe("Could not find or create data folder.");
            return;
        }
        File config = new File(getDataFolder(), "config.yml");
        //Create config if not exist
        if (!config.isFile()) {
            InputStream inputStream = getResourceAsStream("config.yml");
            if (inputStream == null) {
                getLogger().severe("No packaged config.yml?!");
                return;
            }
            try {
                FileUtils.copyToFile(inputStream, config);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error while creating new config", e);
                return;
            }
        }
        this.config = new FileConfig(new File(getDataFolder(), "config.yml"));
        this.config.reload();
    }

    private void initLogs() {
        //Initiate or get logs folder
        logsFolder = new File(getDataFolder(), "logs");
        if (!logsFolder.isDirectory() && !logsFolder.mkdirs()) {
            getLogger().severe("Could not find or create logs folder.");
            logsFolder = null;
        }
    }

    private void initPlayerCounter() {
        if (getProxy().getPluginManager().getPlugin("PremiumVanish") != null) {
            getLogger().info(ChatColor.YELLOW + "PremiumVanish detected, enabling support...");
            PlayerCount.setReporter(new PremiumVanishPlayerCountReporter());
        } else {
            getLogger().info(ChatColor.YELLOW + "Enabling default player counter...");
            PlayerCount.setReporter(new DefaultPlayerCountReporter());
        }
        BungeecordAdapter updater = new BungeecordAdapter();
        updater.setNextUpdate(Instant.now().getEpochSecond());
        try {
            updater.setRateLimit(Long.parseLong(getConfig().getOrDefault("discord.rate-limit", "300")));
        } catch (NumberFormatException e) {
            getLogger().severe("Discord rate-limit '%s' is not an integer");
            updater.setRateLimit(600);
        }
        getProxy().getPluginManager().registerListener(this, updater);

    }

    private void registerListeners() {
        Path bungeeChatLogsFolder = getBungeeChatLogsPath();
        if (bungeeChatLogsFolder == null) {
            return;
        }
        bungeeChatAdapter = new BungeeChatAdapter(bungeeChatLogsFolder);
    }

    @Override
    public void onDisable() {
        discordAdapter.updateDiscordServerStatus(false);
        jda.shutdown();
    }
}
