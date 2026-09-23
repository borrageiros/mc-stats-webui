package webui.stats.mc.stats;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdvancementService {
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

	private AdvancementService() {
	}

	public static Map<String, Object> catalogPayload(List<PlayerRecord> players) {
		AdvancementCatalog catalog = AdvancementCatalog.get();
		List<Map<String, Object>> tabs = new ArrayList<>();
		for (AdvancementTab tab : catalog.tabs()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", tab.id());
			item.put("title", tab.title());
			item.put("icon", tab.icon());
			tabs.add(item);
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (AdvancementInfo info : catalog.all()) {
			Map<String, Object> item = infoPayload(info);
			item.put("holders", holders(info.id(), players).size());
			items.add(item);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total", catalog.size());
		body.put("tabs", tabs);
		body.put("advancements", items);
		return body;
	}

	public static Map<String, Object> detailPayload(AdvancementInfo info, List<PlayerRecord> players) {
		List<PlayerRecord> holders = holders(info.id(), players);
		holders.sort(
			Comparator.comparingLong((PlayerRecord player) -> {
					long at = player.advancementTime(info.id());
					return at > 0 ? at : Long.MAX_VALUE;
				})
				.thenComparing(player -> player.name.toLowerCase(Locale.ROOT))
		);
		List<Map<String, Object>> entries = new ArrayList<>();
		int rank = 1;
		for (PlayerRecord player : holders) {
			long at = player.advancementTime(info.id());
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("rank", rank);
			entry.put("name", player.name);
			entry.put("uuid", player.uuid.toString());
			entry.put("value", at);
			entry.put("display", at > 0 ? WHEN.format(Instant.ofEpochMilli(at)) : "✓");
			entries.add(entry);
			rank += 1;
		}
		Map<String, Object> body = infoPayload(info);
		body.put("holders", entries.size());
		body.put("entries", entries);
		return body;
	}

	private static Map<String, Object> infoPayload(AdvancementInfo info) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("id", info.id());
		item.put("tab", info.tab());
		item.put("title", info.title());
		item.put("description", info.description());
		item.put("icon", info.icon());
		item.put("frame", info.frame());
		item.put("hidden", info.hidden());
		return item;
	}

	private static List<PlayerRecord> holders(String id, List<PlayerRecord> players) {
		List<PlayerRecord> holders = new ArrayList<>();
		for (PlayerRecord player : players) {
			if (player.hasAdvancement(id)) {
				holders.add(player);
			}
		}
		return holders;
	}
}
