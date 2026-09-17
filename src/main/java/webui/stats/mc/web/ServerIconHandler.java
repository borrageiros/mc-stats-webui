package webui.stats.mc.web;

import com.sun.net.httpserver.HttpExchange;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerIconHandler {
	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
			exchange.sendResponseHeaders(405, -1);
			exchange.close();
			return;
		}

		Path icon = FabricLoader.getInstance().getGameDir().resolve("server-icon.png");
		if (!Files.isRegularFile(icon)) {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
			return;
		}

		byte[] bytes = Files.readAllBytes(icon);
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.getResponseHeaders().set("Cache-Control", "no-cache");
		boolean head = "HEAD".equalsIgnoreCase(method);
		exchange.sendResponseHeaders(200, head ? -1 : bytes.length);
		if (!head) {
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		} else {
			exchange.close();
		}
	}
}
