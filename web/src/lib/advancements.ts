import advancementLang from '$lib/mc/advancementLang.json';
import { i18n, type Locale } from '$lib/i18n/i18n.svelte';

const dictionaries = advancementLang as Record<string, Record<string, string>>;

function langKey(id: string, kind: 'title' | 'description') {
	const colon = id.indexOf(':');
	if (colon <= 0) {
		return '';
	}
	const namespace = id.slice(0, colon);
	const path = id.slice(colon + 1);
	if (namespace !== 'minecraft' || !path) {
		return '';
	}
	return `advancements.${path.replaceAll('/', '.')}.${kind}`;
}

function lookup(key: string, locale: Locale) {
	if (!key) {
		return '';
	}
	return dictionaries[locale]?.[key] || dictionaries.en?.[key] || '';
}

export function advancementTitle(id: string, fallback = id) {
	return lookup(langKey(id, 'title'), i18n.locale) || fallback;
}

export function advancementDescription(id: string, fallback = '') {
	return lookup(langKey(id, 'description'), i18n.locale) || fallback;
}

export function advancementTabTitle(tab: string, fallback = tab) {
	return lookup(`advancements.${tab}.root.title`, i18n.locale) || fallback;
}

export function advancementHref(id: string) {
	return `/advancements/${id
		.replace(':', '/')
		.split('/')
		.map((part) => encodeURIComponent(part))
		.join('/')}`;
}

export function matchesAdvancementQuery(
	id: string,
	fallbackTitle: string,
	fallbackDescription: string,
	query: string
) {
	const needle = query.trim().toLowerCase();
	if (!needle) {
		return true;
	}
	const title = advancementTitle(id, fallbackTitle).toLowerCase();
	const description = advancementDescription(id, fallbackDescription).toLowerCase();
	return title.includes(needle) || description.includes(needle) || id.toLowerCase().includes(needle);
}
