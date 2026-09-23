package webui.stats.mc.stats;

import java.util.Map;
import java.util.Set;
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
	public final Set<String> advancements;
	public final Map<String, Long> advancementTimes;

	public PlayerRecord(
		UUID uuid,
		String name,
		double playHours,
		double championScore,
		Map<String, Double> values,
		Map<String, Double> categories,
		Map<String, Double> grades,
		Map<String, Map<String, Long>> vanilla,
		Set<String> advancements,
		Map<String, Long> advancementTimes
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
		this.advancements = Set.copyOf(advancements);
		this.advancementTimes = Map.copyOf(advancementTimes);
	}

	public PlayerRecord withChampion(double score, Map<String, Double> nextGrades) {
		return new PlayerRecord(
			uuid,
			name,
			playHours,
			score,
			values,
			categories,
			nextGrades,
			vanilla,
			advancements,
			advancementTimes
		);
	}

	public PlayerRecord withAdvancements(Set<String> nextAdvancements, Map<String, Long> nextTimes) {
		return new PlayerRecord(
			uuid,
			name,
			playHours,
			championScore,
			values,
			categories,
			grades,
			vanilla,
			nextAdvancements,
			nextTimes
		);
	}

	public long vanillaRaw(String group, String id) {
		Map<String, Long> values = vanilla.get(group);
		if (values == null) {
			return 0;
		}
		return values.getOrDefault(id, 0L);
	}

	public boolean hasAdvancement(String id) {
		return advancements.contains(id);
	}

	public long advancementTime(String id) {
		return advancementTimes.getOrDefault(id, 0L);
	}

	public double value(String leaderboardId) {
		return switch (leaderboardId) {
			case "champion" -> championScore;
			case "dedicated", "play-time" -> playHours;
			case "efficient" -> scorePerHour;
			case "advancements" -> advancements.size();
			default -> values.getOrDefault(leaderboardId, 0.0);
		};
	}
}
