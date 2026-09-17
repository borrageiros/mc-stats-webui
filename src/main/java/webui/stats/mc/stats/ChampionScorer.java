package webui.stats.mc.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ChampionScorer {
	private static final String[] CATEGORIES = {"combat", "milestones", "mining", "explore", "produce", "survive"};
	private static final double[] WEIGHTS = {0.22, 0.18, 0.16, 0.16, 0.14, 0.08};
	private static final double FLEX_WEIGHT = 0.06;

	private ChampionScorer() {
	}

	public static List<PlayerRecord> apply(List<PlayerRecord> players) {
		if (players.isEmpty()) {
			return players;
		}
		Map<UUID, Double> survival = new HashMap<>();
		Map<String, Double> max = new HashMap<>();
		for (String category : CATEGORIES) {
			max.put(category, 0.0);
		}
		for (PlayerRecord player : players) {
			double deaths = player.values.getOrDefault("deaths", 0.0);
			double deathRate = player.playHours >= 0.25 ? deaths / player.playHours : deaths;
			double survive = Math.max(0, Math.log(1 + player.playHours) - deathRate * 0.25);
			survival.put(player.uuid, survive);
			for (String category : CATEGORIES) {
				double raw = raw(player, category, survival);
				if (raw > max.get(category)) {
					max.put(category, raw);
				}
			}
		}
		Map<UUID, Integer> flex = new HashMap<>();
		for (PlayerRecord player : players) {
			flex.put(player.uuid, 0);
		}
		int topN = Math.min(5, players.size());
		for (String category : CATEGORIES) {
			List<PlayerRecord> ranked = new ArrayList<>(players);
			ranked.sort(Comparator.comparingDouble((PlayerRecord player) -> raw(player, category, survival)).reversed());
			int given = 0;
			for (PlayerRecord player : ranked) {
				if (given >= topN) {
					break;
				}
				if (raw(player, category, survival) <= 0) {
					continue;
				}
				flex.put(player.uuid, flex.get(player.uuid) + 1);
				given += 1;
			}
		}
		double maxFlex = 0;
		for (int count : flex.values()) {
			if (count > maxFlex) {
				maxFlex = count;
			}
		}
		List<PlayerRecord> scored = new ArrayList<>();
		for (PlayerRecord player : players) {
			double total = 0;
			Map<String, Double> grades = new LinkedHashMap<>();
			for (int i = 0; i < CATEGORIES.length; i++) {
				double grade = normalize(raw(player, CATEGORIES[i], survival), max.get(CATEGORIES[i]));
				grades.put(CATEGORIES[i], grade);
				total += grade * WEIGHTS[i];
			}
			double flexGrade = normalize(flex.get(player.uuid), maxFlex);
			grades.put("flex", flexGrade);
			total += flexGrade * FLEX_WEIGHT;
			scored.add(player.withChampion(total, grades));
		}
		scored.sort(
			Comparator.comparingDouble((PlayerRecord player) -> player.championScore)
				.reversed()
				.thenComparing(player -> player.name.toLowerCase(Locale.ROOT))
		);
		return scored;
	}

	private static double raw(PlayerRecord player, String category, Map<UUID, Double> survival) {
		if ("survive".equals(category)) {
			return survival.getOrDefault(player.uuid, 0.0);
		}
		return player.categories.getOrDefault(category, 0.0);
	}

	private static double normalize(double value, double max) {
		if (max <= 0 || value <= 0) {
			return 0;
		}
		return 100.0 * value / max;
	}
}
