package webui.stats.mc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WebConfig {
	public static final int DEFAULT_PORT = 25580;
	public static final String DEFAULT_BIND = "0.0.0.0";
	public static final String DEFAULT_WORLD = "";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final String bind;
	private final int port;
	private final String world;

	public WebConfig(String bind, int port, String world) {
		this.bind = bind;
		this.port = port;
		this.world = world;
	}

	public String bind() {
		return bind;
	}

	public int port() {
		return port;
	}

	public String world() {
		return world;
	}

	public static WebConfig defaults() {
		return new WebConfig(DEFAULT_BIND, DEFAULT_PORT, DEFAULT_WORLD);
	}

	public static WebConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("mc-stats-webui.json");
		WebConfig loaded = defaults();
		try {
			Files.createDirectories(path.getParent());
			if (!Files.isRegularFile(path)) {
				write(path, loaded);
				McStatsWebui.LOGGER.info("Wrote default config {}", path);
				return loaded;
			}
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = parse(JsonParser.parseReader(reader).getAsJsonObject());
			}
			write(path, loaded);
		} catch (Exception e) {
			McStatsWebui.LOGGER.warn("Could not read {}, using defaults", path, e);
			return defaults();
		}
		return loaded;
	}

	private static WebConfig parse(JsonObject json) {
		int port = json.has("port") ? json.get("port").getAsInt() : DEFAULT_PORT;
		if (port < 1 || port > 65535) {
			McStatsWebui.LOGGER.warn("Invalid port {}, using {}", port, DEFAULT_PORT);
			port = DEFAULT_PORT;
		}
		String bind = json.has("bind") ? json.get("bind").getAsString().trim() : DEFAULT_BIND;
		if (bind.isEmpty()) {
			bind = DEFAULT_BIND;
		}
		String world = json.has("world") ? json.get("world").getAsString().trim() : DEFAULT_WORLD;
		if (world.contains("..") || world.startsWith("/") || world.startsWith("\\")) {
			McStatsWebui.LOGGER.warn("Ignoring unsafe world folder {}", world);
			world = DEFAULT_WORLD;
		}
		return new WebConfig(bind, port, world);
	}

	private static void write(Path path, WebConfig config) throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("port", config.port());
		body.put("bind", config.bind());
		body.put("world", config.world());
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(body, writer);
			writer.write('\n');
		}
	}
}
