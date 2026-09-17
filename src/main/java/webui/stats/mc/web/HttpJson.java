package webui.stats.mc.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpJson {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	private HttpJson() {
	}

	public static void send(HttpExchange exchange, int status, Object body) throws IOException {
		byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		boolean head = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
		exchange.sendResponseHeaders(status, head ? -1 : bytes.length);
		if (!head) {
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		} else {
			exchange.close();
		}
	}

	public static void error(HttpExchange exchange, int status, String code, String message) throws IOException {
		send(exchange, status, Map.of("error", code, "message", message));
	}
}
