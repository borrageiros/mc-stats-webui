package webui.stats.mc.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import webui.stats.mc.McStatsWebui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NameDirectory {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
	private final Map<UUID, String> names = new HashMap<>();

	public void put(UUID uuid, String name) {
		if (name != null && !name.isBlank()) {
			names.put(uuid, name);
		}
	}

	public String get(UUID uuid) {
		String name = names.get(uuid);
		if (name != null) {
			return name;
		}
		return uuid.toString().substring(0, 8);
	}

	public boolean knows(UUID uuid) {
		return names.containsKey(uuid);
	}

	public void loadLocal(Path gameDir) {
		readUuidNameList(gameDir.resolve("usercache.json"));
		readUuidNameList(gameDir.resolve("whitelist.json"));
		readUuidNameList(gameDir.resolve("ops.json"));
		Path cache = cacheFile();
		if (Files.isRegularFile(cache)) {
			try (Reader reader = Files.newBufferedReader(cache, StandardCharsets.UTF_8)) {
				JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
					try {
						put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
					} catch (Exception ignored) {
					}
				}
			} catch (Exception e) {
				McStatsWebui.LOGGER.warn("Could not read name cache", e);
			}
		}
	}

	public void resolveMissing(Set<UUID> uuids) {
		Set<UUID> missing = new HashSet<>();
		for (UUID uuid : uuids) {
			if (!knows(uuid)) {
				missing.add(uuid);
			}
		}
		for (UUID uuid : missing) {
			String name = fetchMojang(uuid);
			if (name != null) {
				put(uuid, name);
			}
		}
		if (!missing.isEmpty()) {
			saveCache();
		}
	}

	public void saveCache() {
		JsonObject object = new JsonObject();
		for (Map.Entry<UUID, String> entry : names.entrySet()) {
			object.addProperty(entry.getKey().toString(), entry.getValue());
		}
		Path cache = cacheFile();
		try {
			Files.createDirectories(cache.getParent());
			try (Writer writer = Files.newBufferedWriter(cache, StandardCharsets.UTF_8)) {
				GSON.toJson(object, writer);
			}
		} catch (IOException e) {
			McStatsWebui.LOGGER.warn("Could not save name cache", e);
		}
	}

	private void readUuidNameList(Path path) {
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (!root.isJsonArray()) {
				return;
			}
			JsonArray array = root.getAsJsonArray();
			for (JsonElement element : array) {
				if (!element.isJsonObject()) {
					continue;
				}
				JsonObject object = element.getAsJsonObject();
				if (!object.has("uuid") || !object.has("name")) {
					continue;
				}
				try {
					put(UUID.fromString(object.get("uuid").getAsString()), object.get("name").getAsString());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception e) {
			McStatsWebui.LOGGER.warn("Could not read {}", path.getFileName(), e);
		}
	}

	private static String fetchMojang(UUID uuid) {
		String compact = uuid.toString().replace("-", "");
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + compact))
			.timeout(Duration.ofSeconds(6))
			.header("User-Agent", "mc-stats-webui")
			.GET()
			.build();
		try {
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
				return null;
			}
			JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
			if (!object.has("name")) {
				return null;
			}
			return object.get("name").getAsString();
		} catch (Exception e) {
			McStatsWebui.LOGGER.warn("Could not resolve name for {}", uuid);
			return null;
		}
	}

	private static Path cacheFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("mc-stats-webui-names.json");
	}
}
