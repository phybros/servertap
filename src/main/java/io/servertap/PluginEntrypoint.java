package io.servertap;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.servertap.api.v1.EconomyApi;
import io.servertap.api.v1.PlayerApi;
import io.servertap.api.v1.ServerApi;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

import static io.javalin.apibuilder.ApiBuilder.*;

public class PluginEntrypoint extends JavaPlugin {

    private static final Logger log = Bukkit.getLogger();

    private static Economy econ = null;

    private static Javalin app = null;

    @Override
    public void onEnable() {

        setupEconomy();

        // Get the current class loader.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Temporarily set this thread's class loader to the plugin's class loader.
        // Replace JavalinTestPlugin.class with your own plugin's class.
        Thread.currentThread().setContextClassLoader(PluginEntrypoint.class.getClassLoader());

        // Instantiate the web server (which will now load using the plugin's class
        // loader).
        if(app == null) {
            app = Javalin.create(config -> {
                config.defaultContentType = "application/json";
            });
        }
        // Don't create a new instance if the plugin is reloaded
        app.start(4567);


        app.before(ctx -> log.info(ctx.req.getPathInfo()));

        app.routes(() -> {
            // Routes for v1 of the API
            path(Constants.API_V1, () -> {
                // Pings
                get("ping", ServerApi::ping);
                post("ping", ServerApi::ping);

                // Server routes
                get("server", ServerApi::serverGet);
                get("worlds", ServerApi::worldsGet);
                get("worlds/:world", ServerApi::worldGet);

                // Communication
                post("broadcast", ServerApi::broadcastPost);

                // Player routes
                get("players", PlayerApi::playersGet);
                get("players/:player", PlayerApi::playerGet);
                get("allPlayers", PlayerApi::offlinePlayersGet);
                get("player/:uuid/:world/inventory", PlayerApi::getPlayerInv);
                // Whitelist routes
                get("whitelist", ServerApi::whitelistGet);
                post("whitelist", ServerApi::whitelistPost);
                
                // Economy routes
                post("economy/pay", EconomyApi::playerPay);
                post("economy/debit", EconomyApi::playerDebit);


            });
        });

        // Default fallthrough. Just give them a 404.
        app.get("*", ctx -> {
            throw new NotFoundResponse();
        });

        // Put the original class loader back where it was.
        Thread.currentThread().setContextClassLoader(classLoader);

    }

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
        // Release port so that /reload will work
        app.stop();
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        econ = rsp.getProvider();
    }

    public static Economy getEconomy() {
        return econ;
    }

}
