package webui.stats.mc.stats;

import java.util.Map;
import java.util.UUID;

public final class PlayerRecord {
	public final UUID uuid;
	public final String name;
	public final double playHours;
	public final double championScore;
	public final double scorePerHour;
	public final Map<String, Double> values;
	public final Map<String, Double> categories;
	public final Map<String, Double> grades;
	public final Map<String, Map<String, Long>> vanilla;

	public PlayerRecord(
		UUID uuid,
		String name,
		double playHours,
		double championScore,
		Map<String, Double> values,
		Map<String, Double> categories,
		Map<String, Double> grades,
		Map<String, Map<String, Long>> vanilla
	) {
		this.uuid = uuid;
		this.name = name;
		this.playHours = playHours;
		this.championScore = championScore;
		this.scorePerHour = playHours >= 0.25 ? championScore / playHours : 0;
		this.values = values;
		this.categories = categories;
		this.grades = grades;
		this.vanilla = vanilla;
	}

	public PlayerRecord withChampion(double score, Map<String, Double> nextGrades) {
		return new PlayerRecord(uuid, name, playHours, score, values, categories, nextGrades, vanilla);
	}

	public long vanillaRaw(String group, String id) {
		Map<String, Long> values = vanilla.get(group);
		if (values == null) {
			return 0;
		}
		return values.getOrDefault(id, 0L);
	}

	public double value(String leaderboardId) {
		return switch (leaderboardId) {
			case "champion" -> championScore;
			case "dedicated", "play-time" -> playHours;
			case "efficient" -> scorePerHour;
			default -> values.getOrDefault(leaderboardId, 0.0);
		};
	}
}
