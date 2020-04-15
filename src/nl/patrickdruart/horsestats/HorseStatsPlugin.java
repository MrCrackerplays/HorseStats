package nl.patrickdruart.horsestats;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import nl.patrickdruart.horsestats.commands.HorseStatsCommand;
import nl.tabuu.tabuucore.configuration.ConfigurationManager;
import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.util.Dictionary;

/**
 * HorseStats plugin's {@link JavaPlugin}/main class. Use {@link #getPlugin()}
 * to get instance.
 */
public class HorseStatsPlugin extends JavaPlugin {

	private static Plugin _plugin;
	private static ConfigurationManager _configurationManager;
	private static IConfiguration _language;

	@Override
	public void onEnable() {
		String[] tabuucoreVersion = this.getServer().getPluginManager().getPlugin("TabuuCore").getDescription()
				.getVersion().split("\\.");
		if (Integer.valueOf(tabuucoreVersion[0]) < 2020 || Integer.valueOf(tabuucoreVersion[1]) < 4
				|| Integer.valueOf(tabuucoreVersion[2]) < 5) {
			this.getLogger().severe(
					"Error: Using an older version of TabuuCore than required. Use TabuuCore version 2020.4.5 or higher!");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		_plugin = this;

		// loading configurations
		_configurationManager = new ConfigurationManager(_plugin);
		_configurationManager.addConfiguration("config");
		IConfiguration config = _configurationManager.getConfiguration("config");
		_configurationManager.addConfiguration(config.getString("settings.language-file"));
		_language = _configurationManager.getConfiguration(config.getString("settings.language-file"));

		// setting commands handler
		this.getCommand("horsestats").setExecutor(new HorseStatsCommand());
	}

	/**
	 * @return This plugin's instance.
	 */
	public static Plugin getPlugin() {
		return _plugin;
	}

	/**
	 * @return {@link ConfigurationManager} used to manage all configs.
	 */
	public static ConfigurationManager getConfigurationManager() {
		return _configurationManager;
	}

	/**
	 * @return {@link Dictionary} containing all translated messages from the
	 *         language config as defined in the plugin config.
	 */
	public static Dictionary getDictionary() {
		return _language.getDictionary("");
	}

	public static void reloadAllConfigs() {
		_configurationManager.reloadAll();
		_language = _configurationManager
				.getConfiguration(_configurationManager.getConfiguration("config").getString("settings.language-file"));
	}
}