<script lang="ts">
	import {
		advancementDescription,
		advancementHref,
		advancementTitle
	} from '$lib/advancements';
	import McItem from '$lib/components/McItem.svelte';
	import Tooltip from '$lib/components/Tooltip.svelte';
	import { t } from '$lib/i18n/i18n.svelte';

	let {
		item,
		dim = false,
		showHolders = false
	}: {
		item: {
			id: string;
			title?: string;
			description?: string;
			icon: string;
			holders?: number;
		};
		dim?: boolean;
		showHolders?: boolean;
	} = $props();
</script>

<div class="cell">
	<Tooltip text={advancementDescription(item.id, item.description ?? '')}>
		<a class="tile slot" class:dim href={advancementHref(item.id)}>
			<span class="glyph">
				<McItem id={item.icon} />
			</span>
			<span class="label">{advancementTitle(item.id, item.title)}</span>
			{#if showHolders}
				<span class="holders">{t('advancements.holders', { count: item.holders ?? 0 })}</span>
			{/if}
		</a>
	</Tooltip>
</div>

<style>
	.cell,
	.cell :global(.wrap),
	.cell :global(.hit) {
		width: 100%;
		height: 100%;
	}

	.cell :global(.hit) {
		cursor: pointer;
	}

	.tile {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.28rem;
		width: 100%;
		height: 100%;
		padding: 0.7rem 0.45rem 0.55rem;
		text-align: center;
		min-height: 8.2rem;
	}

	.tile.dim {
		opacity: 0.45;
	}

	.glyph {
		display: grid;
		place-items: center;
		width: 2.6rem;
		height: 2.6rem;
		margin-bottom: 0.15rem;
	}

	.label,
	.holders {
		color: var(--color-muted);
		font-size: 0.72rem;
		line-height: 1.25;
	}

	.holders {
		color: var(--color-accent);
	}
</style>
