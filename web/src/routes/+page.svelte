<script lang="ts">
	import { getLeaderboard } from '$lib/api';
	import HomePodium from '$lib/components/HomePodium.svelte';
	import Loader from '$lib/components/Loader.svelte';
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import RankingScore from '$lib/components/RankingScore.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import Tabs from '$lib/components/Tabs.svelte';
	import Tooltip from '$lib/components/Tooltip.svelte';
	import { branding } from '$lib/branding.svelte';
	import { boardHint, i18n, pageTitle, t, tDynamic } from '$lib/i18n/i18n.svelte';
	import type { LeaderboardResponse } from '$lib/types';

	const rankingIds = ['champion', 'dedicated', 'efficient'] as const;
	type RankingId = (typeof rankingIds)[number];

	let selected = $state<RankingId>('champion');
	let rankings = $state<Partial<Record<RankingId, LeaderboardResponse | null>>>({});
	let failed = $state(false);
	let loaded = $state(false);
	let query = $state('');

	const ranking = $derived(rankings[selected] ?? null);
	const tabs = $derived(
		rankingIds.map((id) => ({
			id,
			label: tDynamic('leaderboard', id)
		}))
	);
	const hint = $derived.by(() => {
		void i18n.locale;
		return boardHint(selected);
	});
	const shown = $derived.by(() => {
		const entries = ranking?.entries ?? [];
		const needle = query.trim().toLowerCase();
		if (!needle) {
			return entries;
		}
		return entries.filter((entry) => entry.name.toLowerCase().includes(needle));
	});

	async function load() {
		try {
			failed = false;
			loaded = false;
			const loadedRankings: Partial<Record<RankingId, LeaderboardResponse | null>> = {};
			await Promise.all(
				rankingIds.map(async (id) => {
					try {
						loadedRankings[id] = await getLeaderboard(id);
					} catch {
						loadedRankings[id] = null;
					}
				})
			);
			rankings = loadedRankings;
		} catch {
			failed = true;
		} finally {
			loaded = true;
		}
	}

	$effect(() => {
		load();
	});
</script>

<svelte:head>
	<title>{pageTitle(t('nav.home'))}</title>
</svelte:head>

{#if failed}
	<p class="empty">{t('home.apiError')}</p>
{:else if !loaded}
	<Loader />
{:else}
	<section class="ranking">
		{#if ranking && ranking.entries.length > 0}
			<HomePodium entries={ranking.entries} coin={selected === 'champion'} hint={hint} />
		{/if}
		<Tabs {tabs} bind:selected label={branding.title} />
		{#if ranking && ranking.entries.length > 0}
			<div class="filters">
				<SearchField bind:value={query} placeholder={t('home.search')} />
			</div>
			{#if shown.length > 0}
				<ol>
					{#each shown as entry}
						<li>
							<a class="slot" href="/players/{encodeURIComponent(entry.name)}">
								<span class="rank">{entry.rank}</span>
								<PlayerSkin uuid={entry.uuid} variant="head" scale={5} />
								<span class="player">{entry.name}</span>
								<Tooltip text={hint} align="end">
									{#if selected === 'champion'}
										<RankingScore value={entry.display} />
									{:else}
										<span class="value">{entry.display}</span>
									{/if}
								</Tooltip>
							</a>
						</li>
					{/each}
				</ol>
			{:else}
				<p class="empty">{t('player.noMatches')}</p>
			{/if}
		{:else}
			<p class="empty">{t('home.rankingEmpty')}</p>
		{/if}
	</section>
{/if}

<style>
	.empty {
		color: var(--color-muted);
	}

	.ranking {
		margin-top: 0;
	}

	.filters {
		margin: 0 0 0.85rem;
	}

	ol {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
	}

	.ranking ol a {
		display: grid;
		grid-template-columns: 2.5rem 2.5rem 1fr auto;
		gap: 0.8rem;
		padding: 0.55rem 0.75rem;
		align-items: center;
		overflow: visible;
	}

	.rank {
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
	}

	.player {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.value {
		margin: 0;
		color: var(--color-accent);
		font-variant-numeric: tabular-nums;
	}

</style>
