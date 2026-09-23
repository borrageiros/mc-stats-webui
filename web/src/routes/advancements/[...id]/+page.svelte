<script lang="ts">
	import { page } from '$app/state';
	import { advancementDescription, advancementTitle } from '$lib/advancements';
	import { getAdvancement, getAdvancementMost } from '$lib/api';
	import BackNav from '$lib/components/BackNav.svelte';
	import HomePodium from '$lib/components/HomePodium.svelte';
	import Loader from '$lib/components/Loader.svelte';
	import McItem from '$lib/components/McItem.svelte';
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import Tooltip from '$lib/components/Tooltip.svelte';
	import { boardHint, formatWhen, pageTitle, t } from '$lib/i18n/i18n.svelte';
	import type { AdvancementDetailResponse, LeaderboardResponse } from '$lib/types';

	let board = $state<LeaderboardResponse | null>(null);
	let detail = $state<AdvancementDetailResponse | null>(null);
	let failed = $state(false);
	let loading = $state(true);
	let query = $state('');

	const boardId = $derived(
		Array.isArray(page.params.id) ? page.params.id.join('/') : (page.params.id ?? '')
	);
	const most = $derived(boardId === 'most');
	const heading = $derived(
		most
			? t('advancements.most')
			: detail
				? advancementTitle(detail.id, detail.title)
				: t('nav.advancements')
	);
	const hint = $derived(
		most
			? boardHint('advancements')
			: detail
				? advancementDescription(detail.id, detail.description)
				: ''
	);
	const entries = $derived.by(() => {
		const rows = (most ? board?.entries : detail?.entries) ?? [];
		if (most) {
			return rows;
		}
		return rows.map((entry) => ({
			...entry,
			display: formatWhen(entry.value, entry.display)
		}));
	});
	const shown = $derived.by(() => {
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
		detail = null;
		failed = false;
		loading = true;
		query = '';
		const request = most ? getAdvancementMost() : getAdvancement(id);
		request
			.then((data) => {
				if (most) {
					board = data as LeaderboardResponse;
				} else {
					detail = data as AdvancementDetailResponse;
				}
			})
			.catch(() => {
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
	{#if most}
		<McItem id="minecraft:experience_bottle" />
	{:else if detail}
		<McItem id={detail.icon} />
	{/if}
	{heading}
</h1>
<p class="lead">
	{#if most}
		{t('advancements.mostLead')}
	{:else if detail}
		{advancementDescription(detail.id, detail.description)}
		{#if detail.hidden}
			{' · '}{t('advancements.hidden')}
		{/if}
	{/if}
</p>

{#if failed}
	<p class="empty">{t('advancements.loadError')}</p>
{:else if loading}
	<Loader />
{:else}
	{#if entries.length === 0}
		<p class="empty">{most ? t('advancements.nobody') : t('advancements.empty')}</p>
	{:else}
		<HomePodium entries={entries} hint={hint} />
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
								<span class="value">{entry.display}</span>
							</Tooltip>
						</a>
					</li>
				{/each}
			</ol>
		{:else}
			<p class="empty">{t('player.noMatches')}</p>
		{/if}
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
