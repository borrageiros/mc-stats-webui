<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import { page } from '$app/state';
	import Icon from '$lib/components/Icon.svelte';
	import { t } from '$lib/i18n/i18n.svelte';

	let fromPath = $state<string | null>(null);

	afterNavigate(({ from }) => {
		fromPath = from ? `${from.url.pathname}${from.url.search}` : null;
	});

	const fallbackPath = $derived(/^\/leaderboards\/[^/]+$/.test(page.url.pathname) ? '/leaderboards' : '/');
	const href = $derived(fromPath ?? fallbackPath);
	const label = $derived(t('nav.back'));

	function onBack(event: MouseEvent) {
		if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) {
			return;
		}
		if (fromPath) {
			event.preventDefault();
			history.back();
		}
	}
</script>

<a class="mc-btn back" href={href} onclick={onBack} aria-label={label}>
	<Icon name="back" />
	{label}
</a>

<style>
	.back {
		display: inline-flex;
		width: auto;
		margin: 0 0 1.1rem;
		min-height: 2.15rem;
		padding: 0.28rem 0.9rem;
	}
</style>
