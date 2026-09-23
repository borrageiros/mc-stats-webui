package webui.stats.mc.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LeaderboardCatalog {
	private static final List<String> LISTED_CATEGORIES = List.of(
		"advancements",
		"life",
		"exploration",
		"mining",
		"deaths",
		"combat"
	);

	private static final Map<String, String> CATEGORY_ICONS = Map.of(
		"crowns",
		"minecraft:nether_star",
		"advancements",
		"minecraft:experience_bottle",
		"life",
		"minecraft:clock",
		"exploration",
		"minecraft:compass",
		"mining",
		"minecraft:diamond_pickaxe",
		"deaths",
		"minecraft:bone",
		"combat",
		"minecraft:iron_sword"
	);

	private static final List<LeaderboardInfo> ALL = List.of(
		crown("champion", "score", "minecraft:nether_star"),
		crown("dedicated", "hours", "minecraft:clock"),
		crown("efficient", "score_per_hour", "minecraft:experience_bottle"),
		hiddenCompare("advancements", "advancements", "count", "minecraft:experience_bottle"),
		hiddenCompare("play-time", "life", "hours", "minecraft:clock"),
		board("animals-bred", "life", "count", "minecraft:wheat"),
		board("fish-caught", "life", "count", "minecraft:fishing_rod"),
		board("villager-trades", "life", "count", "minecraft:emerald"),
		board("items-enchanted", "life", "count", "minecraft:enchanted_book"),
		board("sleeps", "life", "count", "minecraft:red_bed"),
		board("jumps", "life", "count", "minecraft:rabbit_foot"),
		board("distance-total", "exploration", "km", "minecraft:compass"),
		board("distance-walk-sprint", "exploration", "km", "minecraft:leather_boots"),
		board("distance-walk", "exploration", "km", "minecraft:leather_boots"),
		board("distance-sprint", "exploration", "km", "minecraft:sugar"),
		board("distance-swim", "exploration", "km", "minecraft:heart_of_the_sea"),
		board("distance-boat", "exploration", "km", "minecraft:oak_boat"),
		board("distance-elytra", "exploration", "km", "minecraft:elytra"),
		board("distance-horse", "exploration", "km", "minecraft:saddle"),
		board("distance-happy-ghast", "exploration", "km", "minecraft:happy_ghast_spawn_egg"),
		board("distance-nautilus", "exploration", "km", "minecraft:nautilus_shell"),
		board("distance-minecart", "exploration", "km", "minecraft:minecart"),
		board("distance-strider", "exploration", "km", "minecraft:warped_fungus_on_a_stick"),
		board("chests-opened", "exploration", "count", "minecraft:chest"),
		board("blocks-mined", "mining", "blocks", "minecraft:diamond_pickaxe"),
		board("ores", "mining", "blocks", "minecraft:iron_ore"),
		board("diamonds-mined", "mining", "blocks", "minecraft:diamond"),
		board("iron-mined", "mining", "blocks", "minecraft:iron_ingot"),
		board("ancient-debris", "mining", "blocks", "minecraft:ancient_debris"),
		board("logs-mined", "mining", "blocks", "minecraft:oak_log"),
		board("sculk-mined", "mining", "blocks", "minecraft:sculk"),
		board("tools-broken", "mining", "count", "minecraft:netherite_scrap"),
		lower("deaths", "deaths", "count", "minecraft:bone"),
		lower("killed-by-zombie", "deaths", "count", "minecraft:zombie_head"),
		lower("killed-by-creeper", "deaths", "count", "minecraft:creeper_head"),
		lower("killed-by-skeleton", "deaths", "count", "minecraft:skeleton_skull"),
		lower("killed-by-warden", "deaths", "count", "minecraft:echo_shard"),
		board("hostile-kills", "combat", "kills", "minecraft:iron_sword"),
		board("farm-kills", "combat", "kills", "minecraft:cooked_chicken"),
		board("zombies", "combat", "kills", "minecraft:zombie_head"),
		board("skeletons", "combat", "kills", "minecraft:skeleton_skull"),
		board("creepers", "combat", "kills", "minecraft:creeper_head"),
		board("endermen", "combat", "kills", "minecraft:ender_pearl"),
		board("blazes", "combat", "kills", "minecraft:blaze_rod"),
		board("wither-skeletons", "combat", "kills", "minecraft:wither_skeleton_skull"),
		board("ender-dragon", "combat", "kills", "minecraft:dragon_egg"),
		board("wither", "combat", "kills", "minecraft:nether_star"),
		board("warden", "combat", "kills", "minecraft:echo_shard"),
		board("elder-guardian", "combat", "kills", "minecraft:elder_guardian_spawn_egg"),
		board("bosses", "combat", "kills", "minecraft:dragon_head"),
		board("raids-won", "combat", "count", "minecraft:crossbow"),
		board("player-kills", "combat", "kills", "minecraft:player_head"),
		board("damage-dealt", "combat", "hearts", "minecraft:diamond_sword"),
		board("kills-per-hour", "combat", "kills_per_hour", "minecraft:clock")
	);

	private LeaderboardCatalog() {
	}

	public static List<LeaderboardInfo> all() {
		return ALL;
	}

	public static List<String> listedCategories() {
		return LISTED_CATEGORIES;
	}

	public static String categoryIcon(String category) {
		return CATEGORY_ICONS.getOrDefault(category, "");
	}

	public static List<LeaderboardInfo> listedIn(String category) {
		List<LeaderboardInfo> boards = new ArrayList<>();
		for (LeaderboardInfo info : ALL) {
			if (info.listed() && info.category().equals(category)) {
				boards.add(info);
			}
		}
		return boards;
	}

	public static Map<String, List<LeaderboardInfo>> listedByCategory() {
		Map<String, List<LeaderboardInfo>> grouped = new LinkedHashMap<>();
		for (String category : LISTED_CATEGORIES) {
			grouped.put(category, listedIn(category));
		}
		return grouped;
	}

	public static LeaderboardInfo find(String id) {
		for (LeaderboardInfo info : ALL) {
			if (info.id().equals(id)) {
				return info;
			}
		}
		return null;
	}

	public static LeaderboardInfo vanilla(String path, String group, String unit) {
		return new LeaderboardInfo(path, group, unit, "", false, false, false);
	}

	private static LeaderboardInfo crown(String id, String unit, String icon) {
		return new LeaderboardInfo(id, "crowns", unit, icon, false, false, false);
	}

	private static LeaderboardInfo board(String id, String category, String unit, String icon) {
		return new LeaderboardInfo(id, category, unit, icon, true, true, false);
	}

	private static LeaderboardInfo hiddenCompare(String id, String category, String unit, String icon) {
		return new LeaderboardInfo(id, category, unit, icon, true, false, false);
	}

	private static LeaderboardInfo lower(String id, String category, String unit, String icon) {
		return new LeaderboardInfo(id, category, unit, icon, true, true, true);
	}
}
