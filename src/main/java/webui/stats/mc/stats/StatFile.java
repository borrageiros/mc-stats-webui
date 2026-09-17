package webui.stats.mc.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class StatFile {
	private final JsonObject stats;

	public StatFile(JsonObject root) {
		JsonElement stats = root.get("stats");
		this.stats = stats != null && stats.isJsonObject() ? stats.getAsJsonObject() : new JsonObject();
	}

	public long custom(String name) {
		return value("minecraft:custom", namespaced(name));
	}

	public long killed(String name) {
		return value("minecraft:killed", namespaced(name));
	}

	public long killedBy(String name) {
		return value("minecraft:killed_by", namespaced(name));
	}

	public long pickedUp(String name) {
		return value("minecraft:picked_up", namespaced(name));
	}

	public long mined(String name) {
		return value("minecraft:mined", namespaced(name));
	}

	public long sum(String type) {
		return sum(type, key -> true);
	}

	public long sum(String type, Set<String> keys) {
		return sum(type, keys::contains);
	}

	public long sum(String type, Predicate<String> keys) {
		JsonObject group = group(type);
		if (group == null) {
			return 0;
		}
		long total = 0;
		for (Map.Entry<String, JsonElement> entry : group.entrySet()) {
			if (keys.test(entry.getKey())) {
				total += asLong(entry.getValue());
			}
		}
		return total;
	}

	public Map<String, Long> groupValues(String type) {
		Map<String, Long> values = new java.util.LinkedHashMap<>();
		JsonObject group = group(type);
		if (group == null) {
			return values;
		}
		for (Map.Entry<String, JsonElement> entry : group.entrySet()) {
			values.put(entry.getKey(), asLong(entry.getValue()));
		}
		return values;
	}

	public long value(String type, String key) {
		JsonObject group = group(type);
		if (group == null || !group.has(key)) {
			return 0;
		}
		return asLong(group.get(key));
	}

	private JsonObject group(String type) {
		JsonElement element = stats.get(type);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static long asLong(JsonElement element) {
		try {
			return element.getAsLong();
		} catch (Exception e) {
			return 0;
		}
	}

	private static String namespaced(String name) {
		if (name.indexOf(':') >= 0) {
			return name;
		}
		return "minecraft:" + name.toLowerCase(Locale.ROOT);
	}
}
