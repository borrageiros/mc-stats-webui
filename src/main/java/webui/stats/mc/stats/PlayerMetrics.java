package webui.stats.mc.stats;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerMetrics {
	private static final Set<String> ORES = Set.of(
		"minecraft:coal_ore",
		"minecraft:deepslate_coal_ore",
		"minecraft:iron_ore",
		"minecraft:deepslate_iron_ore",
		"minecraft:copper_ore",
		"minecraft:deepslate_copper_ore",
		"minecraft:gold_ore",
		"minecraft:deepslate_gold_ore",
		"minecraft:redstone_ore",
		"minecraft:deepslate_redstone_ore",
		"minecraft:lapis_ore",
		"minecraft:deepslate_lapis_ore",
		"minecraft:diamond_ore",
		"minecraft:deepslate_diamond_ore",
		"minecraft:emerald_ore",
		"minecraft:deepslate_emerald_ore",
		"minecraft:nether_gold_ore",
		"minecraft:nether_quartz_ore",
		"minecraft:ancient_debris"
	);
	private static final Set<String> FARM = Set.of(
		"minecraft:silverfish",
		"minecraft:chicken",
		"minecraft:cod",
		"minecraft:salmon",
		"minecraft:tropical_fish",
		"minecraft:pufferfish",
		"minecraft:squid",
		"minecraft:glow_squid",
		"minecraft:bat"
	);
	private static final Set<String> HOSTILE = Set.of(
		"minecraft:zombie",
		"minecraft:skeleton",
		"minecraft:creeper",
		"minecraft:spider",
		"minecraft:cave_spider",
		"minecraft:enderman",
		"minecraft:witch",
		"minecraft:drowned",
		"minecraft:husk",
		"minecraft:stray",
		"minecraft:phantom",
		"minecraft:blaze",
		"minecraft:ghast",
		"minecraft:magma_cube",
		"minecraft:slime",
		"minecraft:wither_skeleton",
		"minecraft:piglin",
		"minecraft:piglin_brute",
		"minecraft:zombified_piglin",
		"minecraft:hoglin",
		"minecraft:zoglin",
		"minecraft:guardian",
		"minecraft:elder_guardian",
		"minecraft:shulker",
		"minecraft:vindicator",
		"minecraft:evoker",
		"minecraft:pillager",
		"minecraft:ravager",
		"minecraft:vex",
		"minecraft:warden",
		"minecraft:wither",
		"minecraft:ender_dragon",
		"minecraft:endermite",
		"minecraft:breeze",
		"minecraft:bogged",
		"minecraft:creaking",
		"minecraft:zombie_villager",
		"minecraft:zombie_nautilus"
	);
	private static final Set<String> BOSSES = Set.of(
		"minecraft:ender_dragon",
		"minecraft:wither",
		"minecraft:warden",
		"minecraft:elder_guardian"
	);
	private static final Set<String> CROPS = Set.of(
		"minecraft:wheat",
		"minecraft:potatoes",
		"minecraft:carrots",
		"minecraft:beetroots",
		"minecraft:sugar_cane",
		"minecraft:nether_wart",
		"minecraft:melon",
		"minecraft:pumpkin",
		"minecraft:cocoa",
		"minecraft:cactus",
		"minecraft:bamboo",
		"minecraft:sweet_berry_bush",
		"minecraft:kelp",
		"minecraft:chorus_flower",
		"minecraft:torchflower",
		"minecraft:pitcher_crop"
	);

	private PlayerMetrics() {
	}

	public static PlayerRecord compute(UUID uuid, String name, StatFile stats) {
		double playHours = stats.custom("play_time") / 20.0 / 3600.0;
		long hostile = stats.sum("minecraft:killed", HOSTILE);
		long farm = stats.sum("minecraft:killed", FARM);
		long bosses = stats.sum("minecraft:killed", BOSSES);
		long ores = stats.sum("minecraft:mined", ORES);
		long diamonds = stats.mined("diamond_ore") + stats.mined("deepslate_diamond_ore");
		long iron = stats.mined("iron_ore") + stats.mined("deepslate_iron_ore");
		long debris = stats.mined("ancient_debris");
		long logs = stats.sum("minecraft:mined", key -> key.endsWith("_log") || key.endsWith("_stem"));
		long mined = stats.sum("minecraft:mined");
		long broken = stats.sum("minecraft:broken");
		double walk = km(stats.custom("walk_one_cm"));
		double sprint = km(stats.custom("sprint_one_cm"));
		double swim = km(stats.custom("swim_one_cm"));
		double boat = km(stats.custom("boat_one_cm"));
		double elytra = km(stats.custom("aviate_one_cm"));
		double horse = km(stats.custom("horse_one_cm"));
		double ghast = km(stats.custom("happy_ghast_one_cm"));
		double nautilus = km(stats.custom("nautilus_one_cm"));
		double minecart = km(stats.custom("minecart_one_cm"));
		double strider = km(stats.custom("strider_one_cm"));
		double climb = km(stats.custom("climb_one_cm"));
		double crouch = km(stats.custom("crouch_one_cm"));
		double water = km(stats.custom("walk_on_water_one_cm"));
		double under = km(stats.custom("walk_under_water_one_cm"));
		double travel = walk + sprint + swim + boat + elytra + horse + ghast + nautilus + minecart + strider + climb + crouch + water + under;
		long trades = stats.custom("traded_with_villager");
		long enchanted = stats.custom("enchant_item");
		long raids = stats.custom("raid_win");
		long animals = stats.custom("animals_bred");
		long fish = stats.custom("fish_caught");
		long deaths = stats.custom("deaths");
		long mobKills = stats.custom("mob_kills");

		Map<String, Double> categories = new HashMap<>();
		categories.put("combat", combatRaw(stats, hostile));
		categories.put("milestones", milestoneRaw(stats, raids, enchanted, debris));
		categories.put("mining", miningRaw(stats, diamonds, iron, debris));
		categories.put("explore", travel + stats.custom("open_enderchest") * 0.4 + Math.min(stats.custom("open_chest"), 400) * 0.03);
		categories.put("produce", animals * 2.0 + fish + trades * 3.0 + stats.sum("minecraft:mined", CROPS) * 0.04);

		Map<String, Double> values = new HashMap<>();
		values.put("hostile-kills", (double) hostile);
		values.put("farm-kills", (double) farm);
		values.put("zombies", (double) stats.killed("zombie"));
		values.put("skeletons", (double) stats.killed("skeleton"));
		values.put("creepers", (double) stats.killed("creeper"));
		values.put("endermen", (double) stats.killed("enderman"));
		values.put("blazes", (double) stats.killed("blaze"));
		values.put("wither-skeletons", (double) stats.killed("wither_skeleton"));
		values.put("ender-dragon", (double) stats.killed("ender_dragon"));
		values.put("wither", (double) stats.killed("wither"));
		values.put("warden", (double) stats.killed("warden"));
		values.put("elder-guardian", (double) stats.killed("elder_guardian"));
		values.put("bosses", (double) bosses);
		values.put("raids-won", (double) raids);
		values.put("player-kills", (double) stats.custom("player_kills"));
		values.put("damage-dealt", stats.custom("damage_dealt") / 20.0);
		values.put("deaths", (double) deaths);
		values.put("kills-per-hour", playHours > 0.05 ? mobKills / playHours : 0);
		values.put("blocks-mined", (double) mined);
		values.put("ores", (double) ores);
		values.put("diamonds-mined", (double) diamonds);
		values.put("iron-mined", (double) iron);
		values.put("ancient-debris", (double) debris);
		values.put("logs-mined", (double) logs);
		values.put("sculk-mined", (double) stats.mined("sculk"));
		values.put("tools-broken", (double) broken);
		values.put("distance-total", travel);
		values.put("distance-walk-sprint", walk + sprint);
		values.put("distance-walk", walk);
		values.put("distance-sprint", sprint);
		values.put("distance-swim", swim);
		values.put("distance-boat", boat);
		values.put("distance-elytra", elytra);
		values.put("distance-horse", horse);
		values.put("distance-happy-ghast", ghast);
		values.put("distance-nautilus", nautilus);
		values.put("distance-minecart", minecart);
		values.put("distance-strider", strider);
		values.put("chests-opened", (double) stats.custom("open_chest"));
		values.put("animals-bred", (double) animals);
		values.put("fish-caught", (double) fish);
		values.put("villager-trades", (double) trades);
		values.put("items-enchanted", (double) enchanted);
		values.put("sleeps", (double) stats.custom("sleep_in_bed"));
		values.put("jumps", (double) stats.custom("jump"));
		values.put("killed-by-zombie", (double) stats.killedBy("zombie"));
		values.put("killed-by-creeper", (double) stats.killedBy("creeper"));
		values.put("killed-by-skeleton", (double) stats.killedBy("skeleton"));
		values.put("killed-by-warden", (double) stats.killedBy("warden"));
		Map<String, Map<String, Long>> vanilla = new LinkedHashMap<>();
		for (String group : VanillaCatalog.GROUPS) {
			vanilla.put(group, stats.groupValues(VanillaCatalog.jsonType(group)));
		}
		return new PlayerRecord(uuid, name, playHours, 0, values, categories, Map.of(), vanilla);
	}

	private static double combatRaw(StatFile stats, long hostile) {
		return hostile
			+ stats.killed("blaze")
			+ stats.killed("wither_skeleton")
			+ stats.killed("guardian")
			+ stats.killed("elder_guardian")
			+ stats.killed("shulker") * 2
			+ stats.killed("pillager")
			+ stats.killed("vindicator")
			+ stats.killed("evoker")
			+ stats.killed("ravager")
			+ stats.killed("vex")
			+ stats.killed("breeze");
	}

	private static double milestoneRaw(StatFile stats, long raids, long enchanted, long debris) {
		return stats.killed("ender_dragon") * 12
			+ stats.killed("wither") * 18
			+ stats.killed("warden") * 24
			+ stats.killed("elder_guardian") * 8
			+ raids * 14
			+ enchanted * 0.4
			+ stats.custom("interact_with_beacon") * 6
			+ stats.custom("interact_with_smithing_table") * 2
			+ stats.pickedUp("elytra") * 16
			+ debris * 1.2;
	}

	private static double miningRaw(StatFile stats, long diamonds, long iron, long debris) {
		long gold = stats.mined("gold_ore") + stats.mined("deepslate_gold_ore") + stats.mined("nether_gold_ore");
		long copper = stats.mined("copper_ore") + stats.mined("deepslate_copper_ore");
		long emerald = stats.mined("emerald_ore") + stats.mined("deepslate_emerald_ore");
		long sculk = stats.mined("sculk")
			+ stats.mined("sculk_vein")
			+ stats.mined("sculk_catalyst")
			+ stats.mined("sculk_shrieker")
			+ stats.mined("sculk_sensor");
		return diamonds * 8
			+ debris * 15
			+ emerald * 6
			+ gold * 3
			+ iron * 2
			+ copper
			+ (stats.mined("lapis_ore") + stats.mined("deepslate_lapis_ore")) * 1.5
			+ (stats.mined("redstone_ore") + stats.mined("deepslate_redstone_ore")) * 0.8
			+ stats.mined("nether_quartz_ore") * 1.2
			+ (stats.mined("coal_ore") + stats.mined("deepslate_coal_ore")) * 0.25
			+ sculk * 2
			+ stats.mined("end_stone") * 0.15;
	}

	private static double km(long cm) {
		return cm / 100_000.0;
	}
}
