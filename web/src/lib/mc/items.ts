import iconMap from './icons.json';

const icons = iconMap as Record<string, string>;

export function shortItemId(id: string): string {
	return id.replace(/^minecraft:/, '');
}

export function itemLetter(id: string): string {
	const name = shortItemId(id);
	return name.charAt(0).toUpperCase();
}

export function itemTextureUrls(id: string): string[] {
	const name = shortItemId(id);
	const urls: string[] = [];
	const mapped = icons[id];
	if (mapped) {
		urls.push(mapped);
	}
	const aliases = [
		name.replace('_wall_sign', '_sign').replace('_wall_hanging_sign', '_hanging_sign'),
		name.startsWith('wall_') ? name.slice(5) : '',
		name.endsWith('s') ? name.slice(0, -1) : ''
	];
	for (const alias of aliases) {
		if (!alias || alias === name) {
			continue;
		}
		const fromMap = icons[`minecraft:${alias}`];
		if (fromMap && !urls.includes(fromMap)) {
			urls.push(fromMap);
		}
	}
	for (const candidate of [
		`/mc/item/${name}.png`,
		`/mc/inv/${name}.png`,
		`/mc/iso/${name}.png`,
		`/mc/block/${name}.png`,
		`/mc/item/${name}_spawn_egg.png`,
		`/mc/block/${name}_front.png`,
		`/mc/block/${name}_top.png`,
		`/mc/block/${name}_side.png`,
		`/mc/block/${name}_0.png`,
		`/mc/block/${name}_still.png`
	]) {
		if (!urls.includes(candidate)) {
			urls.push(candidate);
		}
	}
	return urls;
}

export function itemTexture(id: string): string | undefined {
	return itemTextureUrls(id)[0];
}

export function resolveItemIcon(id: string): string | undefined {
	if (icons[id]) {
		return id;
	}
	const name = shortItemId(id);
	for (const candidate of [`minecraft:${name}_head`, `minecraft:${name}_skull`, `minecraft:${name}_spawn_egg`]) {
		if (icons[candidate]) {
			return candidate;
		}
	}
	return id;
}
