package webui.stats.mc.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import webui.stats.mc.stats.StatsCache;

import java.nio.file.Path;
import java.util.UUID;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
	@Shadow
	@Final
	private Path playerSavePath;

	@Shadow
	private ServerPlayer player;

	@Inject(method = "save", at = @At("RETURN"))
	private void mcStatsWebui$afterSave(CallbackInfo ci) {
		UUID uuid = player != null ? player.getUUID() : uuidFromFile(playerSavePath);
		if (uuid != null) {
			StatsCache.get().onStatsSaved(uuid);
		}
	}

	@Inject(method = "award", at = @At("RETURN"))
	private void mcStatsWebui$afterAward(
		AdvancementHolder holder,
		String criterion,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (Boolean.TRUE.equals(cir.getReturnValue()) && player != null) {
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
