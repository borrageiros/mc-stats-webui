<script lang="ts">
	import Loader from '$lib/components/Loader.svelte';
	import minecoin from '$lib/assets/minecoin.webp';

	let { value }: { value: string } = $props();
	let ready = $state(false);
	let imgEl = $state<HTMLImageElement | undefined>();

	$effect(() => {
		const el = imgEl;
		if (el && el.complete && el.naturalWidth > 0) {
			ready = true;
		}
	});
</script>

<span class="score">
	{value}
	<span class="coin">
		{#if !ready}
			<Loader embed />
		{/if}
		<img
			bind:this={imgEl}
			class:ready
			src={minecoin}
			alt=""
			width="16"
			height="16"
			onload={() => {
				ready = true;
			}}
		/>
	</span>
</span>

<style>
	.score {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		color: var(--color-gold);
		font-variant-numeric: tabular-nums;
		white-space: nowrap;
	}

	.coin {
		position: relative;
		width: 1.05rem;
		height: 1.05rem;
		flex-shrink: 0;
		display: inline-grid;
		place-items: center;
	}

	img {
		width: 1.05rem;
		height: 1.05rem;
		image-rendering: pixelated;
		flex-shrink: 0;
		display: block;
		opacity: 0;
	}

	img.ready {
		opacity: 1;
	}
</style>
