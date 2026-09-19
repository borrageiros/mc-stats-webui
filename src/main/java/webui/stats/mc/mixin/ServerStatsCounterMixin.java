package webui.stats.mc.mixin;

import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.stats.Stat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import webui.stats.mc.stats.StatsCache;

import java.nio.file.Path;
import java.util.UUID;

@Mixin(ServerStatsCounter.class)
public class ServerStatsCounterMixin {
	@Shadow
	@Final
	private Path file;

	@Inject(method = "save", at = @At("RETURN"))
	private void mcStatsWebui$afterSave(CallbackInfo ci) {
		UUID uuid = uuidFromFile(file);
		if (uuid != null) {
			StatsCache.get().onStatsSaved(uuid);
		}
	}

	@Inject(method = "setValue", at = @At("RETURN"))
	private void mcStatsWebui$afterSetValue(Player player, Stat<?> stat, int value, CallbackInfo ci) {
		if (player != null) {
			StatsCache.get().markDirty(player.getUUID());
		}
	}

	private static UUID uuidFromFile(Path file) {
		if (file == null) {
			return null;
		}
		String name = file.getFileName().toString();
		if (!name.endsWith(".json") || name.length() <= 5) {
			return null;
		}
		try {
			return UUID.fromString(name.substring(0, name.length() - 5));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
