package nl.patrickdruart.horsestats;

import nl.patrickdruart.horsestats.commands.HorseStatsCommand;
import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.configuration.file.YamlConfiguration;
import nl.tabuu.tabuucore.plugin.TabuuCorePlugin;
import nl.tabuu.tabuucore.util.Dictionary;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * HorseStats plugin's {@link JavaPlugin}/main class. Use {@link #getInstance()} ()}
 * to get instance.
 */
public class HorseStatsPlugin extends TabuuCorePlugin {

	private static HorseStatsPlugin INSTANCE;

	private Dictionary _local;
	private IConfiguration _config;

	@Override
	public void onEnable() {
		if (!isTabuuCoreVersionSupported(
				this.getServer().getPluginManager().getPlugin("TabuuCore").getDescription().getVersion())) {
			this.getLogger().severe(
					"Error: Using an older version of TabuuCore than required. Use TabuuCore version 2020.4.5 or higher!");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		INSTANCE = this;

		// loading configurations
		_config = getConfigurationManager().addConfiguration("config.yml", YamlConfiguration.class);

		String language = getConfiguration().getString("settings.language-file");
		_local = getConfigurationManager().addConfiguration(language, YamlConfiguration.class).getDictionary("");

		// setting commands handler
		this.getCommand("horsestats").setExecutor(new HorseStatsCommand());
	}

	public void reloadAllConfigs() {
		getConfigurationManager().reloadAll();

		String language = getConfiguration().getString("settings.language-file");
		_local = getConfigurationManager().addConfiguration(language, YamlConfiguration.class).getDictionary("");
	}

	public Dictionary getLocal() {
		return _local;
	}

	public IConfiguration getConfiguration() {
		return _config;
	}

	public boolean isTabuuCoreVersionSupported(String version) {
		int[] supported = new int[] { 2, 0, 0 };
		int[] active = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
		for (int i = 0; i < 3; i++) {
			if (active[i] < supported[i])
				return false;
			else if (active[i] > supported[i])
				return true;
		}
		return true;
	}

	/**
	 * @return {@link Dictionary} containing all translated messages from the
	 *         language config as defined in the plugin config.
	 */
	@Deprecated
	public static Dictionary getDictionary() {
		return getInstance().getLocal();
	}

	public static HorseStatsPlugin getInstance() {
		return INSTANCE;
	}
}