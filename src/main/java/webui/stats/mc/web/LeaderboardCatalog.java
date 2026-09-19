package webui.stats.mc.web;

import java.util.List;

public final class LeaderboardCatalog {
	private static final List<LeaderboardInfo> ALL = List.of(
		new LeaderboardInfo("champion", "Campeón", "crowns", "score"),
		new LeaderboardInfo("dedicated", "Más dedicado", "crowns", "hours"),
		new LeaderboardInfo("efficient", "Más eficiente", "crowns", "score_per_hour"),
		new LeaderboardInfo("hostile-kills", "Hostiles (sin farms)", "combat", "kills"),
		new LeaderboardInfo("farm-kills", "Farms", "combat", "kills"),
		new LeaderboardInfo("zombies", "Zombis", "combat", "kills"),
		new LeaderboardInfo("skeletons", "Esqueletos", "combat", "kills"),
		new LeaderboardInfo("creepers", "Creepers", "combat", "kills"),
		new LeaderboardInfo("endermen", "Endermans", "combat", "kills"),
		new LeaderboardInfo("blazes", "Blazes", "combat", "kills"),
		new LeaderboardInfo("wither-skeletons", "Wither skeletons", "combat", "kills"),
		new LeaderboardInfo("ender-dragon", "Dragón", "combat", "kills"),
		new LeaderboardInfo("wither", "Wither", "combat", "kills"),
		new LeaderboardInfo("warden", "Warden", "combat", "kills"),
		new LeaderboardInfo("elder-guardian", "Guardián anciano", "combat", "kills"),
		new LeaderboardInfo("bosses", "Jefes", "combat", "kills"),
		new LeaderboardInfo("raids-won", "Raids ganadas", "combat", "count"),
		new LeaderboardInfo("player-kills", "Jugadores matados", "combat", "kills"),
		new LeaderboardInfo("damage-dealt", "Daño melee", "combat", "hearts"),
		new LeaderboardInfo("kills-per-hour", "Kills por hora", "combat", "kills_per_hour"),
		new LeaderboardInfo("blocks-mined", "Bloques picados", "mining", "blocks"),
		new LeaderboardInfo("ores", "Ores", "mining", "blocks"),
		new LeaderboardInfo("diamonds-mined", "Diamantes picados", "mining", "blocks"),
		new LeaderboardInfo("iron-mined", "Hierro picado", "mining", "blocks"),
		new LeaderboardInfo("ancient-debris", "Ancient debris", "mining", "blocks"),
		new LeaderboardInfo("logs-mined", "Troncos", "mining", "blocks"),
		new LeaderboardInfo("sculk-mined", "Sculk", "mining", "blocks"),
		new LeaderboardInfo("tools-broken", "Herramientas rotas", "mining", "count"),
		new LeaderboardInfo("distance-total", "Distancia útil", "exploration", "km"),
		new LeaderboardInfo("distance-walk-sprint", "Andar y sprint", "exploration", "km"),
		new LeaderboardInfo("distance-walk", "Andar", "exploration", "km"),
		new LeaderboardInfo("distance-sprint", "Sprint", "exploration", "km"),
		new LeaderboardInfo("distance-swim", "Nadar", "exploration", "km"),
		new LeaderboardInfo("distance-boat", "Barco", "exploration", "km"),
		new LeaderboardInfo("distance-elytra", "Élitros", "exploration", "km"),
		new LeaderboardInfo("distance-horse", "Caballo", "exploration", "km"),
		new LeaderboardInfo("distance-happy-ghast", "Happy Ghast", "exploration", "km"),
		new LeaderboardInfo("distance-nautilus", "Nautilus", "exploration", "km"),
		new LeaderboardInfo("distance-minecart", "Vagoneta", "exploration", "km"),
		new LeaderboardInfo("distance-strider", "Strider", "exploration", "km"),
		new LeaderboardInfo("chests-opened", "Cofres abiertos", "exploration", "count"),
		new LeaderboardInfo("play-time", "Tiempo jugado", "life", "hours"),
		new LeaderboardInfo("animals-bred", "Animales criados", "life", "count"),
		new LeaderboardInfo("fish-caught", "Peces", "life", "count"),
		new LeaderboardInfo("villager-trades", "Tratos", "life", "count"),
		new LeaderboardInfo("items-enchanted", "Encantamientos", "life", "count"),
		new LeaderboardInfo("sleeps", "Veces dormido", "life", "count"),
		new LeaderboardInfo("jumps", "Saltos", "life", "count"),
		new LeaderboardInfo("deaths", "Muertes totales", "deaths", "count"),
		new LeaderboardInfo("killed-by-zombie", "Muertes por zombi", "deaths", "count"),
		new LeaderboardInfo("killed-by-creeper", "Muertes por creeper", "deaths", "count"),
		new LeaderboardInfo("killed-by-skeleton", "Muertes por esqueleto", "deaths", "count"),
		new LeaderboardInfo("killed-by-warden", "Muertes por warden", "deaths", "count")
	);

	private LeaderboardCatalog() {
	}

	public static List<LeaderboardInfo> all() {
		return ALL;
	}

	public static LeaderboardInfo find(String id) {
		for (LeaderboardInfo info : ALL) {
			if (info.id().equals(id)) {
				return info;
			}
		}
		return null;
	}
}
