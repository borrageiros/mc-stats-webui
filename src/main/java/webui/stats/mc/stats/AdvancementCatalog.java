package webui.stats.mc.stats;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AdvancementCatalog {
	private static final AdvancementCatalog INSTANCE = new AdvancementCatalog();
	private static final List<String> TAB_ORDER = List.of("story", "nether", "end", "adventure", "husbandry");

	private final Object lock = new Object();
	private List<AdvancementInfo> advancements = List.of();
	private List<AdvancementTab> tabs = List.of();
	private Map<String, AdvancementInfo> byId = Map.of();
	private boolean ready;

	private AdvancementCatalog() {
	}

	public static AdvancementCatalog get() {
		return INSTANCE;
	}

	public void ensure(MinecraftServer server) {
		if (ready) {
			return;
		}
		boot(server);
	}

	public void boot(MinecraftServer server) {
		synchronized (lock) {
			List<AdvancementInfo> next = new ArrayList<>();
			Map<String, Identifier> parents = new HashMap<>();
			Map<String, DisplayInfo> displays = new HashMap<>();
			for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
				Advancement advancement = holder.value();
				Identifier id = holder.id();
				advancement.parent().ifPresent(parent -> parents.put(id.toString(), parent));
				advancement.display().ifPresent(display -> displays.put(id.toString(), display));
			}
			Map<String, AdvancementTab> tabMap = new LinkedHashMap<>();
			for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
				Optional<DisplayInfo> display = holder.value().display();
				if (display.isEmpty()) {
					continue;
				}
				DisplayInfo info = display.get();
				String id = holder.id().toString();
				String tab = tabOf(holder.id(), parents, displays);
				if (!tabMap.containsKey(tab)) {
					DisplayInfo root = rootDisplay(holder.id(), parents, displays);
					tabMap.put(
						tab,
						new AdvancementTab(
							tab,
							root != null ? plain(root.title()) : tab,
							root != null ? itemId(root.icon()) : itemId(info.icon())
						)
					);
				}
				next.add(
					new AdvancementInfo(
						id,
						tab,
						plain(info.title()),
						plain(info.description()),
						itemId(info.icon()),
						frame(info.type()),
						info.hidden()
					)
				);
			}
			next.sort(
				Comparator.comparingInt((AdvancementInfo item) -> tabIndex(item.tab()))
					.thenComparing(item -> item.title().toLowerCase(Locale.ROOT))
					.thenComparing(AdvancementInfo::id)
			);
			List<AdvancementTab> nextTabs = new ArrayList<>(tabMap.values());
			nextTabs.sort(Comparator.comparingInt((AdvancementTab tab) -> tabIndex(tab.id())).thenComparing(AdvancementTab::id));
			Map<String, AdvancementInfo> index = new LinkedHashMap<>();
			for (AdvancementInfo item : next) {
				index.put(item.id(), item);
			}
			advancements = List.copyOf(next);
			tabs = List.copyOf(nextTabs);
			byId = Map.copyOf(index);
			ready = true;
		}
	}

	public void clear() {
		synchronized (lock) {
			advancements = List.of();
			tabs = List.of();
			byId = Map.of();
			ready = false;
		}
	}

	public boolean isReady() {
		return ready;
	}

	public int size() {
		return advancements.size();
	}

	public List<AdvancementInfo> all() {
		return advancements;
	}

	public List<AdvancementTab> tabs() {
		return tabs;
	}

	public AdvancementInfo find(String id) {
		return byId.get(id);
	}

	public Set<String> ids() {
		return byId.keySet();
	}

	private static int tabIndex(String tab) {
		int index = TAB_ORDER.indexOf(tab);
		return index >= 0 ? index : TAB_ORDER.size();
	}

	private static String tabOf(Identifier id, Map<String, Identifier> parents, Map<String, DisplayInfo> displays) {
		Identifier current = id;
		Identifier shown = displays.containsKey(id.toString()) ? id : null;
		for (int i = 0; i < 48; i++) {
			Identifier parent = parents.get(current.toString());
			if (parent == null) {
				break;
			}
			current = parent;
			if (displays.containsKey(current.toString())) {
				shown = current;
			}
		}
		if (shown == null) {
			shown = current;
		}
		String path = shown.getPath();
		int slash = path.indexOf('/');
		if (slash > 0) {
			return path.substring(0, slash);
		}
		return path.isEmpty() ? "other" : path;
	}

	private static DisplayInfo rootDisplay(Identifier id, Map<String, Identifier> parents, Map<String, DisplayInfo> displays) {
		Identifier current = id;
		DisplayInfo shown = displays.get(id.toString());
		for (int i = 0; i < 48; i++) {
			Identifier parent = parents.get(current.toString());
			if (parent == null) {
				break;
			}
			current = parent;
			DisplayInfo next = displays.get(current.toString());
			if (next != null) {
				shown = next;
			}
		}
		return shown;
	}

	private static String frame(AdvancementType type) {
		if (type == null) {
			return "task";
		}
		return type.name().toLowerCase(Locale.ROOT);
	}

	private static String itemId(ItemStackTemplate template) {
		if (template == null) {
			return "minecraft:barrier";
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(template.item().value());
		return id != null ? id.toString() : "minecraft:barrier";
	}

	private static String plain(net.minecraft.network.chat.Component component) {
		if (component == null) {
			return "";
		}
		return component.getString();
	}
}
