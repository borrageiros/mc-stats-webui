export type PlayerTextures = {
	name: string;
	uuid: string;
	slim: boolean;
	skinUrl: string;
	capeUrl: string | null;
};

type MojangProfile = {
	id: string;
	name: string;
	properties?: { name: string; value: string }[];
};

type TexturePayload = {
	textures?: {
		SKIN?: {
			url: string;
			metadata?: { model?: string };
		};
		CAPE?: {
			url: string;
		};
	};
};

const SESSION_ORIGIN = 'https://sessionserver.mojang.com';
const cache = new Map<string, Promise<PlayerTextures>>();

export function formatUuid(uuid: string): string {
	const hex = uuid.replaceAll('-', '').toLowerCase();
	if (!/^[0-9a-f]{32}$/.test(hex)) {
		throw new Error('Invalid UUID');
	}
	return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function mojangProfileUrl(uuid: string): string {
	return `${SESSION_ORIGIN}/session/minecraft/profile/${formatUuid(uuid)}`;
}

export function viaMojangProxy(url: string): string {
	if (url.startsWith(SESSION_ORIGIN)) {
		return `/mojang${url.slice(SESSION_ORIGIN.length)}`;
	}
	const parsed = new URL(url);
	if (parsed.hostname === 'textures.minecraft.net') {
		return `/mojang/textures${parsed.pathname}`;
	}
	return url;
}

export function getPlayerTextures(uuid: string): Promise<PlayerTextures> {
	const id = formatUuid(uuid);
	const cached = cache.get(id);
	if (cached) {
		return cached;
	}
	const pending = loadTextures(id);
	cache.set(id, pending);
	pending.catch(() => {
		cache.delete(id);
	});
	return pending;
}

async function loadTextures(uuid: string): Promise<PlayerTextures> {
	const response = await fetch(viaMojangProxy(mojangProfileUrl(uuid)));
	if (!response.ok) {
		throw new Error(`${response.status} ${response.statusText}`);
	}
	const profile = (await response.json()) as MojangProfile;
	const encoded = profile.properties?.find((property) => property.name === 'textures')?.value;
	if (!encoded) {
		throw new Error('Missing textures');
	}
	const payload = JSON.parse(atob(encoded)) as TexturePayload;
	const skin = payload.textures?.SKIN;
	if (!skin?.url) {
		throw new Error('Missing skin');
	}
	return {
		name: profile.name,
		uuid,
		slim: skin.metadata?.model === 'slim',
		skinUrl: viaMojangProxy(skin.url),
		capeUrl: payload.textures?.CAPE?.url ? viaMojangProxy(payload.textures.CAPE.url) : null
	};
}
