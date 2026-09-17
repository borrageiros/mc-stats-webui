<script lang="ts">
	import HeaderSelect from '$lib/components/HeaderSelect.svelte';
	import type { IconName } from '$lib/components/iconNames';
	import { i18n, locales, setLocale, t, type Locale, type MessageKey } from '$lib/i18n/i18n.svelte';

	const localeKeys: Record<Locale, MessageKey> = {
		es: 'language.es',
		en: 'language.en',
		fr: 'language.fr'
	};

	const localeIcons: Record<Locale, IconName> = {
		es: 'es',
		en: 'en',
		fr: 'fr'
	};

	const options = $derived(
		locales.map((locale) => ({
			id: locale,
			label: t(localeKeys[locale]),
			icon: localeIcons[locale]
		}))
	);
</script>

<HeaderSelect
	id="language"
	label={t('language.label')}
	value={i18n.locale}
	{options}
	onselect={(next) => setLocale(next as Locale)}
/>
