<script lang="ts">
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import RankingScore from '$lib/components/RankingScore.svelte';
	import Tooltip from '$lib/components/Tooltip.svelte';
	import { t } from '$lib/i18n/i18n.svelte';
	import type { LeaderboardEntry } from '$lib/types';

	let {
		entries,
		coin = false,
		hint = ''
	}: {
		entries: LeaderboardEntry[];
		coin?: boolean;
		hint?: string;
	} = $props();

	const first = $derived(entries[0] ?? null);
	const second = $derived(entries[1] ?? null);
	const third = $derived(entries[2] ?? null);

	type Medal = 'gold' | 'silver' | 'bronze';

	let compact = $state(false);

	$effect(() => {
		const query = window.matchMedia('(max-width: 640px)');
		const apply = () => {
			compact = query.matches;
		};
		apply();
		query.addEventListener('change', apply);
		return () => query.removeEventListener('change', apply);
	});
</script>

{#snippet plate(entry: LeaderboardEntry | null, medal: Medal)}
	{#if entry}
		<a class="place {medal}" href="/players/{encodeURIComponent(entry.name)}">
			<div class="figure">
				<PlayerSkin uuid={entry.uuid} variant="body" presentation="podium" controls={false} />
			</div>
			<div class="card">
				<p class="name">{entry.name}</p>
				<p class="meta">
					<Tooltip text={hint}>
						{#if coin}
							<RankingScore value={entry.display} />
						{:else}
							<span class="value">{entry.display}</span>
						{/if}
					</Tooltip>
				</p>
			</div>
		</a>
	{:else}
		<div class="place empty {medal}">
			<div class="figure vacant"></div>
			<div class="card">
				<p class="name">{t('home.nobody')}</p>
			</div>
		</div>
	{/if}
{/snippet}

{#if !compact}
	<section class="podium" aria-label={t('home.podium')}>
		{@render plate(second, 'silver')}
		{@render plate(first, 'gold')}
		{@render plate(third, 'bronze')}
	</section>
{/if}

<style>
	.podium {
		display: grid;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1.22fr) minmax(0, 1fr);
		align-items: end;
		gap: 0.55rem;
		margin: 0 0 1.6rem;
		padding-top: 7rem;
		overflow: visible;
		position: relative;
		z-index: 2;
	}

	.place {
		display: flex;
		flex-direction: column;
		align-items: stretch;
		min-width: 0;
		position: relative;
		overflow: visible;
	}

	.figure {
		margin: 0 0.15rem -7.1rem;
		z-index: 0;
		overflow: visible;
		pointer-events: none;
	}

	.bronze .figure {
		height: 13.2rem;
	}

	.silver .figure {
		height: 14.6rem;
	}

	.gold .figure {
		height: 17.2rem;
	}

	.place :global(canvas) {
		transform-origin: 50% 80%;
	}

	.bronze :global(canvas) {
		transform: scale(1.28, 1.12) translateY(12%);
	}

	.silver :global(canvas) {
		transform: scale(1.4, 1.22) translateY(14%);
	}

	.gold :global(canvas) {
		transform: scale(1.56, 1.38) translateY(22%);
	}

	.figure.vacant {
		opacity: 0.25;
		background: linear-gradient(to top, var(--color-plate), transparent);
	}

	.card {
		position: relative;
		z-index: 1;
		isolation: isolate;
		display: flex;
		flex-direction: column;
		justify-content: center;
		text-align: center;
		border: 2px solid var(--color-border);
		box-shadow: inset 0 0 0 2px var(--button-shadow);
	}

	.bronze .card {
		min-height: 3.35rem;
		padding: 0.5rem 0.6rem 0.45rem;
		background: var(--color-plate-bronze);
		border-color: var(--color-bronze);
		box-shadow: inset 0 0 0 2px color-mix(in srgb, var(--color-bronze) 55%, var(--button-shadow));
	}

	.silver .card {
		min-height: 4.55rem;
		padding: 0.75rem 0.7rem 0.65rem;
		background: var(--color-plate-silver);
		border-color: var(--color-silver);
		box-shadow: inset 0 0 0 2px color-mix(in srgb, var(--color-silver) 55%, var(--button-shadow));
	}

	.gold .card {
		min-height: 6.05rem;
		padding: 1.05rem 0.75rem 0.9rem;
		background: var(--color-plate-gold);
		border-color: var(--color-gold);
		box-shadow: inset 0 0 0 2px color-mix(in srgb, var(--color-gold) 55%, var(--button-shadow));
	}

	.name {
		margin: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.95rem;
	}

	.meta {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.45rem;
		margin: 0.4rem 0 0;
	}

	.value {
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
	}

	.place:hover .name {
		color: var(--color-hover);
	}

	.empty .name {
		color: var(--color-muted);
	}

	@media (max-width: 640px) {
		.podium {
			display: none;
		}
	}
</style>
