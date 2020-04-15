package nl.patrickdruart.horsestats.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import edu.rit.numeric.Cubic;
import nl.patrickdruart.horsestats.HorseStatsPlugin;
import nl.tabuu.tabuucore.command.Command;
import nl.tabuu.tabuucore.command.CommandResult;
import nl.tabuu.tabuucore.command.SenderType;
import nl.tabuu.tabuucore.command.argument.ArgumentType;
import nl.tabuu.tabuucore.command.argument.converter.OrderedArgumentConverter;
import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.nms.wrapper.INBTTagCompound;
import nl.tabuu.tabuucore.text.ComponentBuilder;
import nl.tabuu.tabuucore.util.Dictionary;

/**
 * Command class for base horsestats command.
 */
public class HorseStatsCommand extends Command {
	public HorseStatsCommand() {
		super("horsestats");
		this.addSubCommand("help", new HorseStatsHelpCommand(this));
		this.addSubCommand("view", new HorseStatsViewCommand(this));
		this.addSubCommand("edit", new HorseStatsEditCommand(this));
		this.addSubCommand("reload", new HorseStatsReloadCommand(this));
	}

	@Override
	protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
		Bukkit.dispatchCommand(sender, "horsestats help");
		return CommandResult.SUCCESS;
	}

	/**
	 * Command class for help command.
	 */
	class HorseStatsHelpCommand extends Command {

		protected HorseStatsHelpCommand(Command parent) {
			super("horsestats help", parent);
		}

		@Override
		protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
			Bukkit.dispatchCommand(sender, "help horsestats");
			return CommandResult.SUCCESS;
		}

	}

	/**
	 * Command class for reload command.
	 */
	class HorseStatsReloadCommand extends Command {

		protected HorseStatsReloadCommand(Command parent) {
			super("horsestats reload", parent);
		}

		@Override
		protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
			HorseStatsPlugin.reloadAllConfigs();
			sender.spigot().sendMessage(ComponentBuilder
					.parse((HorseStatsPlugin.getDictionary().translate("COMMAND_RELOAD_SUCCESS"))).build());
			return CommandResult.SUCCESS;
		}

	}

	/**
	 * Command class for view command.
	 */
	class HorseStatsViewCommand extends Command {
		protected HorseStatsViewCommand(Command parent) {
			super("horsestats view", parent);
			this.setRequiredSenderType(SenderType.PLAYER);
		}

		@Override
		protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
			Player player = (Player) sender;
			AbstractHorse target = getTarget(player);
			Dictionary dict = HorseStatsPlugin.getDictionary();
			if (target == null) {
				sender.spigot().sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_VIEW_NOT_FOUND")).build());
				return CommandResult.SUCCESS;
			}

			// Header
			ComponentBuilder header = ComponentBuilder.parse(dict.translate("COMMAND_VIEW_HEADER"));

			// Name
			ComponentBuilder name = ComponentBuilder.parse(dict.translate("COMMAND_VIEW_NAME", "{NAME}",
					target.getCustomName() != null ? target.getCustomName() : target.getType().name()));

			// Health
			ComponentBuilder health = ComponentBuilder.parse(dict.translate("COMMAND_VIEW_HEALTH", "{HEALTH}",
					"" + Math.round(target.getHealth() * 10000.0) / 10000.0, "{MAXHEALTH}",
					"" + Math.round(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 10000.0) / 10000.0,
					"{HEARTS}", "" + Math.round((target.getHealth() / 2) * 10000.0) / 10000.0, "{MAXHEARTS}",
					"" + Math.round((target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) * 10000.0)
							/ 10000.0));

			// Jump
			ComponentBuilder jump = ComponentBuilder.parse(dict.translate("COMMAND_VIEW_JUMP", "{JUMPNUMBER}",
					"" + Math.round(target.getJumpStrength() * 10000.0) / 10000.0, "{JUMPBLOCKS}",
					"" + Math.round(jumpToBlocks(target.getJumpStrength()) * 10000.0) / 10000.0));

			// Speed
			ComponentBuilder speed = ComponentBuilder.parse(dict.translate("COMMAND_VIEW_SPEED", "{SPEEDNUMBER}", ""
					+ Math.round(target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * 10000.0) / 10000.0,
					"{SPEEDBLOCKS}",
					"" + Math.round(
							speedToBlocks(target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue()) * 10000.0)
							/ 10000.0));

			// Builder
			ComponentBuilder builder = ComponentBuilder.create().then(header).thenText("\n").then(name).thenText("\n")
					.then(health).thenText("\n").then(jump).thenText("\n").then(speed);

			// Horse
			if (target instanceof Horse) {
				// Color
				ComponentBuilder color = ComponentBuilder
						.parse(dict.translate("COMMAND_VIEW_COLOR", "{COLOR}", ((Horse) target).getColor().name()));
				// Markings
				ComponentBuilder mark = ComponentBuilder
						.parse(dict.translate("COMMAND_VIEW_MARK", "{MARK}", ((Horse) target).getStyle().name()));
				builder.thenText("\n").then(color).thenText("\n").then(mark);
			}

			// Llama
			if (target instanceof Llama) {
				// Strength
				ComponentBuilder strength = ComponentBuilder.parse(dict.translate("COMMAND_VIEW_STRENGTH", "{STRENGTH}",
						"" + INBTTagCompound.get(target).getInt("Strength"), "{STRENGTHSLOTS}",
						"" + (INBTTagCompound.get(target).getInt("Strength") * 3)));
				builder.thenText("\n").then(strength);
			}

			player.spigot().sendMessage(builder.build());
			return CommandResult.SUCCESS;
		}
	}

	/**
	 * Command class for the base edit command.
	 */
	class HorseStatsEditCommand extends Command {
		protected HorseStatsEditCommand(Command parent) {
			super("horsestats edit", parent);
			this.setRequiredSenderType(SenderType.PLAYER);
			this.addSubCommand("health",
					new HorseStatsEditAttributeCommand("horsestats edit health", parent, Attribute.GENERIC_MAX_HEALTH));
			this.addSubCommand("jumpstrength", new HorseStatsEditAttributeCommand("horsestats edit jumpstrength",
					parent, Attribute.HORSE_JUMP_STRENGTH));
			this.addSubCommand("speed", new HorseStatsEditAttributeCommand("horsestats edit speed", parent,
					Attribute.GENERIC_MOVEMENT_SPEED));
			this.addSubCommand("color", new HorseStatsEditAppearanceCommand("horsestats edit color", parent, true));
			this.addSubCommand("markings",
					new HorseStatsEditAppearanceCommand("horsestats edit markings", parent, false));
			this.addSubCommand("strength", new HorseStatsEditStrengthCommand("horsestats edit strength", parent));
		}

		@Override
		protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
			return CommandResult.WRONG_SYNTAX;
		}

		/**
		 * Command class used to edit attributes of a {@link AbstractHorse} entity.
		 */
		class HorseStatsEditAttributeCommand extends Command {
			private Attribute _attribute;

			protected HorseStatsEditAttributeCommand(String command, Command parent, Attribute attribute) {
				super(command, parent);
				_attribute = attribute;
				OrderedArgumentConverter converter = new OrderedArgumentConverter();
				converter.setSequence(ArgumentType.DOUBLE, ArgumentType.STRING);
				this.setArgumentConverter(converter);
				this.setRequiredSenderType(SenderType.PLAYER);
			}

			@Override
			protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
				if (!args.get(0).isPresent())
					return CommandResult.WRONG_SYNTAX;
				Player player = (Player) sender;
				AbstractHorse target = getTarget(player);
				Dictionary dict = HorseStatsPlugin.getDictionary();
				if (target == null) {
					sender.spigot()
							.sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_EDIT_NOT_FOUND")).build());
					return CommandResult.SUCCESS;
				}
				double value = (Double) args.get(0).get();
				if (args.get(1).isPresent() && (this.getName().split(" ")[2].equalsIgnoreCase("jumpstrength")
						|| this.getName().split(" ")[2].equalsIgnoreCase("speed"))) {
					boolean jump = this.getName().split(" ")[2].equalsIgnoreCase("jumpstrength");
					String type = (String) args.get(1).get();
					if (type.equalsIgnoreCase("blocks")) {
						value = (jump) ? blocksToJump(value) : blocksToSpeed(value);
						sender.spigot().sendMessage(
								ComponentBuilder.parse(dict.translate("COMMAND_EDIT_BLOCKS_WARN")).build());
					} else if (!type.equalsIgnoreCase("number"))
						return CommandResult.WRONG_SYNTAX;
				}
				target.getAttribute(_attribute).setBaseValue(value);
				sender.spigot().sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_EDIT_SUCCESS")).build());
				return CommandResult.SUCCESS;
			}

			@Override
			public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command bukkitCommand,
					String label, String[] arguments) {
				List<String> result = super.onTabComplete(sender, bukkitCommand, label, arguments);
				if (arguments.length == 2)
					if (this.getName().split(" ")[2].equalsIgnoreCase("Jumpstrength")
							|| this.getName().split(" ")[2].equalsIgnoreCase("Speed"))
						for (String type : new String[] { "Blocks", "Number" })
							if (type.toUpperCase().startsWith(arguments[1].toUpperCase()))
								result.add(type);
				return result;
			}
		}

		/**
		 * Command class used to edit the Strength nbt tag of a {@link Llama} entity.
		 * The Strength nbt tag determines the inventory size of a {@link Llama}
		 */
		class HorseStatsEditStrengthCommand extends Command {

			protected HorseStatsEditStrengthCommand(String command, Command parent) {
				super(command, parent);
				OrderedArgumentConverter converter = new OrderedArgumentConverter();
				converter.setSequence(ArgumentType.INTEGER);
				this.setArgumentConverter(converter);
				this.setRequiredSenderType(SenderType.PLAYER);
			}

			@Override
			protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
				if (!args.get(0).isPresent())
					return CommandResult.WRONG_SYNTAX;
				Player player = (Player) sender;
				AbstractHorse target = getTarget(player);
				if (!(target instanceof Llama))
					target = null;
				Dictionary dict = HorseStatsPlugin.getDictionary();
				if (target == null) {
					sender.spigot()
							.sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_EDIT_NO_LLAMA")).build());
					return CommandResult.SUCCESS;
				}
				INBTTagCompound tag = INBTTagCompound.get(target);
				tag.setInt("Strength", (Integer) args.get(0).get());
				tag.apply(target);
				sender.spigot().sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_EDIT_SUCCESS")).build());
				return CommandResult.SUCCESS;
			}

			@Override
			protected List<String> onTabSuggest(CommandSender sender, List<String> arguments, String partial,
					List<String> suggestions) {
				if (arguments.size() != 0)
					return new ArrayList<String>();
				List<String> a = new ArrayList<String>();
				a.addAll(Arrays.asList("1", "2", "3", "4", "5"));
				return a;
			}
		}

		/**
		 * Command class used to edit the appearance of a {@link Horse} entity.
		 */
		class HorseStatsEditAppearanceCommand extends Command {
			private boolean _color;

			protected HorseStatsEditAppearanceCommand(String command, Command parent, boolean isColor) {
				super(command, parent);
				_color = isColor;
				OrderedArgumentConverter converter = new OrderedArgumentConverter();
				converter.setSequence(ArgumentType.STRING);
				this.setArgumentConverter(converter);
				this.setRequiredSenderType(SenderType.PLAYER);
			}

			@Override
			protected CommandResult onCommand(CommandSender sender, List<Optional<?>> args) {
				if (!args.get(0).isPresent())
					return CommandResult.WRONG_SYNTAX;
				Player player = (Player) sender;
				AbstractHorse abstrTarget = getTarget(player);
				Dictionary dict = HorseStatsPlugin.getDictionary();
				if (abstrTarget == null) {
					sender.spigot()
							.sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_EDIT_NOT_FOUND")).build());
					return CommandResult.SUCCESS;
				}
				if (!(abstrTarget instanceof Horse)) {
					sender.spigot().sendMessage(ComponentBuilder
							.parse(dict.translate("COMMAND_EDIT_NO_" + (_color ? "COLOR" : "MARK"))).build());
					return CommandResult.SUCCESS;
				}
				Horse target = (Horse) abstrTarget;
				if (_color)
					target.setColor(Color.valueOf(((String) args.get(0).get()).toUpperCase()));
				else
					target.setStyle(Style.valueOf(((String) args.get(0).get()).toUpperCase()));
				sender.spigot().sendMessage(ComponentBuilder.parse(dict.translate("COMMAND_EDIT_SUCCESS")).build());
				return CommandResult.SUCCESS;
			}

			@Override
			public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command bukkitCommand,
					String label, String[] arguments) {
				List<String> result = super.onTabComplete(sender, bukkitCommand, label, arguments);
				if (arguments.length == 1)
					for (Enum<?> e : _color ? Horse.Color.values() : Horse.Style.values())
						if (e.name().toUpperCase().startsWith(arguments[0].toUpperCase()))
							result.add(e.name());
				return result;
			}
		}
	}

	/**
	 * @param player who's looking for an {@link AbstractHorse}
	 * @return the closest {@link AbstractHorse} the player is looking at. Returns
	 *         null if no {@link AbstractHorse} is found.
	 */
	private AbstractHorse getTarget(Player player) {
		AbstractHorse target = null;
		double range = HorseStatsPlugin.getConfigurationManager().getConfiguration("config").getDouble("gameplay.range",
				10.0D);
		for (Entity entity : player.getNearbyEntities(range, range, range)) {
			if (!(entity instanceof AbstractHorse) || !(isLookingAt(player, (AbstractHorse) entity)))
				continue;
			// entity is an instance of AbstractHorse and the player is looking at the
			// entity.
			if (target == null || player.getEyeLocation().distance(entity.getLocation()) < player.getEyeLocation()
					.distance(target.getLocation()))
				// target will be the closest AbstractHorse by the end of the for loop.
				target = (AbstractHorse) entity;
		}
		return target;
	}

	/**
	 * @param jumpstrength is the jump strength value to convert to the amount of
	 *                     blocks.
	 * @return the amount of blocks that the value parameter equates to. Only
	 *         accurate up to and including 5 blocks.
	 */
	private double jumpToBlocks(double jumpstrength) {
		IConfiguration config = HorseStatsPlugin.getConfigurationManager().getConfiguration("config");
		// values from https://minecraft.gamepedia.com/Horse#Jump_strength
		// y = ax^3 + bx^2 + cx + d
		final double a = config.getDouble("data.jump.a", -0.1817584952);
		final double b = config.getDouble("data.jump.b", 3.689713992);
		final double c = config.getDouble("data.jump.c", 2.128599134);
		final double d = config.getDouble("data.jump.d", -0.343930367);
		double x = jumpstrength;
		double y = (a * x * x * x) + (b * x * x) + (c * x) + d;
		return y < 16.375 ? y : 16.375;
		// 16.375 seems to be the jump height for a value of 2.0 which is the hard limit
		// for jump strength
	}

	/**
	 * @param blocks is the amount of blocks to convert to the jump strength value.
	 * @return the jump strength amount that the blocks parameter equates to. Only
	 *         accurate up to and including 5 blocks.
	 */
	private double blocksToJump(double blocks) {
		IConfiguration config = HorseStatsPlugin.getConfigurationManager().getConfiguration("config");
		// values from https://minecraft.gamepedia.com/Horse#Jump_strength
		// 0 = ax^3 + bx^2 + cx + d - y
		final double a = config.getDouble("data.jump.a", -0.1817584952);
		final double b = config.getDouble("data.jump.b", 3.689713992);
		final double c = config.getDouble("data.jump.c", 2.128599134);
		final double y = blocks;
		final double d = config.getDouble("data.jump.d", -0.343930367) - y;
		Cubic cub = new Cubic();
		cub.solve(a, b, c, d);
		double x = 0;
		if (!Double.isNaN(cub.x1) && cub.x1 >= 0 && cub.x1 <= 2)
			x = cub.x1;
		if (!Double.isNaN(cub.x2) && cub.x2 >= 0 && cub.x2 <= 2)
			x = cub.x2;
		if (!Double.isNaN(cub.x3) && cub.x3 >= 0 && cub.x3 <= 2)
			x = cub.x3;
		// x is between 0.0 and 2.0 (inclusive) and thus a correct value for jump
		// strength
		return x;
	}

	/**
	 * @param double value is the movement speed value to convert to the amount of
	 *               blocks per second.
	 * @return the amount of blocks per second the value parameter equates to.
	 */
	private double speedToBlocks(double value) {
		// values from https://minecraft.gamepedia.com/Tutorials/Horses#Speed
		return (value * HorseStatsPlugin.getConfigurationManager().getConfiguration("config")
				.getDouble("data.speed-conversion", 42.157787584D));
		// beneath code is an example of why reading is important
		// // speed goes up linearly thus a/b = c/d is applicable
		// double a = 0.1125;
		// double b = 4.85;
		// double c = value;
		// double d = 0;
		// // a/b = c/d
		// // (a*d)/b = c
		// // a*d = b*c
		// // d = (b*c)/a
		// d = (b * c) / a;
		// return d;
	}

	/**
	 * @param blocks is the amount of blocks per second to convert to the movement
	 *               speed value.
	 * @return the movement speed value the blocks parameter equates to.
	 */
	private double blocksToSpeed(double blocks) {
		// values from https://minecraft.gamepedia.com/Tutorials/Horses#Speed
		return (blocks / HorseStatsPlugin.getConfigurationManager().getConfiguration("config")
				.getDouble("data.speed-conversion", 42.157787584D));
		// beneath code is an example of why reading is important
		// // speed goes up linearly thus a/b = c/d is applicable
		// double a = 4.85;
		// double b = 0.1125;
		// double c = blocks;
		// double d = 0;
		// // a/b = c/d
		// // (a*d)/b = c
		// // a*d = b*c
		// // d = (b*c)/a
		// d = (b * c) / a;
		// return d;
	}

	/**
	 * @param player is the player who is looking.
	 * @param entity is the entity to check if the player is looking at it.
	 * @return whether the player is looking at the entity.
	 */
	private boolean isLookingAt(Player player, LivingEntity entity) {
		Location eye = player.getEyeLocation();
		Location entityAA = entity.getLocation().subtract(entity.getWidth() / 2, 0, entity.getWidth() / 2);
		Location entityBB = entity.getLocation().add(entity.getWidth() / 2, entity.getHeight(), entity.getWidth() / 2);
		return traceRay(entityAA.toVector(), entityBB.toVector(), eye);
	}

	/**
	 * originates from <a href=
	 * "https://bukkit.org/threads/check-if-vector-goes-through-certain-area.393647/"
	 * >https://bukkit.org/threads/check-if-vector-goes-through-certain-area.393647/</a>
	 * 
	 * @param pointAA     corner AA.
	 * @param pointBB     corner BB.
	 * @param eyeLocation {@link LivingEntity}#getEyeLocation().
	 * @return whether the eyeLocation parameter's direction is looking through an
	 *         AABB area.
	 */
	private boolean traceRay(Vector pointAA, Vector pointBB, Location eyeLocation) {
		Vector dir = eyeLocation.getDirection();
		Vector mini = new Vector(Math.min(pointAA.getX(), pointBB.getX()), Math.min(pointAA.getY(), pointBB.getY()),
				Math.min(pointAA.getZ(), pointBB.getZ()));
		Vector maxi = new Vector(Math.max(pointAA.getX(), pointBB.getX()), Math.max(pointAA.getY(), pointBB.getY()),
				Math.max(pointAA.getZ(), pointBB.getZ()));

		double dirFracX = 1.0F / dir.getX();
		double dirFracY = 1.0F / dir.getY();
		double dirFracZ = 1.0F / dir.getZ();

		float t1 = (float) ((mini.getX() - eyeLocation.getX()) * dirFracX);
		float t2 = (float) ((maxi.getX() - eyeLocation.getX()) * dirFracX);
		float t3 = (float) ((mini.getY() - eyeLocation.getY()) * dirFracY);
		float t4 = (float) ((maxi.getY() - eyeLocation.getY()) * dirFracY);
		float t5 = (float) ((mini.getZ() - eyeLocation.getZ()) * dirFracZ);
		float t6 = (float) ((maxi.getZ() - eyeLocation.getZ()) * dirFracZ);

		float tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
		float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

		// If the ray intersects the AABB, but the AABB is behind the player
		if (tmax < 0)
			return false;

		// If the ray doesn't intersect the AABB.
		if (tmin > tmax)
			return false;

		return true;
	}
}
