<script lang="ts">
	import { page } from '$app/state';
	import BackNav from '$lib/components/BackNav.svelte';
	import Loader from '$lib/components/Loader.svelte';
	import McItem from '$lib/components/McItem.svelte';
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import RankingScore from '$lib/components/RankingScore.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import Tooltip from '$lib/components/Tooltip.svelte';
	import { getPlayerProfile } from '$lib/api';
	import { boardHint, i18n, pageTitle, t, tDynamic } from '$lib/i18n/i18n.svelte';
	import { gradeItems, gradeOrder, statGroups } from '$lib/playerStats';
	import type { PlayerDetail } from '$lib/types';
	import {
		hideZerosByDefault,
		vanillaBoardHref,
		vanillaGroups,
		vanillaIcon,
		vanillaLabel,
		visibleVanilla,
		type VanillaGroup
	} from '$lib/vanillaStats';

	let player = $state<PlayerDetail | null>(null);
	let failed = $state(false);
	let loading = $state(true);
	let query = $state('');
	let hideZeros = $state({ ...hideZerosByDefault });
	let openGroups = $state(closedGroups());

	const searching = $derived(query.trim().length > 0);

	function closedGroups(): Record<VanillaGroup, boolean> {
		return {
			custom: false,
			killed: false,
			killed_by: false,
			mined: false,
			crafted: false,
			used: false,
			broken: false,
			picked_up: false,
			dropped: false
		};
	}

	function isOpen(group: VanillaGroup) {
		return searching || openGroups[group];
	}

	function toggleGroup(group: VanillaGroup) {
		if (searching) {
			return;
		}
		openGroups = { ...openGroups, [group]: !openGroups[group] };
	}

	$effect(() => {
		const name = page.params.name;
		if (!name) {
			return;
		}
		player = null;
		failed = false;
		loading = true;
		query = '';
		hideZeros = { ...hideZerosByDefault };
		openGroups = closedGroups();
		getPlayerProfile(name)
			.then((data) => {
				player = data;
				failed = false;
			})
			.catch(() => {
				player = null;
				failed = true;
			})
			.finally(() => {
				loading = false;
			});
	});

	const shownGrades = $derived(
		player
			? gradeOrder.filter((id) => player && Object.prototype.hasOwnProperty.call(player.grades, id))
			: []
	);
	const scoreHint = $derived.by(() => {
		void i18n.locale;
		return boardHint('champion');
	});
	const playHint = $derived.by(() => {
		void i18n.locale;
		return boardHint('play-time');
	});
	const rateHint = $derived.by(() => {
		void i18n.locale;
		return boardHint('efficient');
	});
</script>

<svelte:head>
	<title>{pageTitle(page.params.name ?? t('title.site'))}</title>
</svelte:head>

<BackNav />

{#if failed}
	<h1>{page.params.name}</h1>
	<p class="muted">{t('player.loadError')}</p>
{:else if loading || !player}
	<h1>{page.params.name}</h1>
	<Loader />
{:else}
	<section class="hero">
		<div class="portrait">
			<PlayerSkin uuid={player.uuid} variant="body" />
		</div>
		<div class="info">
			<div class="identity">
				<div class="title">
					<PlayerSkin uuid={player.uuid} variant="head" />
					<h1>{player.name}</h1>
				</div>
				<p class="scoreline">
					<Tooltip text={scoreHint} align="end" focusable>
						<RankingScore value={player.championDisplay} />
					</Tooltip>
					<span class="rank">{t('player.rankOf', { rank: player.championRank, total: player.trackedPlayers })}</span>
				</p>
				<p class="meta">
					<Tooltip text={playHint} align="end" focusable>
						<span>{t('player.playTime')}: {player.playHoursDisplay}</span>
					</Tooltip>
					<Tooltip text={rateHint} align="end" focusable>
						<span>{t('player.scorePerHour')}: {player.scorePerHourDisplay}</span>
					</Tooltip>
				</p>
			</div>
			{#if shownGrades.length > 0}
				<div class="grades">
					<p class="section">{t('player.grades')}</p>
					{#each shownGrades as id}
						{@const grade = player.grades[id] ?? 0}
						<div class="grade">
							<McItem id={gradeItems[id]} />
							<div class="grade-copy">
								<span>{tDynamic('player.grade', id)}</span>
								<span class="grade-value">{Math.round(grade)}</span>
							</div>
							<div class="bar {id}">
								<i style="width: {Math.min(100, Math.max(0, grade))}%"></i>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</section>

	{#if player.vanilla}
		<div class="filters">
			<SearchField bind:value={query} placeholder={t('player.search')} />
		</div>
		{#each vanillaGroups as group (group + i18n.locale)}
			{@const rows = player.vanilla[group] ?? []}
			{@const shown = visibleVanilla(group, rows, query, hideZeros[group])}
			<section class="pack">
				<div class="pack-head">
					<button
						class="fold"
						type="button"
						aria-expanded={isOpen(group)}
						onclick={() => toggleGroup(group)}
					>
						<span class="chevron" class:open={isOpen(group)}>▸</span>
						<span class="pack-title">
							{tDynamic('player.group', group)}
							<span class="count">{t('player.shown', { shown: shown.length, total: rows.length })}</span>
						</span>
					</button>
					<label class="toggle">
						<input type="checkbox" bind:checked={hideZeros[group]} />
						{t('player.hideZeros')}
					</label>
				</div>
				{#if isOpen(group)}
					{#if shown.length === 0}
						<p class="muted">{t('player.noMatches')}</p>
					{:else}
						<div class="tiles">
							{#each shown as stat (stat.id)}
								<a class="tile slot" class:zero={stat.value <= 0} href={vanillaBoardHref(group, stat.id)}>
									<span class="glyph">
										<McItem id={vanillaIcon(group, stat.id)} />
									</span>
									<span class="amount">{stat.display}</span>
									<span class="label">{vanillaLabel(group, stat.id)}</span>
									{#if stat.value > 0}
										<span class="place">{t('player.rankOf', { rank: stat.rank, total: player.trackedPlayers })}</span>
									{/if}
								</a>
							{/each}
						</div>
					{/if}
				{/if}
			</section>
		{/each}
	{:else}
		{#each statGroups as group}
			<section class="pack">
				<h2>{tDynamic('category', group.category)}</h2>
				<div class="tiles">
					{#each group.stats as stat}
						{@const row = player.stats[stat.id]}
						<a class="tile slot" class:zero={!row || row.value <= 0} href="/leaderboards/{stat.id}">
							<span class="glyph">
								<McItem id={stat.icon} />
							</span>
							<span class="amount">{row?.display ?? '0'}</span>
							<span class="label">{tDynamic('leaderboard', stat.id)}</span>
							{#if row && row.value > 0}
								<span class="place">{t('player.rankOf', { rank: row.rank, total: player.trackedPlayers })}</span>
							{/if}
						</a>
					{/each}
				</div>
			</section>
		{/each}
	{/if}
{/if}

<style>
	.muted {
		color: var(--color-muted);
	}

	.hero {
		display: flex;
		flex-wrap: wrap;
		gap: 1.5rem;
		align-items: stretch;
		margin-bottom: 1.8rem;
	}

	.portrait {
		flex: 0 0 22rem;
		width: 22rem;
		display: flex;
		min-height: 20rem;
	}

	.portrait :global(.skin.body) {
		flex: 1 1 auto;
		width: 100%;
		height: 100%;
		min-height: 20rem;
	}

	.portrait :global(.body .stage) {
		width: 100%;
		height: 100%;
		min-height: 20rem;
	}

	.info {
		min-width: min(100%, 18rem);
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.identity {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		text-align: right;
	}

	.title {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.75rem;
	}

	h1 {
		margin: 0;
	}

	.scoreline {
		display: flex;
		flex-wrap: wrap;
		align-items: baseline;
		justify-content: flex-end;
		gap: 0.75rem;
		margin: 0.85rem 0 0;
	}

	.rank,
	.meta,
	.section,
	.label,
	.place,
	.count {
		color: var(--color-muted);
	}

	.meta {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 0.2rem;
		margin: 0.45rem 0 0;
	}

	.grades {
		margin-top: 1.2rem;
		display: flex;
		flex-direction: column;
		gap: 0.55rem;
	}

	.section {
		margin: 0 0 0.2rem;
		font-size: 0.95rem;
	}

	.grade {
		display: grid;
		grid-template-columns: 2rem 1fr;
		grid-template-rows: auto auto;
		column-gap: 0.7rem;
		row-gap: 0.28rem;
		align-items: center;
	}

	.grade-copy {
		display: flex;
		justify-content: space-between;
		gap: 0.8rem;
		align-items: baseline;
	}

	.grade-value {
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
	}

	.bar {
		grid-column: 2;
		height: 0.55rem;
		background: var(--slot-bg);
		border: 2px solid var(--color-border);
		box-shadow: inset 0 0 0 1px var(--button-shadow);
	}

	.bar i {
		display: block;
		height: 100%;
		background: var(--color-accent);
	}

	.bar.combat i {
		background: #c53a3a;
	}

	.bar.milestones i {
		background: #9b59d0;
	}

	.bar.mining i {
		background: #4fc3f7;
	}

	.bar.explore i {
		background: #55ff55;
	}

	.bar.produce i {
		background: #e6c35c;
	}

	.bar.survive i {
		background: #f28aa0;
	}

	.bar.flex i {
		background: #55ffff;
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
		font-size: 1rem;
	}

	.pack h2 {
		margin: 0 0 0.7rem;
		font-size: 1rem;
		color: var(--color-muted);
	}

	.count {
		margin-left: 0.45rem;
		font-size: 0.78rem;
	}

	.toggle {
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		color: var(--color-muted);
		font-size: 0.78rem;
		cursor: pointer;
	}

	.tiles {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(8.4rem, 1fr));
		gap: 0.45rem;
	}

	.tile {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.28rem;
		padding: 0.7rem 0.45rem 0.55rem;
		text-align: center;
		min-height: 8.2rem;
	}

	.tile.zero {
		opacity: 0.42;
	}

	.glyph {
		display: grid;
		place-items: center;
		width: 2.6rem;
		height: 2.6rem;
		margin-bottom: 0.15rem;
	}

	.amount {
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
		font-size: 1.05rem;
	}

	.label,
	.place {
		font-size: 0.72rem;
		line-height: 1.25;
	}

	.place {
		color: var(--color-accent);
	}
</style>
