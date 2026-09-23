package webui.stats.mc.web;

import com.sun.net.httpserver.HttpExchange;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import webui.stats.mc.McStatsWebui;
import webui.stats.mc.WebConfig;
import webui.stats.mc.stats.AdvancementCatalog;
import webui.stats.mc.stats.AdvancementInfo;
import webui.stats.mc.stats.AdvancementService;
import webui.stats.mc.stats.PlayerRecord;
import webui.stats.mc.stats.StatsCache;
import webui.stats.mc.stats.StatsService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ApiHandler {
	private final WebConfig config;

	public ApiHandler(WebConfig config) {
		this.config = config;
	}

	public void handle(HttpExchange exchange, MinecraftServer server) throws IOException {
		String method = exchange.getRequestMethod();
		if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
			HttpJson.error(exchange, 405, "method_not_allowed", "Use GET");
			return;
		}

		String path = exchange.getRequestURI().getPath();
		if (path.endsWith("/") && path.length() > 1) {
			path = path.substring(0, path.length() - 1);
		}

		try {
			List<PlayerRecord> players = onServerThread(server, StatsCache.get()::snapshot);
			switch (path) {
				case "/api" -> HttpJson.send(exchange, 200, apiIndex());
				case "/api/health" -> HttpJson.send(exchange, 200, health());
				case "/api/status" -> HttpJson.send(exchange, 200, status(players));
				case "/api/players" -> HttpJson.send(exchange, 200, StatsService.playersPayload(players));
				case "/api/leaderboards" -> HttpJson.send(exchange, 200, StatsService.catalogPayload());
				case "/api/crowns" -> HttpJson.send(exchange, 200, StatsService.crownsPayload(players));
				case "/api/advancements" -> HttpJson.send(exchange, 200, AdvancementService.catalogPayload(players));
				case "/api/advancements/most" -> {
					LeaderboardInfo info = LeaderboardCatalog.find("advancements");
					HttpJson.send(exchange, 200, StatsService.leaderboardPayload(info, players));
				}
				default -> handleDynamic(exchange, path, players);
			}
		} catch (Exception e) {
			McStatsWebui.LOGGER.error("API request failed", e);
			HttpJson.error(exchange, 500, "internal_error", "Internal server error");
		}
	}

	private void handleDynamic(HttpExchange exchange, String path, List<PlayerRecord> players) throws IOException {
		if (path.startsWith("/api/players/")) {
			String name = decode(path.substring("/api/players/".length()));
			PlayerRecord player = StatsService.find(players, name);
			if (player == null) {
				HttpJson.error(exchange, 404, "not_found", "Player not found");
				return;
			}
			HttpJson.send(exchange, 200, StatsService.playerPayload(player, players));
			return;
		}
		if (path.startsWith("/api/leaderboards/")) {
			String id = decode(path.substring("/api/leaderboards/".length()));
			LeaderboardInfo info = StatsService.resolve(id);
			if (info == null) {
				HttpJson.error(exchange, 404, "not_found", "Unknown leaderboard");
				return;
			}
			HttpJson.send(exchange, 200, StatsService.leaderboardPayload(info, players));
			return;
		}
		if (path.startsWith("/api/advancements/")) {
			String raw = decode(path.substring("/api/advancements/".length()));
			String id = toAdvancementId(raw);
			AdvancementInfo info = AdvancementCatalog.get().find(id);
			if (info == null) {
				HttpJson.error(exchange, 404, "not_found", "Unknown advancement");
				return;
			}
			HttpJson.send(exchange, 200, AdvancementService.detailPayload(info, players));
			return;
		}
		HttpJson.error(exchange, 404, "not_found", "Unknown endpoint");
	}

	private Map<String, Object> apiIndex() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("health", "/api/health");
		body.put("status", "/api/status");
		body.put("players", "/api/players");
		body.put("leaderboards", "/api/leaderboards");
		body.put("crowns", "/api/crowns");
		body.put("advancements", "/api/advancements");
		return body;
	}

	private Map<String, Object> health() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("mod", McStatsWebui.MOD_ID);
		body.put("version", modVersion());
		body.put("minecraft", minecraftVersion());
		return body;
	}

	private Map<String, Object> status(List<PlayerRecord> players) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("bind", config.bind());
		body.put("trackedPlayers", players.size());
		body.put("branding", config.brandingPayload());
		return body;
	}

	public Path resolveWorld(MinecraftServer server) {
		String folder = config.world();
		if (folder.isEmpty()) {
			return server.getWorldPath(LevelResource.ROOT);
		}
		return FabricLoader.getInstance().getGameDir().resolve(folder);
	}

	private <T> T onServerThread(MinecraftServer server, ServerRead<T> read) throws Exception {
		CompletableFuture<T> future = new CompletableFuture<>();
		server.execute(() -> {
			try {
				future.complete(read.apply(server));
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future.get(5, TimeUnit.SECONDS);
	}

	private static String toAdvancementId(String path) {
		int slash = path.indexOf('/');
		if (slash <= 0) {
			return path;
		}
		return path.substring(0, slash) + ":" + path.substring(slash + 1);
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String modVersion() {
		return FabricLoader.getInstance()
			.getModContainer(McStatsWebui.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	private static String minecraftVersion() {
		return FabricLoader.getInstance()
			.getModContainer("minecraft")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	@FunctionalInterface
	private interface ServerRead<T> {
		T apply(MinecraftServer server);
	}
}
