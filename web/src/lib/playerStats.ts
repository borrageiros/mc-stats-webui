export const gradeOrder = [
	'combat',
	'milestones',
	'mining',
	'explore',
	'produce',
	'survive',
	'flex'
] as const;

export const gradeItems: Record<(typeof gradeOrder)[number], string> = {
	combat: 'minecraft:iron_sword',
	milestones: 'minecraft:nether_star',
	mining: 'minecraft:diamond_pickaxe',
	explore: 'minecraft:compass',
	produce: 'minecraft:wheat',
	survive: 'minecraft:clock',
	flex: 'minecraft:totem_of_undying'
};

export type StatEntry = {
	id: string;
	icon: string;
	lowerWins?: boolean;
	compare?: boolean;
};

export type StatGroup = {
	category: string;
	icon: string;
	stats: StatEntry[];
};

export const statGroups: StatGroup[] = [
	{
		category: 'combat',
		icon: 'minecraft:iron_sword',
		stats: [
			{ id: 'hostile-kills', icon: 'minecraft:iron_sword' },
			{ id: 'farm-kills', icon: 'minecraft:cooked_chicken' },
			{ id: 'zombies', icon: 'minecraft:zombie_head' },
			{ id: 'skeletons', icon: 'minecraft:skeleton_skull' },
			{ id: 'creepers', icon: 'minecraft:creeper_head' },
			{ id: 'endermen', icon: 'minecraft:ender_pearl' },
			{ id: 'blazes', icon: 'minecraft:blaze_rod' },
			{ id: 'wither-skeletons', icon: 'minecraft:wither_skeleton_skull' },
			{ id: 'ender-dragon', icon: 'minecraft:dragon_egg' },
			{ id: 'wither', icon: 'minecraft:nether_star' },
			{ id: 'warden', icon: 'minecraft:echo_shard' },
			{ id: 'elder-guardian', icon: 'minecraft:elder_guardian_spawn_egg' },
			{ id: 'bosses', icon: 'minecraft:dragon_head' },
			{ id: 'raids-won', icon: 'minecraft:crossbow' },
			{ id: 'player-kills', icon: 'minecraft:player_head' },
			{ id: 'damage-dealt', icon: 'minecraft:diamond_sword' },
			{ id: 'kills-per-hour', icon: 'minecraft:clock' }
		]
	},
	{
		category: 'mining',
		icon: 'minecraft:diamond_pickaxe',
		stats: [
			{ id: 'blocks-mined', icon: 'minecraft:diamond_pickaxe' },
			{ id: 'ores', icon: 'minecraft:iron_ore' },
			{ id: 'diamonds-mined', icon: 'minecraft:diamond' },
			{ id: 'iron-mined', icon: 'minecraft:iron_ingot' },
			{ id: 'ancient-debris', icon: 'minecraft:ancient_debris' },
			{ id: 'logs-mined', icon: 'minecraft:oak_log' },
			{ id: 'sculk-mined', icon: 'minecraft:sculk' },
			{ id: 'tools-broken', icon: 'minecraft:netherite_scrap' }
		]
	},
	{
		category: 'exploration',
		icon: 'minecraft:compass',
		stats: [
			{ id: 'distance-total', icon: 'minecraft:compass' },
			{ id: 'distance-walk-sprint', icon: 'minecraft:leather_boots' },
			{ id: 'distance-walk', icon: 'minecraft:leather_boots' },
			{ id: 'distance-sprint', icon: 'minecraft:sugar' },
			{ id: 'distance-swim', icon: 'minecraft:heart_of_the_sea' },
			{ id: 'distance-boat', icon: 'minecraft:oak_boat' },
			{ id: 'distance-elytra', icon: 'minecraft:elytra' },
			{ id: 'distance-horse', icon: 'minecraft:saddle' },
			{ id: 'distance-happy-ghast', icon: 'minecraft:happy_ghast_spawn_egg' },
			{ id: 'distance-nautilus', icon: 'minecraft:nautilus_shell' },
			{ id: 'distance-minecart', icon: 'minecraft:minecart' },
			{ id: 'distance-strider', icon: 'minecraft:warped_fungus_on_a_stick' },
			{ id: 'chests-opened', icon: 'minecraft:chest' }
		]
	},
	{
		category: 'life',
		icon: 'minecraft:clock',
		stats: [
			{ id: 'play-time', icon: 'minecraft:clock', compare: false },
			{ id: 'animals-bred', icon: 'minecraft:wheat' },
			{ id: 'fish-caught', icon: 'minecraft:fishing_rod' },
			{ id: 'villager-trades', icon: 'minecraft:emerald' },
			{ id: 'items-enchanted', icon: 'minecraft:enchanted_book' },
			{ id: 'sleeps', icon: 'minecraft:red_bed' },
			{ id: 'jumps', icon: 'minecraft:rabbit_foot' }
		]
	},
	{
		category: 'deaths',
		icon: 'minecraft:bone',
		stats: [
			{ id: 'deaths', icon: 'minecraft:bone', lowerWins: true },
			{ id: 'killed-by-zombie', icon: 'minecraft:zombie_head', lowerWins: true },
			{ id: 'killed-by-creeper', icon: 'minecraft:creeper_head', lowerWins: true },
			{ id: 'killed-by-skeleton', icon: 'minecraft:skeleton_skull', lowerWins: true },
			{ id: 'killed-by-warden', icon: 'minecraft:echo_shard', lowerWins: true }
		]
	}
];

export const statItems: Record<string, string> = Object.fromEntries(
	statGroups.flatMap((group) => group.stats.map((stat) => [stat.id, stat.icon]))
);

export function compareStats(group: StatGroup): StatEntry[] {
	return group.stats.filter((stat) => stat.compare !== false);
}
