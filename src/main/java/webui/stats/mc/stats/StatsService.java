package webui.stats.mc.stats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import webui.stats.mc.McStatsWebui;
import webui.stats.mc.web.LeaderboardCatalog;
import webui.stats.mc.web.LeaderboardInfo;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StatsService {
	private StatsService() {
	}

	public static List<PlayerRecord> load(Path worldRoot, Map<UUID, String> knownNames) {
		NameDirectory names = nameDirectory(knownNames);
		Path directory = statsDirectory(worldRoot);
		List<Path> files = new ArrayList<>();
		if (directory != null && Files.isDirectory(directory)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
				for (Path path : stream) {
					files.add(path);
				}
			} catch (IOException e) {
				McStatsWebui.LOGGER.error("Could not list stats directory {}", directory, e);
			}
		}

		Set<UUID> uuids = new HashSet<>();
		Map<UUID, StatFile> parsed = new HashMap<>();
		for (Path path : files) {
			UUID uuid = uuidFromStatsFile(path);
			if (uuid == null) {
				continue;
			}
			StatFile stats = readStatsFile(path);
			if (stats == null) {
				continue;
			}
			uuids.add(uuid);
			parsed.put(uuid, stats);
		}
		names.resolveMissing(uuids);

		List<PlayerRecord> players = new ArrayList<>();
		for (Map.Entry<UUID, StatFile> entry : parsed.entrySet()) {
			UUID uuid = entry.getKey();
			players.add(PlayerMetrics.compute(uuid, names.get(uuid), entry.getValue()));
		}
		return ChampionScorer.apply(players);
	}

	public static PlayerRecord loadOne(Path worldRoot, UUID uuid, Map<UUID, String> knownNames) {
		Path path = statsFile(worldRoot, uuid);
		if (path == null || !Files.isRegularFile(path)) {
			return null;
		}
		StatFile stats = readStatsFile(path);
		if (stats == null) {
			return null;
		}
		NameDirectory names = nameDirectory(knownNames);
		names.resolveMissing(Set.of(uuid));
		return PlayerMetrics.compute(uuid, names.get(uuid), stats);
	}

	public static Path statsDirectory(Path worldRoot) {
		if (worldRoot == null) {
			return null;
		}
		List<Path> candidates = new ArrayList<>();
		candidates.add(worldRoot.resolve("players").resolve("stats"));
		candidates.add(worldRoot.resolve("stats"));
		for (Path path : candidates) {
			if (Files.isDirectory(path)) {
				return path;
			}
		}
		return candidates.getFirst();
	}

	public static Path statsDirectory(MinecraftServer server) {
		return statsDirectory(server.getWorldPath(LevelResource.ROOT));
	}

	private static NameDirectory nameDirectory(Map<UUID, String> knownNames) {
		NameDirectory names = new NameDirectory();
		names.loadLocal(FabricLoader.getInstance().getGameDir());
		for (Map.Entry<UUID, String> entry : knownNames.entrySet()) {
			names.put(entry.getKey(), entry.getValue());
		}
		return names;
	}

	private static Path statsFile(Path worldRoot, UUID uuid) {
		Path directory = statsDirectory(worldRoot);
		if (directory == null) {
			return null;
		}
		return directory.resolve(uuid + ".json");
	}

	private static UUID uuidFromStatsFile(Path path) {
		String filename = path.getFileName().toString();
		if (!filename.endsWith(".json")) {
			return null;
		}
		try {
			return UUID.fromString(filename.substring(0, filename.length() - 5));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static StatFile readStatsFile(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			return new StatFile(root);
		} catch (Exception e) {
			McStatsWebui.LOGGER.warn("Could not read stats file {}", path.getFileName(), e);
			return null;
		}
	}

	public static Map<String, Object> playersPayload(List<PlayerRecord> players) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (PlayerRecord player : players) {
			list.add(playerSummary(player));
		}
		return Map.of("players", list);
	}

	public static Map<String, Object> playerPayload(PlayerRecord player, List<PlayerRecord> players) {
		Map<String, Object> item = playerSummary(player);
		item.put("championScore", round(player.championScore));
		item.put("championDisplay", display("score", player.championScore));
		item.put("championRank", rank(players, "champion", player));
		item.put("playHours", round(player.playHours));
		item.put("playHoursDisplay", display("hours", player.playHours));
		item.put("scorePerHour", round(player.scorePerHour));
		item.put("scorePerHourDisplay", display("score_per_hour", player.scorePerHour));
		item.put("trackedPlayers", players.size());
		Map<String, Object> grades = new LinkedHashMap<>();
		for (Map.Entry<String, Double> grade : player.grades.entrySet()) {
			grades.put(grade.getKey(), round(grade.getValue()));
		}
		item.put("grades", grades);
		Map<String, Object> stats = new LinkedHashMap<>();
		for (LeaderboardInfo info : LeaderboardCatalog.all()) {
			double value = player.value(info.id());
			Map<String, Object> stat = new LinkedHashMap<>();
			stat.put("value", round(value));
			stat.put("display", display(info.unit(), value));
			stat.put("unit", info.unit());
			stat.put("category", info.category());
			stat.put("rank", rank(players, info.id(), player));
			stats.put(info.id(), stat);
		}
		item.put("stats", stats);
		item.put("vanilla", vanillaPayload(player, players));
		return item;
	}

	private static Map<String, Object> vanillaPayload(PlayerRecord player, List<PlayerRecord> players) {
		Map<String, Object> body = new LinkedHashMap<>();
		for (String group : VanillaCatalog.GROUPS) {
			List<Map<String, Object>> rows = new ArrayList<>();
			for (String id : VanillaCatalog.keys(group)) {
				long raw = player.vanillaRaw(group, id);
				String unit = vanillaUnit(group, id);
				double value = vanillaValue(group, id, raw);
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", id);
				row.put("value", round(value));
				row.put("display", display(unit, value));
				row.put("unit", unit);
				row.put("rank", vanillaRank(players, group, id, raw));
				rows.add(row);
			}
			body.put(group, rows);
		}
		return body;
	}

	private static String vanillaUnit(String group, String id) {
		if (!"custom".equals(group)) {
			return "count";
		}
		if (id.endsWith("_one_cm")) {
			return "km";
		}
		if (id.contains("damage_")) {
			return "hearts";
		}
		if (id.endsWith("_time") || id.startsWith("minecraft:time_since_") || "minecraft:sneak_time".equals(id)) {
			return "hours";
		}
		return "count";
	}

	private static double vanillaValue(String group, String id, long raw) {
		String unit = vanillaUnit(group, id);
		return switch (unit) {
			case "km" -> raw / 100_000.0;
			case "hours" -> raw / 20.0 / 3600.0;
			case "hearts" -> raw / 20.0;
			default -> raw;
		};
	}

	private static int vanillaRank(List<PlayerRecord> players, String group, String id, long raw) {
		int place = 1;
		for (PlayerRecord player : players) {
			if (player.vanillaRaw(group, id) > raw) {
				place += 1;
			}
		}
		return place;
	}

	public static LeaderboardInfo resolve(String path) {
		LeaderboardInfo info = LeaderboardCatalog.find(path);
		if (info != null) {
			return info;
		}
		int slash = path.indexOf('/');
		if (slash <= 0) {
			return null;
		}
		String group = path.substring(0, slash);
		String statId = path.substring(slash + 1);
		if (!VanillaCatalog.isGroup(group) || !VanillaCatalog.keys(group).contains(statId)) {
			return null;
		}
		return new LeaderboardInfo(path, statId, group, vanillaUnit(group, statId));
	}

	public static Map<String, Object> leaderboardPayload(LeaderboardInfo info, List<PlayerRecord> players) {
		List<Map<String, Object>> entries = new ArrayList<>();
		List<PlayerRecord> ranked = new ArrayList<>(players);
		ranked.sort(
			Comparator.comparingDouble((PlayerRecord player) -> boardValue(info, player))
				.reversed()
				.thenComparing(Comparator.comparingDouble((PlayerRecord player) -> player.playHours).reversed())
				.thenComparing(player -> player.name.toLowerCase(Locale.ROOT))
		);
		int rank = 1;
		for (PlayerRecord player : ranked) {
			double value = boardValue(info, player);
			if (value <= 0 && !"champion".equals(info.id())) {
				continue;
			}
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("rank", rank);
			entry.put("name", player.name);
			entry.put("uuid", player.uuid.toString());
			entry.put("value", round(value));
			entry.put("display", display(info.unit(), value));
			entries.add(entry);
			rank += 1;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", info.id());
		body.put("title", info.title());
		body.put("category", info.category());
		body.put("unit", info.unit());
		body.put("entries", entries);
		return body;
	}

	public static Map<String, Object> crownsPayload(List<PlayerRecord> players) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("champion", crown(best(players, "champion"), "score"));
		body.put("dedicated", crown(best(players, "dedicated"), "hours"));
		body.put("efficient", crown(best(players, "efficient"), "score_per_hour"));
		return body;
	}

	public static Map<String, Object> catalogPayload() {
		List<Map<String, Object>> items = new ArrayList<>();
		for (LeaderboardInfo info : LeaderboardCatalog.all()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", info.id());
			item.put("title", info.title());
			item.put("category", info.category());
			items.add(item);
		}
		Map<String, Object> vanilla = new LinkedHashMap<>();
		for (String group : VanillaCatalog.GROUPS) {
			vanilla.put(group, VanillaCatalog.keys(group));
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("leaderboards", items);
		body.put("vanilla", vanilla);
		return body;
	}

	public static PlayerRecord find(List<PlayerRecord> players, String name) {
		for (PlayerRecord player : players) {
			if (player.name.equalsIgnoreCase(name) || player.uuid.toString().equalsIgnoreCase(name)) {
				return player;
			}
		}
		return null;
	}

	private static Map<String, Object> playerSummary(PlayerRecord player) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("name", player.name);
		item.put("uuid", player.uuid.toString());
		return item;
	}

	private static double boardValue(LeaderboardInfo info, PlayerRecord player) {
		int slash = info.id().indexOf('/');
		if (slash > 0) {
			String group = info.id().substring(0, slash);
			if (VanillaCatalog.isGroup(group)) {
				String statId = info.id().substring(slash + 1);
				return vanillaValue(group, statId, player.vanillaRaw(group, statId));
			}
		}
		return player.value(info.id());
	}

	private static int rank(List<PlayerRecord> players, String id, PlayerRecord target) {
		double value = target.value(id);
		int place = 1;
		for (PlayerRecord player : players) {
			if (player.value(id) > value) {
				place += 1;
			}
		}
		return place;
	}

	private static PlayerRecord best(List<PlayerRecord> players, String id) {
		PlayerRecord best = null;
		double top = 0;
		for (PlayerRecord player : players) {
			double value = player.value(id);
			if (value > top) {
				top = value;
				best = player;
			}
		}
		return best;
	}

	private static Map<String, Object> crown(PlayerRecord player, String unit) {
		if (player == null) {
			return null;
		}
		double value = switch (unit) {
			case "hours" -> player.playHours;
			case "score_per_hour" -> player.scorePerHour;
			default -> player.championScore;
		};
		if (value <= 0) {
			return null;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", player.name);
		body.put("uuid", player.uuid.toString());
		body.put("score", round(value));
		body.put("display", display(unit, value));
		return body;
	}

	private static String display(String unit, double value) {
		return switch (unit) {
			case "hours" -> format(value, 2) + " h";
			case "km" -> format(value, 2) + " km";
			case "hearts" -> format(value, 1) + " ♥";
			case "kills_per_hour" -> format(value, 1) + "/h";
			case "score_per_hour" -> format(value, 1) + "/h";
			case "score" -> format(value, 1);
			default -> String.valueOf(Math.round(value));
		};
	}

	private static String format(double value, int decimals) {
		return String.format(Locale.US, "%." + decimals + "f", value);
	}

	private static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
