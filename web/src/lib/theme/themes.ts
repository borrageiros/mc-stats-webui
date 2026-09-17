export const themes = ['system', 'light', 'dark'] as const;

export type Theme = (typeof themes)[number];

export type ResolvedTheme = 'light' | 'dark';

export const STORAGE_KEY = 'mc-stats-theme';

export function isTheme(value: string | null | undefined): value is Theme {
	return value === 'system' || value === 'light' || value === 'dark';
}
