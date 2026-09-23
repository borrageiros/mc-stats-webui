package webui.stats.mc.web;

public record LeaderboardInfo(
	String id,
	String category,
	String unit,
	String icon,
	boolean listed,
	boolean compare,
	boolean lowerWins
) {
}
