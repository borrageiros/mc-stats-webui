package webui.stats.mc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webui.stats.mc.web.StatsHttpServer;

import java.io.IOException;

public class McStatsWebui implements ModInitializer {
	public static final String MOD_ID = "mc-stats-webui";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				StatsHttpServer.getInstance().start(server);
			} catch (IOException e) {
				LOGGER.error("Could not bind the stats web server", e);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> StatsHttpServer.getInstance().stop());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
