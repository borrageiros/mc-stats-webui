<script lang="ts">
	import HeaderSelect from '$lib/components/HeaderSelect.svelte';
	import type { IconName } from '$lib/components/iconNames';
	import { t, type MessageKey } from '$lib/i18n/i18n.svelte';
	import { setTheme, theme, themes, type Theme } from '$lib/theme/theme.svelte';

	const themeKeys: Record<Theme, MessageKey> = {
		system: 'theme.system',
		light: 'theme.light',
		dark: 'theme.dark'
	};

	const themeIcons: Record<Theme, IconName> = {
		system: 'monitor',
		light: 'sun',
		dark: 'moon'
	};

	const options = $derived(
		themes.map((option) => ({
			id: option,
			label: t(themeKeys[option]),
			icon: themeIcons[option]
		}))
	);
</script>

<HeaderSelect
	id="theme"
	label={t('theme.label')}
	value={theme.preference}
	{options}
	onselect={(next) => setTheme(next as Theme)}
/>
