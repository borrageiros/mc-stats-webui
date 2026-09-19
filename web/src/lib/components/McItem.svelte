<script lang="ts">
	import { itemLetter, itemTextureUrls } from '$lib/mc/items';

	let { id, compact = false }: { id: string; compact?: boolean } = $props();
	let attempt = $state(0);
	let ready = $state(false);
	let imgEl = $state<HTMLImageElement | undefined>();

	$effect(() => {
		void id;
		attempt = 0;
		ready = false;
	});

	const urls = $derived(itemTextureUrls(id));
	const src = $derived(urls[attempt]);

	$effect(() => {
		const el = imgEl;
		void src;
		if (el && el.complete && el.naturalWidth > 0) {
			ready = true;
		}
	});
</script>

<span class="frame" class:compact>
	{#if src}
		<img
			bind:this={imgEl}
			class="icon"
			class:ready
			class:compact
			src={src}
			alt=""
			width="32"
			height="32"
			decoding="async"
			loading="lazy"
			onload={() => {
				ready = true;
			}}
			onerror={() => {
				ready = false;
				attempt += 1;
			}}
		/>
	{:else}
		<span class="letter" class:compact>{itemLetter(id)}</span>
	{/if}
</span>

<style>
	.frame {
		position: relative;
		width: 2.25rem;
		height: 2.25rem;
		flex-shrink: 0;
		display: inline-grid;
		place-items: center;
	}

	.frame.compact {
		width: 1.5rem;
		height: 1.5rem;
	}

	.icon {
		width: 2.25rem;
		height: 2.25rem;
		image-rendering: pixelated;
		flex-shrink: 0;
		display: block;
		opacity: 0;
	}

	.icon.ready {
		opacity: 1;
	}

	.letter {
		width: 2.25rem;
		height: 2.25rem;
		display: grid;
		place-items: center;
		color: var(--color-gold);
		background: var(--slot-bg);
		border: 2px solid var(--color-border);
		font-size: 1rem;
		flex-shrink: 0;
	}

	.icon.compact,
	.letter.compact {
		width: 1.5rem;
		height: 1.5rem;
	}

	.letter.compact {
		font-size: 0.8rem;
	}
</style>
