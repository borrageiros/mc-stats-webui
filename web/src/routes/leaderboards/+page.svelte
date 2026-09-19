<script lang="ts">
	import { getLeaderboards, getPlayerProfile, getPlayers } from '$lib/api';
	import Loader from '$lib/components/Loader.svelte';
	import McItem from '$lib/components/McItem.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import { i18n, pageTitle, t, tDynamic } from '$lib/i18n/i18n.svelte';
	import { cappedResults, deferredValue } from '$lib/search.svelte';
	import { statGroups, type StatEntry, type StatGroup } from '$lib/playerStats';
	import {
		matchesVanillaQuery,
		vanillaBoardHref,
		vanillaGroupIcons,
		vanillaGroups,
		vanillaIcon,
		vanillaLabel,
		type VanillaGroup
	} from '$lib/vanillaStats';

	let query = $state('');
	let vanilla = $state<Partial<Record<VanillaGroup, string[]>>>({});
	let vanillaReady = $state(false);
	let openGroups = $state(closedVanilla());
	const filterQuery = deferredValue(() => query);

	const searching = $derived(filterQuery.current.trim().length > 0);

	function closedVanilla(): Record<VanillaGroup, boolean> {
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

	function curatedStats(group: StatGroup): StatEntry[] {
		void i18n.locale;
		const needle = filterQuery.current.trim().toLowerCase();
		if (!needle) {
			return group.stats;
		}
		return group.stats.filter((stat) => {
			const label = tDynamic('leaderboard', stat.id).toLowerCase();
			return label.includes(needle) || stat.id.toLowerCase().includes(needle);
		});
	}

	function vanillaIds(group: VanillaGroup): string[] {
		void i18n.locale;
		return (vanilla[group] ?? []).filter((id) => matchesVanillaQuery(group, id, filterQuery.current));
	}

	const curatedShown = $derived(statGroups.map((group) => ({ group, stats: curatedStats(group) })));
	const vanillaShown = $derived(
		vanillaGroups.map((group) => {
			const matched = vanillaIds(group);
			return { group, matched: matched.length, ids: cappedResults(matched, searching) };
		})
	);
	const hasMatches = $derived(
		curatedShown.some((section) => section.stats.length > 0) ||
			(vanillaReady && vanillaShown.some((section) => section.matched > 0))
	);

	$effect(() => {
		loadVanilla();
	});

	async function loadVanilla() {
		vanillaReady = false;
		const next: Partial<Record<VanillaGroup, string[]>> = {};
		try {
			const data = await getLeaderboards();
			for (const group of vanillaGroups) {
				next[group] = data.vanilla?.[group] ?? [];
			}
			if (vanillaGroups.some((group) => (next[group] ?? []).length > 0)) {
				vanilla = next;
				vanillaReady = true;
				return;
			}
		} catch {
			// Use a player profile, which already lists every vanilla key.
		}
		try {
			const list = await getPlayers();
			const first = list.players[0];
			if (!first) {
				vanilla = {};
				vanillaReady = true;
				return;
			}
			const player = await getPlayerProfile(first.name);
			for (const group of vanillaGroups) {
				next[group] = (player.vanilla?.[group] ?? []).map((row) => row.id);
			}
			vanilla = next;
		} catch {
			vanilla = {};
		} finally {
			vanillaReady = true;
		}
	}
</script>

<svelte:head>
	<title>{pageTitle(t('nav.leaderboards'))}</title>
</svelte:head>

<h1>{t('leaderboards.heading')}</h1>
<p class="lead">{t('leaderboards.lead')}</p>

<div class="filters">
	<SearchField bind:value={query} placeholder={t('player.search')} />
</div>

{#if hasMatches}
	{#each curatedShown as section}
		{#if section.stats.length > 0}
			<section>
				<h2>
					<McItem id={section.group.icon} compact />
					{tDynamic('category', section.group.category)}
				</h2>
				<div class="grid">
					{#each section.stats as stat}
						<a class="mc-btn" href="/leaderboards/{stat.id}">
							<McItem id={stat.icon} compact />
							<span>{tDynamic('leaderboard', stat.id)}</span>
						</a>
					{/each}
				</div>
			</section>
		{/if}
	{/each}

	{#if !vanillaReady}
		<Loader />
	{:else}
		{#each vanillaShown as section (section.group + i18n.locale)}
			{#if section.matched > 0}
				<section class="pack">
					<div class="pack-head">
						<button
							class="fold"
							type="button"
							aria-expanded={isOpen(section.group)}
							onclick={() => toggleGroup(section.group)}
						>
							<span class="chevron" class:open={isOpen(section.group)}>▸</span>
							<span class="pack-title">
								<McItem id={vanillaGroupIcons[section.group]} compact />
								{tDynamic('player.group', section.group)}
								<span class="count"
									>{t('player.shown', {
										shown: section.ids.length,
										total: (vanilla[section.group] ?? []).length
									})}</span
								>
							</span>
						</button>
					</div>
					{#if isOpen(section.group)}
						<div class="tiles">
							{#each section.ids as id (id)}
								<a class="tile slot" href={vanillaBoardHref(section.group, id)}>
									<span class="glyph">
										<McItem id={vanillaIcon(section.group, id)} />
									</span>
									<span class="label">{vanillaLabel(section.group, id)}</span>
								</a>
							{/each}
						</div>
					{/if}
				</section>
			{/if}
		{/each}
	{/if}
{:else if !vanillaReady}
	<Loader />
{:else}
	<p class="empty">{t('player.noMatches')}</p>
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
		margin: 0.9rem 0 1.1rem;
	}

	h2 {
		display: flex;
		align-items: center;
		gap: 0.45rem;
		margin: 1.6rem 0 0.6rem;
		font-size: 1rem;
		color: var(--color-gold);
	}

	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
		gap: 0.5rem;
	}

	.grid :global(.mc-btn) {
		width: 100%;
		justify-content: flex-start;
		text-align: left;
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
		min-height: 7.2rem;
	}

	.glyph {
		display: grid;
		place-items: center;
		width: 2.6rem;
		height: 2.6rem;
		margin-bottom: 0.15rem;
	}

	.label {
		color: var(--color-muted);
		font-size: 0.72rem;
		line-height: 1.25;
	}

	.empty {
		color: var(--color-muted);
		text-align: center;
	}
</style>
