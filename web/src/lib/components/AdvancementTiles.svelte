<script lang="ts">
	import AdvancementTile from '$lib/components/AdvancementTile.svelte';

	let {
		items,
		dim,
		showHolders = false
	}: {
		items: Array<{
			id: string;
			title?: string;
			description?: string;
			icon: string;
			holders?: number;
			hidden?: boolean;
		}>;
		dim?: (item: { id: string; hidden?: boolean }) => boolean;
		showHolders?: boolean;
	} = $props();
</script>

<div class="tiles">
	{#each items as item (item.id)}
		<AdvancementTile {item} dim={dim?.(item) ?? false} {showHolders} />
	{/each}
</div>

<style>
	.tiles {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(8.4rem, 1fr));
		gap: 0.45rem;
	}
</style>
