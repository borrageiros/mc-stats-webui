package webui.stats.mc.web;

import com.sun.net.httpserver.HttpExchange;
import webui.stats.mc.McStatsWebui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.stream.Stream;

public final class StaticHandler {
	private static final String ROOT = "/assets/mc-stats-webui/web";

	private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

	public void preload() {
		cache.clear();
		URL index = McStatsWebui.class.getResource(ROOT + "/index.html");
		if (index == null) {
			McStatsWebui.LOGGER.error("Frontend is not packaged in the mod JAR");
			return;
		}
		try {
			URLConnection connection = index.openConnection();
			if (connection instanceof JarURLConnection jarConnection) {
				cacheJar(jarConnection.getJarFile());
			} else {
				cacheDirectory(Path.of(index.toURI()).getParent());
			}
		} catch (Exception e) {
			McStatsWebui.LOGGER.error("Could not cache frontend from {}", index, e);
			cacheResource(ROOT + "/index.html");
			cacheResource(ROOT + "/200.html");
		}
		McStatsWebui.LOGGER.info("Cached {} frontend files", cache.size());
	}

	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
			exchange.sendResponseHeaders(405, -1);
			exchange.close();
			return;
		}

		String path = exchange.getRequestURI().getPath();
		path = URLDecoder.decode(path, StandardCharsets.UTF_8);
		if (path.contains("..")) {
			exchange.sendResponseHeaders(400, -1);
			exchange.close();
			return;
		}
		if (path.endsWith("/")) {
			path = path + "index.html";
		}
		if (path.equals("")) {
			path = "/index.html";
		}

		boolean head = "HEAD".equalsIgnoreCase(method);
		String resource = ROOT + path;
		byte[] bytes = load(resource);
		if (bytes == null && isAsset(path)) {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
			return;
		}
		if (bytes == null) {
			resource = ROOT + "/200.html";
			bytes = load(resource);
		}
		if (bytes == null) {
			resource = ROOT + "/index.html";
			bytes = load(resource);
		}
		if (bytes == null) {
			send(
				exchange,
				503,
				"text/plain; charset=utf-8",
				"Frontend is not packaged. Rebuild the mod JAR, stop Minecraft, then replace the mc-stats-webui jar in mods/."
					.getBytes(StandardCharsets.UTF_8),
				head,
				resource
			);
			return;
		}

		send(exchange, 200, mime(resource), bytes, head, resource);
	}

	private byte[] load(String resource) {
		byte[] cached = cache.get(resource);
		if (cached != null) {
			return cached;
		}
		return cacheResource(resource);
	}

	private byte[] cacheResource(String resource) {
		try (InputStream stream = McStatsWebui.class.getResourceAsStream(resource)) {
			if (stream == null) {
				return null;
			}
			byte[] bytes = stream.readAllBytes();
			cache.put(resource, bytes);
			return bytes;
		} catch (ZipException e) {
			McStatsWebui.LOGGER.error(
				"Mod JAR was replaced while Minecraft is running. Stop the server, copy the new JAR into mods, then start again."
			);
			return null;
		} catch (IOException e) {
			McStatsWebui.LOGGER.error("Could not read {}", resource, e);
			return null;
		}
	}

	private void cacheJar(JarFile jar) throws IOException {
		String prefix = ROOT.substring(1) + "/";
		jar.stream()
			.filter(entry -> !entry.isDirectory() && entry.getName().startsWith(prefix))
			.forEach(entry -> {
				try (InputStream stream = jar.getInputStream(entry)) {
					cache.put("/" + entry.getName(), stream.readAllBytes());
				} catch (IOException e) {
					McStatsWebui.LOGGER.error("Could not cache {}", entry.getName(), e);
				}
			});
	}

	private void cacheDirectory(Path dir) throws IOException {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.filter(Files::isRegularFile).forEach(file -> {
				String relative = dir.relativize(file).toString().replace('\\', '/');
				try {
					cache.put(ROOT + "/" + relative, Files.readAllBytes(file));
				} catch (IOException e) {
					McStatsWebui.LOGGER.error("Could not cache {}", file, e);
				}
			});
		}
	}

	private static void send(
		HttpExchange exchange,
		int status,
		String contentType,
		byte[] bytes,
		boolean head,
		String resource
	) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", contentType);
		if (status == 200) {
			if (resource.contains("/_app/")) {
				exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
			} else {
				exchange.getResponseHeaders().set("Cache-Control", "no-cache");
			}
		}
		exchange.sendResponseHeaders(status, head ? -1 : bytes.length);
		if (!head) {
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		} else {
			exchange.close();
		}
	}

	private static boolean isAsset(String path) {
		String lower = path.toLowerCase(Locale.ROOT);
		return lower.contains(".") && !lower.endsWith(".html");
	}

	private static String mime(String resource) {
		String lower = resource.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".html")) {
			return "text/html; charset=utf-8";
		}
		if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
			return "text/javascript; charset=utf-8";
		}
		if (lower.endsWith(".css")) {
			return "text/css; charset=utf-8";
		}
		if (lower.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		if (lower.endsWith(".webp")) {
			return "image/webp";
		}
		if (lower.endsWith(".ico")) {
			return "image/x-icon";
		}
		if (lower.endsWith(".woff2")) {
			return "font/woff2";
		}
		if (lower.endsWith(".ttf")) {
			return "font/ttf";
		}
		if (lower.endsWith(".json") || lower.endsWith(".map")) {
			return "application/json";
		}
		return "application/octet-stream";
	}
}
