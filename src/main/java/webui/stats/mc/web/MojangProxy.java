package webui.stats.mc.web;

import com.sun.net.httpserver.HttpExchange;
import webui.stats.mc.McStatsWebui;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public final class MojangProxy {
	private static final HttpClient CLIENT = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(6))
		.build();
	private static final Pattern UUID = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[0-9a-f]{32}");
	private static final Pattern TEXTURE = Pattern.compile("(?i)[0-9a-f]+");

	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
			HttpJson.error(exchange, 405, "method_not_allowed", "Use GET");
			return;
		}
		URI target = resolve(exchange.getRequestURI().getPath());
		if (target == null) {
			HttpJson.error(exchange, 404, "not_found", "Unknown Mojang proxy path");
			return;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder(target)
				.timeout(Duration.ofSeconds(8))
				.header("User-Agent", "mc-stats-webui/1.0")
				.GET()
				.build();
			HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
			String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
			exchange.getResponseHeaders().set("Content-Type", contentType);
			boolean texture = target.getHost().contains("textures.minecraft.net");
			exchange.getResponseHeaders().set(
				"Cache-Control",
				texture ? "public, max-age=86400, immutable" : "public, max-age=300"
			);
			boolean head = "HEAD".equalsIgnoreCase(method);
			byte[] body = response.body() == null ? new byte[0] : response.body();
			exchange.sendResponseHeaders(response.statusCode(), head ? -1 : body.length);
			if (!head) {
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(body);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			HttpJson.error(exchange, 502, "bad_gateway", "Mojang request interrupted");
		} catch (Exception e) {
			McStatsWebui.LOGGER.warn("Mojang proxy failed for {}", target, e);
			if (exchange.getResponseCode() == -1) {
				HttpJson.error(exchange, 502, "bad_gateway", "Could not reach Mojang");
			}
		}
	}

	private static URI resolve(String path) {
		if (path.startsWith("/mojang/session/minecraft/profile/")) {
			String uuid = path.substring("/mojang/session/minecraft/profile/".length());
			if (!UUID.matcher(uuid).matches()) {
				return null;
			}
			return URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
		}
		if (path.startsWith("/mojang/textures/texture/")) {
			String hash = path.substring("/mojang/textures/texture/".length());
			if (!TEXTURE.matcher(hash).matches()) {
				return null;
			}
			return URI.create("https://textures.minecraft.net/texture/" + hash);
		}
		return null;
	}
}
