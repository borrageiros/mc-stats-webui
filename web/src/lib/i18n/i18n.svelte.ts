import { en } from './messages/en';
import { es, type MessageKey } from './messages/es';
import { fr } from './messages/fr';
import { STORAGE_KEY, isLocale, type Locale } from './locales';
import { formatWindowTitle } from '$lib/branding.svelte';

const dictionaries: Record<Locale, Record<MessageKey, string>> = { es, en, fr };

function browserLocale(): Locale {
	if (typeof navigator === 'undefined') {
		return 'es';
	}
	const language = navigator.language?.slice(0, 2).toLowerCase();
	if (isLocale(language)) {
		return language;
	}
	return 'es';
}

function readStored(): Locale | null {
	if (typeof localStorage === 'undefined') {
		return null;
	}
	const stored = localStorage.getItem(STORAGE_KEY);
	return isLocale(stored) ? stored : null;
}

function persist(next: Locale) {
	if (typeof localStorage !== 'undefined') {
		localStorage.setItem(STORAGE_KEY, next);
	}
	if (typeof document !== 'undefined') {
		document.documentElement.lang = next;
	}
}

function initialLocale(): Locale {
	const stored = readStored();
	if (stored) {
		return stored;
	}
	const fromBrowser = browserLocale();
	persist(fromBrowser);
	return fromBrowser;
}

export const i18n = $state({
	locale: initialLocale()
});

if (typeof document !== 'undefined') {
	document.documentElement.lang = i18n.locale;
}

export function setLocale(next: Locale) {
	i18n.locale = next;
	persist(next);
}

export function t(key: MessageKey, vars?: Record<string, string | number>): string {
	let text = dictionaries[i18n.locale][key] ?? dictionaries.es[key] ?? key;
	if (!vars) {
		return text;
	}
	for (const [name, value] of Object.entries(vars)) {
		text = text.replaceAll(`{${name}}`, String(value));
	}
	return text;
}

export function tDynamic(
	prefix:
		| 'leaderboard'
		| 'leaderboard.hint'
		| 'category'
		| 'unit'
		| 'unit.hint'
		| 'player.grade'
		| 'player.group'
		| 'player.custom',
	id: string,
	fallback = id
): string {
	const key = `${prefix}.${id}`;
	if (key in dictionaries.es) {
		return t(key as MessageKey);
	}
	return fallback;
}

export function boardHint(id: string, unit?: string): string {
	return tDynamic('leaderboard.hint', id, '') || (unit ? tDynamic('unit.hint', unit, '') : '');
}

export function pageTitle(page: string): string {
	return formatWindowTitle(page);
}

export { locales, type Locale } from './locales';
export type { MessageKey };
