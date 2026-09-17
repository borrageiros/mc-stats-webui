import { t, tDynamic } from '$lib/i18n/i18n.svelte';
import type { MessageKey } from '$lib/i18n/messages/es';
import type { VanillaStat } from '$lib/types';

export const vanillaGroups = [
	'custom',
	'killed',
	'killed_by',
	'mined',
	'crafted',
	'used',
	'broken',
	'picked_up',
	'dropped'
] as const;

export type VanillaGroup = (typeof vanillaGroups)[number];

export const hideZerosByDefault: Record<VanillaGroup, boolean> = {
	custom: true,
	killed: true,
	killed_by: true,
	mined: true,
	crafted: true,
	used: true,
	broken: true,
	picked_up: true,
	dropped: true
};

const customIcons: Record<string, string> = {
	'minecraft:animals_bred': 'minecraft:wheat',
	'minecraft:aviate_one_cm': 'minecraft:elytra',
	'minecraft:bell_ring': 'minecraft:bell',
	'minecraft:boat_one_cm': 'minecraft:oak_boat',
	'minecraft:clean_armor': 'minecraft:leather_chestplate',
	'minecraft:clean_banner': 'minecraft:white_banner',
	'minecraft:clean_shulker_box': 'minecraft:shulker_box',
	'minecraft:climb_one_cm': 'minecraft:ladder',
	'minecraft:crouch_one_cm': 'minecraft:leather_boots',
	'minecraft:damage_absorbed': 'minecraft:golden_apple',
	'minecraft:damage_blocked_by_shield': 'minecraft:shield',
	'minecraft:damage_dealt': 'minecraft:diamond_sword',
	'minecraft:damage_dealt_absorbed': 'minecraft:diamond_sword',
	'minecraft:damage_dealt_resisted': 'minecraft:netherite_sword',
	'minecraft:damage_resisted': 'minecraft:iron_chestplate',
	'minecraft:damage_taken': 'minecraft:wooden_sword',
	'minecraft:deaths': 'minecraft:bone',
	'minecraft:drop': 'minecraft:dropper',
	'minecraft:eat_cake_slice': 'minecraft:cake',
	'minecraft:enchant_item': 'minecraft:enchanted_book',
	'minecraft:fall_one_cm': 'minecraft:feather',
	'minecraft:fill_cauldron': 'minecraft:water_bucket',
	'minecraft:fish_caught': 'minecraft:fishing_rod',
	'minecraft:fly_one_cm': 'minecraft:firework_rocket',
	'minecraft:happy_ghast_one_cm': 'minecraft:happy_ghast_spawn_egg',
	'minecraft:horse_one_cm': 'minecraft:saddle',
	'minecraft:inspect_dispenser': 'minecraft:dispenser',
	'minecraft:inspect_dropper': 'minecraft:dropper',
	'minecraft:inspect_hopper': 'minecraft:hopper',
	'minecraft:interact_with_anvil': 'minecraft:anvil',
	'minecraft:interact_with_beacon': 'minecraft:beacon',
	'minecraft:interact_with_blast_furnace': 'minecraft:blast_furnace',
	'minecraft:interact_with_brewingstand': 'minecraft:brewing_stand',
	'minecraft:interact_with_campfire': 'minecraft:campfire',
	'minecraft:interact_with_cartography_table': 'minecraft:cartography_table',
	'minecraft:interact_with_crafting_table': 'minecraft:crafting_table',
	'minecraft:interact_with_furnace': 'minecraft:furnace',
	'minecraft:interact_with_grindstone': 'minecraft:grindstone',
	'minecraft:interact_with_lectern': 'minecraft:lectern',
	'minecraft:interact_with_loom': 'minecraft:loom',
	'minecraft:interact_with_smithing_table': 'minecraft:smithing_table',
	'minecraft:interact_with_smoker': 'minecraft:smoker',
	'minecraft:interact_with_stonecutter': 'minecraft:stonecutter',
	'minecraft:jump': 'minecraft:rabbit_foot',
	'minecraft:leave_game': 'minecraft:oak_door',
	'minecraft:minecart_one_cm': 'minecraft:minecart',
	'minecraft:mob_kills': 'minecraft:iron_sword',
	'minecraft:nautilus_one_cm': 'minecraft:nautilus_shell',
	'minecraft:open_barrel': 'minecraft:barrel',
	'minecraft:open_chest': 'minecraft:chest',
	'minecraft:open_enderchest': 'minecraft:ender_chest',
	'minecraft:open_shulker_box': 'minecraft:shulker_box',
	'minecraft:pig_one_cm': 'minecraft:carrot_on_a_stick',
	'minecraft:play_noteblock': 'minecraft:note_block',
	'minecraft:play_record': 'minecraft:jukebox',
	'minecraft:play_time': 'minecraft:clock',
	'minecraft:player_kills': 'minecraft:player_head',
	'minecraft:pot_flower': 'minecraft:flower_pot',
	'minecraft:raid_trigger': 'minecraft:ominous_bottle',
	'minecraft:raid_win': 'minecraft:crossbow',
	'minecraft:sleep_in_bed': 'minecraft:red_bed',
	'minecraft:sneak_time': 'minecraft:leather_boots',
	'minecraft:sprint_one_cm': 'minecraft:sugar',
	'minecraft:strider_one_cm': 'minecraft:warped_fungus_on_a_stick',
	'minecraft:swim_one_cm': 'minecraft:heart_of_the_sea',
	'minecraft:talked_to_villager': 'minecraft:emerald',
	'minecraft:target_hit': 'minecraft:target',
	'minecraft:time_since_death': 'minecraft:totem_of_undying',
	'minecraft:time_since_rest': 'minecraft:phantom_membrane',
	'minecraft:total_world_time': 'minecraft:clock',
	'minecraft:traded_with_villager': 'minecraft:emerald',
	'minecraft:trigger_trapped_chest': 'minecraft:trapped_chest',
	'minecraft:tune_noteblock': 'minecraft:note_block',
	'minecraft:use_cauldron': 'minecraft:cauldron',
	'minecraft:walk_on_water_one_cm': 'minecraft:ice',
	'minecraft:walk_one_cm': 'minecraft:leather_boots',
	'minecraft:walk_under_water_one_cm': 'minecraft:turtle_helmet'
};

const entityIcons: Record<string, string> = {
	'minecraft:area_effect_cloud': 'minecraft:lingering_potion',
	'minecraft:armor_stand': 'minecraft:armor_stand',
	'minecraft:arrow': 'minecraft:arrow',
	'minecraft:block_display': 'minecraft:item_frame',
	'minecraft:breeze_wind_charge': 'minecraft:wind_charge',
	'minecraft:chest_minecart': 'minecraft:chest_minecart',
	'minecraft:command_block_minecart': 'minecraft:command_block_minecart',
	'minecraft:dragon_fireball': 'minecraft:fire_charge',
	'minecraft:egg': 'minecraft:egg',
	'minecraft:end_crystal': 'minecraft:end_crystal',
	'minecraft:ender_pearl': 'minecraft:ender_pearl',
	'minecraft:evoker_fangs': 'minecraft:totem_of_undying',
	'minecraft:experience_bottle': 'minecraft:experience_bottle',
	'minecraft:experience_orb': 'minecraft:experience_bottle',
	'minecraft:eye_of_ender': 'minecraft:ender_eye',
	'minecraft:falling_block': 'minecraft:sand',
	'minecraft:fireball': 'minecraft:fire_charge',
	'minecraft:firework_rocket': 'minecraft:firework_rocket',
	'minecraft:fishing_bobber': 'minecraft:fishing_rod',
	'minecraft:furnace_minecart': 'minecraft:furnace_minecart',
	'minecraft:glow_item_frame': 'minecraft:glow_item_frame',
	'minecraft:hopper_minecart': 'minecraft:hopper_minecart',
	'minecraft:interaction': 'minecraft:barrier',
	'minecraft:item': 'minecraft:bundle',
	'minecraft:item_display': 'minecraft:item_frame',
	'minecraft:item_frame': 'minecraft:item_frame',
	'minecraft:leash_knot': 'minecraft:lead',
	'minecraft:lightning_bolt': 'minecraft:lightning_rod',
	'minecraft:llama_spit': 'minecraft:llama_spawn_egg',
	'minecraft:marker': 'minecraft:barrier',
	'minecraft:minecart': 'minecraft:minecart',
	'minecraft:ominous_item_spawner': 'minecraft:ominous_bottle',
	'minecraft:painting': 'minecraft:painting',
	'minecraft:player': 'minecraft:player_head',
	'minecraft:shulker_bullet': 'minecraft:shulker_shell',
	'minecraft:small_fireball': 'minecraft:fire_charge',
	'minecraft:snowball': 'minecraft:snowball',
	'minecraft:spectral_arrow': 'minecraft:spectral_arrow',
	'minecraft:spawner_minecart': 'minecraft:spawner',
	'minecraft:text_display': 'minecraft:oak_sign',
	'minecraft:tnt': 'minecraft:tnt',
	'minecraft:tnt_minecart': 'minecraft:tnt_minecart',
	'minecraft:trident': 'minecraft:trident',
	'minecraft:wind_charge': 'minecraft:wind_charge',
	'minecraft:wither_skull': 'minecraft:wither_skeleton_spawn_egg'
};

export function shortId(id: string): string {
	return id.replace(/^minecraft:/, '');
}

export function humanizeId(id: string): string {
	return shortId(id)
		.split('_')
		.filter((part) => part.length > 0)
		.map((part) => part.charAt(0).toUpperCase() + part.slice(1))
		.join(' ');
}

export function vanillaLabel(group: VanillaGroup, id: string): string {
	if (group === 'custom') {
		return tDynamic('player.custom', shortId(id), humanizeId(id));
	}
	return humanizeId(id);
}

export const vanillaGroupIcons: Record<VanillaGroup, string> = {
	custom: 'minecraft:clock',
	killed: 'minecraft:iron_sword',
	killed_by: 'minecraft:bone',
	mined: 'minecraft:diamond_pickaxe',
	crafted: 'minecraft:crafting_table',
	used: 'minecraft:flint_and_steel',
	broken: 'minecraft:netherite_scrap',
	picked_up: 'minecraft:hopper',
	dropped: 'minecraft:dropper'
};

export function vanillaBoardHref(group: VanillaGroup, id: string): string {
	return `/leaderboards/${group}/${encodeURIComponent(id)}`;
}

export function parseVanillaBoardId(id: string): { group: VanillaGroup; id: string } | null {
	const slash = id.indexOf('/');
	if (slash <= 0) {
		return null;
	}
	const group = id.slice(0, slash);
	if (!vanillaGroups.includes(group as VanillaGroup)) {
		return null;
	}
	return { group: group as VanillaGroup, id: id.slice(slash + 1) };
}

export function matchesVanillaQuery(group: VanillaGroup, id: string, query: string): boolean {
	const needle = query.trim().toLowerCase();
	if (!needle) {
		return true;
	}
	const label = vanillaLabel(group, id).toLowerCase();
	return label.includes(needle) || id.toLowerCase().includes(needle) || shortId(id).toLowerCase().includes(needle);
}

export function vanillaHint(group: VanillaGroup, id: string): string {
	return t(`leaderboard.hint.group.${group}` as MessageKey, { name: vanillaLabel(group, id) });
}

export function vanillaIcon(group: VanillaGroup, id: string): string {
	if (group === 'custom') {
		return customIcons[id] ?? id;
	}
	if (group === 'killed' || group === 'killed_by') {
		return entityIcons[id] ?? id;
	}
	return id;
}

export function visibleVanilla(
	group: VanillaGroup,
	rows: VanillaStat[],
	query: string,
	hideZeros: boolean
): VanillaStat[] {
	const needle = query.trim().toLowerCase();
	const filtered = rows.filter((row) => {
		if (!needle && hideZeros && row.value <= 0) {
			return false;
		}
		if (!needle) {
			return true;
		}
		const label = vanillaLabel(group, row.id).toLowerCase();
		return label.includes(needle) || row.id.toLowerCase().includes(needle);
	});
	return filtered.slice().sort((a, b) => {
		if (a.value !== b.value) {
			return b.value - a.value;
		}
		return a.id.localeCompare(b.id);
	});
}
