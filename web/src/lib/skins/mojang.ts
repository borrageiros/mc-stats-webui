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
const profileCache = new Map<string, Promise<PlayerTextures>>();
const imageCache = new Map<string, Promise<string>>();

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
	const cached = profileCache.get(id);
	if (cached) {
		return cached;
	}
	const pending = loadTextures(id);
	profileCache.set(id, pending);
	pending.catch(() => {
		profileCache.delete(id);
	});
	return pending;
}

async function cachedImageUrl(url: string): Promise<string> {
	const cached = imageCache.get(url);
	if (cached) {
		return cached;
	}
	const pending = (async () => {
		const response = await fetch(url);
		if (!response.ok) {
			throw new Error(`${response.status} ${response.statusText}`);
		}
		const blob = await response.blob();
		return URL.createObjectURL(blob);
	})();
	imageCache.set(url, pending);
	pending.catch(() => {
		imageCache.delete(url);
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
	const skinProxy = viaMojangProxy(skin.url);
	const capeProxy = payload.textures?.CAPE?.url ? viaMojangProxy(payload.textures.CAPE.url) : null;
	const [skinUrl, capeUrl] = await Promise.all([
		cachedImageUrl(skinProxy),
		capeProxy ? cachedImageUrl(capeProxy) : Promise.resolve(null)
	]);
	return {
		name: profile.name,
		uuid,
		slim: skin.metadata?.model === 'slim',
		skinUrl,
		capeUrl
	};
}
