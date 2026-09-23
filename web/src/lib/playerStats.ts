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
