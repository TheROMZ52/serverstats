package com.mahbod.serverstats;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerStatsPlugin extends JavaPlugin {

    private StatsDatabase db;
    private WebServer webServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        db = new StatsDatabase(getDataFolder(), getLogger());

        getServer().getPluginManager().registerEvents(new StatsListener(db), this);

        String bind = getConfig().getString("bind-address", "0.0.0.0");
        int port = getConfig().getInt("web-port", 8123);
        String serverName = getConfig().getString("server-name", "My Server");

        webServer = new WebServer(db, getLogger(), serverName);
        try {
            webServer.start(bind, port);
        } catch (Exception e) {
            getLogger().severe("Could not start web dashboard on " + bind + ":" + port
                    + " - is the port already in use, or blocked by your host panel? " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (webServer != null) webServer.stop();
        if (db != null) db.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int port = getConfig().getInt("web-port", 8123);
        sender.sendMessage("§b[ServerStats] §fDashboard running on port §e" + port
                + "§f. Make sure that port is opened/forwarded in your host panel.");
        return true;
    }
}
