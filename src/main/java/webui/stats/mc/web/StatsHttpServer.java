package webui.stats.mc.web;

import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import webui.stats.mc.McStatsWebui;
import webui.stats.mc.WebConfig;
import webui.stats.mc.stats.AdvancementCatalog;
import webui.stats.mc.stats.StatsCache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StatsHttpServer {
	private static final StatsHttpServer INSTANCE = new StatsHttpServer();

	private WebConfig config = WebConfig.defaults();
	private ApiHandler apiHandler = new ApiHandler(config);
	private final StaticHandler staticHandler = new StaticHandler();
	private final ServerIconHandler serverIconHandler = new ServerIconHandler();
	private final MojangProxy mojangProxy = new MojangProxy();
	private HttpServer httpServer;
	private ExecutorService executor;
	private volatile MinecraftServer minecraft;

	private StatsHttpServer() {
	}

	public static StatsHttpServer getInstance() {
		return INSTANCE;
	}

	public synchronized void start(MinecraftServer server) throws IOException {
		stop();
		this.config = WebConfig.load();
		this.apiHandler = new ApiHandler(config);
		this.minecraft = server;
		InetSocketAddress address = new InetSocketAddress(config.bind(), config.port());
		httpServer = HttpServer.create(address, 0);
		executor = Executors.newFixedThreadPool(4, runnable -> {
			Thread thread = new Thread(runnable, "mc-stats-webui");
			thread.setDaemon(true);
			return thread;
		});
		httpServer.setExecutor(executor);
		staticHandler.preload();
		Path world = apiHandler.resolveWorld(server);
		AdvancementCatalog.get().boot(server);
		StatsCache.get().boot(world, Map.of());
		httpServer.createContext("/api", exchange -> {
			try {
				MinecraftServer current = minecraft;
				if (current == null) {
					HttpJson.error(exchange, 503, "unavailable", "Minecraft server is not ready");
					return;
				}
				apiHandler.handle(exchange, current);
			} catch (Exception e) {
				McStatsWebui.LOGGER.error("Unhandled API error", e);
				if (exchange.getResponseCode() == -1) {
					HttpJson.error(exchange, 500, "internal_error", "Internal server error");
				}
			} finally {
				exchange.close();
			}
		});
		httpServer.createContext("/mojang", exchange -> {
			try {
				mojangProxy.handle(exchange);
			} catch (Exception e) {
				McStatsWebui.LOGGER.error("Unhandled Mojang proxy error", e);
			} finally {
				exchange.close();
			}
		});
		httpServer.createContext("/", exchange -> {
			try {
				String path = exchange.getRequestURI().getPath();
				if (path.startsWith("/api") || path.startsWith("/mojang")) {
					HttpJson.error(exchange, 404, "not_found", "Unknown endpoint");
					return;
				}
				if ("/server-icon.png".equals(path)) {
					serverIconHandler.handle(exchange);
					return;
				}
				staticHandler.handle(exchange);
			} catch (Exception e) {
				McStatsWebui.LOGGER.error("Unhandled static error", e);
			} finally {
				exchange.close();
			}
		});
		httpServer.start();
		McStatsWebui.LOGGER.info(
			"Stats web UI listening on http://{}:{}/ world={}",
			config.bind(),
			config.port(),
			config.world().isEmpty() ? "(active save)" : config.world()
		);
	}

	public synchronized void stop() {
		if (httpServer != null) {
			httpServer.stop(0);
			httpServer = null;
		}
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		minecraft = null;
		StatsCache.get().clear();
		AdvancementCatalog.get().clear();
	}
}
