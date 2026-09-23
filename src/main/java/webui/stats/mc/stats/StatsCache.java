package webui.stats.mc.stats;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import webui.stats.mc.McStatsWebui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsCache {
	private static final StatsCache INSTANCE = new StatsCache();

	private final Object lock = new Object();
	private final ConcurrentHashMap.KeySetView<UUID, Boolean> dirty = ConcurrentHashMap.newKeySet();
	private Path worldRoot;
	private Map<UUID, String> knownNames = Map.of();
	private List<PlayerRecord> players = List.of();
	private boolean ready;

	private StatsCache() {
	}

	public static StatsCache get() {
		return INSTANCE;
	}

	public void boot(Path worldRoot, Map<UUID, String> knownNames) {
		synchronized (lock) {
			this.worldRoot = worldRoot;
			this.knownNames = Map.copyOf(knownNames);
			this.players = StatsService.load(worldRoot, this.knownNames);
			this.ready = true;
			this.dirty.clear();
			McStatsWebui.LOGGER.info("Stats cache loaded ({} players)", this.players.size());
		}
	}

	public void clear() {
		synchronized (lock) {
			worldRoot = null;
			knownNames = Map.of();
			players = List.of();
			ready = false;
			dirty.clear();
		}
	}

	public boolean isReady() {
		return ready;
	}

	public void rememberName(UUID uuid, String name) {
		synchronized (lock) {
			Map<UUID, String> next = new LinkedHashMap<>(knownNames);
			next.put(uuid, name);
			knownNames = Map.copyOf(next);
		}
	}

	public void markDirty(UUID uuid) {
		dirty.add(uuid);
	}

	public void onStatsSaved(UUID uuid) {
		if (uuid == null) {
			return;
		}
		dirty.remove(uuid);
		reloadPlayer(uuid);
	}

	public void flushDirty(MinecraftServer server) {
		if (dirty.isEmpty()) {
			return;
		}
		List<UUID> batch = new ArrayList<>(dirty);
		for (UUID uuid : batch) {
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			if (player != null) {
				rememberName(uuid, player.getGameProfile().name());
				player.getAdvancements().save();
				player.getStats().save();
			} else {
				dirty.remove(uuid);
				reloadPlayer(uuid);
			}
		}
	}

	public List<PlayerRecord> snapshot(MinecraftServer server) {
		if (!isReady()) {
			AdvancementCatalog.get().ensure(server);
			Path root = worldRoot != null ? worldRoot : StatsService.statsDirectory(server);
			boot(root, Map.of());
		}
		AdvancementCatalog.get().ensure(server);
		flushDirty(server);
		synchronized (lock) {
			return List.copyOf(players);
		}
	}

	private void reloadPlayer(UUID uuid) {
		synchronized (lock) {
			if (!ready || worldRoot == null) {
				return;
			}
			PlayerRecord loaded = StatsService.loadOne(worldRoot, uuid, knownNames);
			Map<UUID, PlayerRecord> byId = new LinkedHashMap<>();
			for (PlayerRecord player : players) {
				byId.put(player.uuid, player);
			}
			if (loaded == null) {
				byId.remove(uuid);
			} else {
				byId.put(uuid, loaded);
			}
			players = ChampionScorer.apply(new ArrayList<>(byId.values()));
		}
	}
}
