package webui.stats.mc.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import webui.stats.mc.McStatsWebui;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AdvancementProgressFile {
	private static final DateTimeFormatter TIME = new DateTimeFormatterBuilder()
		.parseCaseInsensitive()
		.appendPattern("yyyy-MM-dd HH:mm:ss Z")
		.toFormatter(Locale.ROOT);

	public final Set<String> done;
	public final Map<String, Long> obtainedAt;

	public AdvancementProgressFile(Set<String> done, Map<String, Long> obtainedAt) {
		this.done = Set.copyOf(done);
		this.obtainedAt = Map.copyOf(obtainedAt);
	}

	public static AdvancementProgressFile empty() {
		return new AdvancementProgressFile(Set.of(), Map.of());
	}

	public static AdvancementProgressFile read(Path path) {
		if (path == null || !Files.isRegularFile(path)) {
			return empty();
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				return empty();
			}
			return parse(parsed.getAsJsonObject());
		} catch (Exception e) {
			McStatsWebui.LOGGER.warn("Could not read advancements {}", path.getFileName(), e);
			return empty();
		}
	}

	private static AdvancementProgressFile parse(JsonObject root) {
		Set<String> done = new LinkedHashSet<>();
		Map<String, Long> obtainedAt = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			String id = entry.getKey();
			if ("DataVersion".equals(id) || !entry.getValue().isJsonObject()) {
				continue;
			}
			JsonObject body = entry.getValue().getAsJsonObject();
			if (!body.has("done") || !body.get("done").getAsBoolean()) {
				continue;
			}
			done.add(id);
			long at = earliest(body.get("criteria"));
			if (at > 0) {
				obtainedAt.put(id, at);
			}
		}
		return new AdvancementProgressFile(done, obtainedAt);
	}

	private static long earliest(JsonElement criteria) {
		if (criteria == null || !criteria.isJsonObject()) {
			return 0;
		}
		long first = 0;
		for (Map.Entry<String, JsonElement> entry : criteria.getAsJsonObject().entrySet()) {
			if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
				continue;
			}
			long at = parseTime(entry.getValue().getAsString());
			if (at <= 0) {
				continue;
			}
			if (first == 0 || at < first) {
				first = at;
			}
		}
		return first;
	}

	private static long parseTime(String raw) {
		try {
			return OffsetDateTime.parse(raw, TIME).toInstant().toEpochMilli();
		} catch (Exception ignored) {
			try {
				return Instant.parse(raw).toEpochMilli();
			} catch (Exception e) {
				return 0;
			}
		}
	}
}
