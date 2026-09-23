package webui.stats.mc.stats;

public record AdvancementInfo(
	String id,
	String tab,
	String title,
	String description,
	String icon,
	String frame,
	boolean hidden
) {
}
