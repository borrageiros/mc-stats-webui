<script lang="ts">
	import { page } from '$app/state';
	import BackNav from '$lib/components/BackNav.svelte';
	import Loader from '$lib/components/Loader.svelte';
	import McItem from '$lib/components/McItem.svelte';
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import RankingScore from '$lib/components/RankingScore.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import Tooltip from '$lib/components/Tooltip.svelte';
	import { getLeaderboard } from '$lib/api';
	import { boardHint, i18n, pageTitle, t, tDynamic } from '$lib/i18n/i18n.svelte';
	import { statItems } from '$lib/playerStats';
	import type { LeaderboardResponse } from '$lib/types';
	import { parseVanillaBoardId, vanillaHint, vanillaIcon, vanillaLabel } from '$lib/vanillaStats';

	const crownIcons: Record<string, string> = {
		champion: 'minecraft:nether_star',
		dedicated: 'minecraft:clock',
		efficient: 'minecraft:experience_bottle'
	};

	let board = $state<LeaderboardResponse | null>(null);
	let failed = $state(false);
	let loading = $state(true);
	let query = $state('');

	const boardId = $derived(
		Array.isArray(page.params.id) ? page.params.id.join('/') : (page.params.id ?? '')
	);
	const vanilla = $derived(parseVanillaBoardId(board?.id ?? boardId));
	const heading = $derived.by(() => {
		void i18n.locale;
		if (vanilla) {
			return vanillaLabel(vanilla.group, vanilla.id);
		}
		return board
			? tDynamic('leaderboard', board.id, board.title)
			: tDynamic('leaderboard', boardId, t('leaderboard.fallbackTitle'));
	});
	const icon = $derived.by(() => {
		if (vanilla) {
			return vanillaIcon(vanilla.group, vanilla.id);
		}
		const id = board?.id ?? boardId;
		return crownIcons[id] ?? statItems[id] ?? '';
	});
	const categoryLabel = $derived.by(() => {
		void i18n.locale;
		if (vanilla) {
			return tDynamic('player.group', vanilla.group);
		}
		return board ? tDynamic('category', board.category, board.category) : '';
	});
	const unitLabel = $derived(board?.unit ? tDynamic('unit', board.unit, board.unit) : '');
	const hint = $derived.by(() => {
		void i18n.locale;
		if (vanilla) {
			return vanillaHint(vanilla.group, vanilla.id);
		}
		return boardHint(board?.id ?? boardId, board?.unit);
	});
	const shown = $derived.by(() => {
		const entries = board?.entries ?? [];
		const needle = query.trim().toLowerCase();
		if (!needle) {
			return entries;
		}
		return entries.filter((entry) => entry.name.toLowerCase().includes(needle));
	});

	$effect(() => {
		const id = boardId;
		if (!id) {
			return;
		}
		board = null;
		failed = false;
		loading = true;
		query = '';
		getLeaderboard(id)
			.then((data) => {
				board = data;
				failed = false;
			})
			.catch(() => {
				board = null;
				failed = true;
			})
			.finally(() => {
				loading = false;
			});
	});
</script>

<svelte:head>
	<title>{pageTitle(heading)}</title>
</svelte:head>

<BackNav />
<h1>
	{#if icon}
		<McItem id={icon} />
	{/if}
	{heading}
</h1>
<p class="lead">
	{categoryLabel}{#if unitLabel}
		{' · '}
		<Tooltip text={hint} placement="bottom" focusable>{unitLabel}</Tooltip>
	{/if}
</p>

{#if failed}
	<p class="empty">{t('leaderboard.loadError')}</p>
{:else if loading}
	<Loader />
{:else if !board || board.entries.length === 0}
	<p class="empty">{t('leaderboard.empty')}</p>
{:else}
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
						<span class="name">{entry.name}</span>
						<Tooltip text={hint} align="end">
							{#if board.id === 'champion'}
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
{/if}

<style>
	h1 {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.55rem;
		margin: 0;
		text-align: center;
	}

	.lead,
	.empty {
		color: var(--color-muted);
		text-align: center;
	}

	.filters {
		margin: 1rem 0 0;
	}

	ol {
		list-style: none;
		padding: 0;
		margin: 1.25rem 0 0;
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
		overflow: visible;
	}

	a {
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

	.value {
		font-variant-numeric: tabular-nums;
		color: var(--color-accent);
	}
</style>
