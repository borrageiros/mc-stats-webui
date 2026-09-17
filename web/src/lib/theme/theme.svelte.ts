import { STORAGE_KEY, isTheme, type ResolvedTheme, type Theme } from './themes';

function readStored(): Theme | null {
	if (typeof localStorage === 'undefined') {
		return null;
	}
	const stored = localStorage.getItem(STORAGE_KEY);
	return isTheme(stored) ? stored : null;
}

function systemResolved(): ResolvedTheme {
	if (typeof window === 'undefined') {
		return 'dark';
	}
	return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
}

function resolve(preference: Theme): ResolvedTheme {
	return preference === 'system' ? systemResolved() : preference;
}

function persist(preference: Theme) {
	if (typeof localStorage !== 'undefined') {
		localStorage.setItem(STORAGE_KEY, preference);
	}
}

function applyDom(preference: Theme, resolved: ResolvedTheme) {
	if (typeof document === 'undefined') {
		return;
	}
	document.documentElement.dataset.theme = resolved;
	document.documentElement.dataset.themePreference = preference;
	document.documentElement.style.colorScheme = resolved;
}

function initialPreference(): Theme {
	const stored = readStored();
	if (stored) {
		return stored;
	}
	persist('system');
	return 'system';
}

const preference = initialPreference();
const resolved = resolve(preference);

export const theme = $state({
	preference,
	resolved
});

applyDom(theme.preference, theme.resolved);

export function setTheme(next: Theme) {
	theme.preference = next;
	theme.resolved = resolve(next);
	persist(next);
	applyDom(theme.preference, theme.resolved);
}

if (typeof window !== 'undefined') {
	window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', () => {
		if (theme.preference !== 'system') {
			return;
		}
		theme.resolved = systemResolved();
		applyDom(theme.preference, theme.resolved);
	});
}

export { themes, type Theme, type ResolvedTheme } from './themes';
