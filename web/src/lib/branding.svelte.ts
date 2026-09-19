import { getStatus } from '$lib/api';

export type SiteBranding = {
	title: string;
	slogan: string;
	tagline: string;
	windowTitle: string;
};

const defaults: SiteBranding = {
	title: 'Ranking',
	slogan: 'Server stats.',
	tagline: 'by borrageiros',
	windowTitle: '{page} - Server Ranking'
};

export const branding = $state<SiteBranding>({ ...defaults });

let loading: Promise<void> | null = null;

export function loadBranding(): Promise<void> {
	if (loading) {
		return loading;
	}
	loading = getStatus()
		.then((status) => {
			const next = status.branding;
			if (!next) {
				return;
			}
			branding.title = next.title?.trim() || defaults.title;
			branding.slogan = next.slogan?.trim() || defaults.slogan;
			branding.tagline = next.tagline?.trim() || defaults.tagline;
			branding.windowTitle = next.windowTitle?.trim() || defaults.windowTitle;
		})
		.catch(() => {
			Object.assign(branding, defaults);
		})
		.finally(() => {
			loading = null;
		});
	return loading;
}

export function formatWindowTitle(page: string): string {
	const template = branding.windowTitle || defaults.windowTitle;
	return template.replaceAll('{page}', page).replaceAll('<page>', page);
}
