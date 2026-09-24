<script lang="ts">
	import { advancementTabTitle, matchesAdvancementQuery } from '$lib/advancements';
	import { boardHint, i18n, pageTitle, t } from '$lib/i18n/i18n.svelte';
	import { getAdvancementMost, getAdvancements } from '$lib/api';
	import AdvancementTiles from '$lib/components/AdvancementTiles.svelte';
	import HomePodium from '$lib/components/HomePodium.svelte';
	import Loader from '$lib/components/Loader.svelte';
	import McItem from '$lib/components/McItem.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import { cappedResults, deferredValue } from '$lib/search.svelte';
	import type { AdvancementInfo, AdvancementTab, LeaderboardResponse } from '$lib/types';

	let catalog = $state<AdvancementInfo[]>([]);
	let tabs = $state<AdvancementTab[]>([]);
	let total = $state(0);
	let ranking = $state<LeaderboardResponse | null>(null);
	let failed = $state(false);
	let loaded = $state(false);
	let query = $state('');
	let openTabs = $state<Record<string, boolean>>({});
	const filterQuery = deferredValue(() => query);
	const searching = $derived(filterQuery.current.trim().length > 0);
	const hint = $derived(boardHint('advancements'));

	$effect(() => {
		failed = false;
		loaded = false;
		Promise.all([getAdvancements(), getAdvancementMost().catch(() => null)])
			.then(([data, most]) => {
				catalog = data.advancements;
				tabs = data.tabs;
				total = data.total;
				ranking = most;
				openTabs = Object.fromEntries(data.tabs.map((tab) => [tab.id, true]));
			})
			.catch(() => {
				failed = true;
			})
			.finally(() => {
				loaded = true;
			});
	});

	function isOpen(tab: string) {
		return searching || Boolean(openTabs[tab]);
	}

	function toggleTab(tab: string) {
		if (searching) {
			return;
		}
		openTabs = { ...openTabs, [tab]: !openTabs[tab] };
	}

	const sections = $derived.by(() => {
		void i18n.locale;
		const needle = filterQuery.current;
		return tabs
			.map((tab) => {
				const matched = catalog.filter(
					(item) =>
						item.tab === tab.id &&
						matchesAdvancementQuery(item.id, item.title, item.description, needle)
				);
				return { tab, matched, ids: cappedResults(matched, searching) };
			})
			.filter((section) => section.matched.length > 0);
	});
</script>

<svelte:head>
	<title>{pageTitle(t('nav.advancements'))}</title>
</svelte:head>

<h1>{t('advancements.heading')}</h1>
<p class="lead">{t('advancements.lead')}</p>

{#if failed}
	<p class="empty">{t('advancements.loadError')}</p>
{:else if !loaded}
	<Loader />
{:else}
	{#if ranking && ranking.entries.length > 0}
		<HomePodium entries={ranking.entries} hint={hint} />
		<div class="most">
			<a class="mc-btn" href="/advancements/most">{t('advancements.most')}</a>
		</div>
	{/if}

	<div class="filters">
		<SearchField bind:value={query} placeholder={t('advancements.search')} />
	</div>

	{#if sections.length > 0}
		{#each sections as section}
			<section class="pack">
				<div class="pack-head">
					<button
						class="fold"
						type="button"
						aria-expanded={isOpen(section.tab.id)}
						onclick={() => toggleTab(section.tab.id)}
					>
						<span class="chevron" class:open={isOpen(section.tab.id)}>▸</span>
						<span class="pack-title">
							<McItem id={section.tab.icon} compact />
							{advancementTabTitle(section.tab.id, section.tab.title)}
							<span class="count"
								>{t('player.shown', {
									shown: section.ids.length,
									total: catalog.filter((item) => item.tab === section.tab.id).length
								})}</span
							>
						</span>
					</button>
				</div>
				{#if isOpen(section.tab.id)}
					<AdvancementTiles items={section.ids} dim={(item) => Boolean(item.hidden)} showHolders />
				{/if}
			</section>
		{/each}
	{:else if total === 0}
		<p class="empty">{t('advancements.nobody')}</p>
	{:else}
		<p class="empty">{t('player.noMatches')}</p>
	{/if}
{/if}

<style>
	h1 {
		margin: 0 0 0.4rem;
		text-align: center;
	}

	.lead,
	.empty {
		color: var(--color-muted);
		text-align: center;
	}

	.most {
		display: flex;
		justify-content: center;
		margin: 0 0 1.15rem;
	}

	.most :global(.mc-btn) {
		width: min(20rem, 100%);
	}

	.filters {
		margin: 0 0 1.1rem;
	}

	.pack {
		margin-top: 0.55rem;
	}

	.pack-head {
		display: flex;
		flex-wrap: wrap;
		align-items: baseline;
		justify-content: space-between;
		gap: 0.6rem 1rem;
		margin: 0 0 0.7rem;
	}

	.fold {
		display: inline-flex;
		align-items: baseline;
		gap: 0.4rem;
		min-height: 0;
		padding: 0;
		color: var(--color-muted);
		font: inherit;
		text-align: left;
		text-shadow: inherit;
		background: none;
		border: none;
		box-shadow: none;
		cursor: pointer;
	}

	.fold:hover,
	.fold:focus-visible {
		color: var(--color-hover);
		background: none;
		border: none;
		box-shadow: none;
	}

	.chevron {
		display: inline-block;
		color: var(--color-gold);
		transform: rotate(0deg);
		transition: transform 0.12s linear;
	}

	.chevron.open {
		transform: rotate(90deg);
	}

	.pack-title {
		display: inline-flex;
		align-items: center;
		gap: 0.45rem;
		font-size: 1rem;
		color: var(--color-gold);
	}

	.count {
		margin-left: 0.45rem;
		font-size: 0.78rem;
		color: var(--color-muted);
	}
</style>
