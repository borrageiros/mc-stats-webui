<script lang="ts">
	import { goto } from '$app/navigation';
	import { getRoster } from '$lib/api';
	import Loader from '$lib/components/Loader.svelte';
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import RankingScore from '$lib/components/RankingScore.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import { pageTitle, t } from '$lib/i18n/i18n.svelte';
	import type { PlayerSummary } from '$lib/types';

	type SortKey = 'name' | 'hours' | 'rate' | 'score' | 'done';
	type SortDir = 'asc' | 'desc';

	let players = $state<PlayerSummary[]>([]);
	let failed = $state(false);
	let loaded = $state(false);
	let query = $state('');
	let sortKey = $state<SortKey>('hours');
	let sortDir = $state<SortDir>('desc');

	const ranked = $derived.by(() => {
		const dir = sortDir === 'desc' ? -1 : 1;
		const sorted = [...players].sort((left, right) => {
			const compare = comparePlayers(left, right, sortKey);
			if (compare !== 0) {
				return dir * compare;
			}
			return left.name.localeCompare(right.name, undefined, { sensitivity: 'base' });
		});
		return sorted.map((player, index) => ({ player, rank: index + 1 }));
	});
	const shown = $derived.by(() => {
		const needle = query.trim().toLowerCase();
		if (!needle) {
			return ranked;
		}
		return ranked.filter((row) => row.player.name.toLowerCase().includes(needle));
	});
	const countLabel = $derived.by(() => {
		if (query.trim()) {
			return t('player.shown', { shown: shown.length, total: players.length });
		}
		return t('players.count', { count: players.length });
	});

	function comparePlayers(left: PlayerSummary, right: PlayerSummary, key: SortKey) {
		if (key === 'name') {
			return left.name.localeCompare(right.name, undefined, { sensitivity: 'base' });
		}
		return numericValue(left, key) - numericValue(right, key);
	}

	function numericValue(player: PlayerSummary, key: Exclude<SortKey, 'name'>) {
		switch (key) {
			case 'hours':
				return player.playHours ?? 0;
			case 'rate':
				return player.scorePerHour ?? 0;
			case 'score':
				return player.championScore ?? 0;
			case 'done':
				return player.advancements?.done ?? 0;
		}
	}

	function toggleSort(key: SortKey) {
		if (sortKey === key) {
			sortDir = sortDir === 'desc' ? 'asc' : 'desc';
			return;
		}
		sortKey = key;
		sortDir = key === 'name' ? 'asc' : 'desc';
	}

	function sortMark(key: SortKey) {
		if (sortKey !== key) {
			return '';
		}
		return sortDir === 'desc' ? '▾' : '▴';
	}

	function ariaSort(key: SortKey): 'ascending' | 'descending' | 'none' {
		if (sortKey !== key) {
			return 'none';
		}
		return sortDir === 'asc' ? 'ascending' : 'descending';
	}

	function hrefFor(name: string) {
		return `/players/${encodeURIComponent(name)}`;
	}

	function openRow(event: MouseEvent, name: string) {
		if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) {
			return;
		}
		const target = event.target as HTMLElement | null;
		if (target?.closest('a, button')) {
			return;
		}
		goto(hrefFor(name));
	}

	function onRowKey(event: KeyboardEvent, name: string) {
		if (event.key === 'Enter' || event.key === ' ') {
			event.preventDefault();
			goto(hrefFor(name));
		}
	}

	$effect(() => {
		failed = false;
		loaded = false;
		getRoster()
			.then((list) => {
				players = list;
			})
			.catch(() => {
				failed = true;
			})
			.finally(() => {
				loaded = true;
			});
	});
</script>

<svelte:head>
	<title>{pageTitle(t('nav.players'))}</title>
</svelte:head>

<h1>{t('players.heading')}</h1>
<p class="lead">{t('players.lead')}</p>

{#if failed}
	<p class="empty">{t('players.loadError')}</p>
{:else if !loaded}
	<Loader />
{:else if players.length === 0}
	<p class="empty">{t('players.empty')}</p>
{:else}
	<div class="filters">
		<SearchField bind:value={query} placeholder={t('home.search')} />
	</div>
	<p class="meta">{countLabel}</p>
	{#if shown.length === 0}
		<p class="empty">{t('player.noMatches')}</p>
	{:else}
		<div class="wrap">
			<table>
				<thead>
					<tr>
						<th class="rank">#</th>
						<th class="player" aria-sort={ariaSort('name')}>
							<button type="button" class="sort" class:on={sortKey === 'name'} onclick={() => toggleSort('name')}>
								{t('players.col.player')}
								<span>{sortMark('name')}</span>
							</button>
						</th>
						<th class="num hours" aria-sort={ariaSort('hours')}>
							<button type="button" class="sort" class:on={sortKey === 'hours'} onclick={() => toggleSort('hours')}>
								{t('players.col.hours')}
								<span>{sortMark('hours')}</span>
							</button>
						</th>
						<th class="num rate" aria-sort={ariaSort('rate')}>
							<button type="button" class="sort" class:on={sortKey === 'rate'} onclick={() => toggleSort('rate')}>
								{t('players.col.rate')}
								<span>{sortMark('rate')}</span>
							</button>
						</th>
						<th class="num score" aria-sort={ariaSort('score')}>
							<button type="button" class="sort" class:on={sortKey === 'score'} onclick={() => toggleSort('score')}>
								{t('players.col.score')}
								<span>{sortMark('score')}</span>
							</button>
						</th>
						<th class="num done" aria-sort={ariaSort('done')}>
							<button type="button" class="sort" class:on={sortKey === 'done'} onclick={() => toggleSort('done')}>
								{t('players.col.advancements')}
								<span>{sortMark('done')}</span>
							</button>
						</th>
					</tr>
				</thead>
				<tbody>
					{#each shown as row (row.player.uuid)}
						<tr
							tabindex="0"
							onclick={(event) => openRow(event, row.player.name)}
							onkeydown={(event) => onRowKey(event, row.player.name)}
						>
							<td class="rank">{row.rank}</td>
							<td class="player">
								<a href={hrefFor(row.player.name)}>
									<PlayerSkin uuid={row.player.uuid} variant="head" scale={5} />
									<span class="name">{row.player.name}</span>
								</a>
							</td>
							<td class="num hours">{row.player.playHoursDisplay ?? '0 h'}</td>
							<td class="num rate">{row.player.scorePerHourDisplay ?? '0/h'}</td>
							<td class="num score">
								<RankingScore value={row.player.championDisplay ?? '0'} />
							</td>
							<td class="num done">
								{t('advancements.doneOf', {
									done: row.player.advancements?.done ?? 0,
									total: row.player.advancements?.total ?? 0
								})}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
{/if}

<style>
	h1 {
		margin: 0 0 0.4rem;
		text-align: center;
	}

	.lead {
		color: var(--color-muted);
		text-align: center;
	}

	.filters {
		margin: 0.9rem 0 0.65rem;
	}

	.meta {
		margin: 0 0 0.75rem;
		color: var(--color-muted);
	}

	.empty {
		color: var(--color-muted);
	}

	.wrap {
		overflow-x: auto;
	}

	table {
		width: 100%;
		border-collapse: separate;
		border-spacing: 0 0.35rem;
	}

	th {
		padding: 0 0.6rem 0.35rem;
		color: var(--color-muted);
		font-weight: 400;
		text-align: left;
		white-space: nowrap;
	}

	th.num,
	td.num {
		text-align: right;
	}

	td {
		padding: 0.5rem 0.6rem;
		vertical-align: middle;
		white-space: nowrap;
	}

	.sort {
		display: inline-flex;
		align-items: center;
		justify-content: inherit;
		gap: 0.35rem;
		min-height: 0;
		padding: 0;
		color: inherit;
		font: inherit;
		text-align: inherit;
		text-shadow: inherit;
		background: none;
		border: none;
		box-shadow: none;
		cursor: pointer;
	}

	.sort:hover,
	.sort:focus-visible,
	.sort.on {
		color: var(--color-gold);
		background: none;
		border: none;
		box-shadow: none;
	}

	th.num .sort {
		width: 100%;
		justify-content: flex-end;
	}

	tbody tr {
		cursor: pointer;
	}

	tbody td:first-child {
		border-left: 2px solid var(--color-border);
	}

	tbody td:last-child {
		border-right: 2px solid var(--color-border);
	}

	tbody td {
		background: var(--slot-bg);
		border-top: 2px solid var(--color-border);
		border-bottom: 2px solid var(--color-border);
	}

	tbody tr:hover td {
		background: var(--slot-hover);
		color: var(--color-hover);
	}

	.rank {
		width: 2.4rem;
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
	}

	.player a {
		display: flex;
		align-items: center;
		gap: 0.7rem;
		min-width: 0;
	}

	.name {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.hours {
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
	}

	.num {
		font-variant-numeric: tabular-nums;
	}

	@media (max-width: 640px) {
		.score,
		.done {
			display: none;
		}

		th,
		td {
			padding-left: 0.4rem;
			padding-right: 0.4rem;
		}
	}
</style>
