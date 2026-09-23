<script lang="ts">
	export type TabItem = {
		id: string;
		label: string;
	};

	let {
		tabs,
		selected = $bindable(),
		label
	}: {
		tabs: TabItem[];
		selected: string;
		label: string;
	} = $props();
</script>

<div
	class="tabs"
	class:stack={tabs.length > 2}
	style="--tab-count: {tabs.length}"
	role="tablist"
	aria-label={label}
>
	{#each tabs as tab (tab.id)}
		<button
			type="button"
			class="mc-btn"
			class:on={selected === tab.id}
			role="tab"
			aria-selected={selected === tab.id}
			onclick={() => (selected = tab.id)}
		>
			{tab.label}
		</button>
	{/each}
</div>

<style>
	.tabs {
		display: grid;
		grid-template-columns: repeat(var(--tab-count), minmax(0, 1fr));
		gap: 0.45rem;
		margin: 0 0 1.15rem;
	}

	.tabs :global(.mc-btn) {
		width: 100%;
		min-height: 2.15rem;
		padding: 0.28rem 0.55rem;
	}

	.tabs :global(.mc-btn.on) {
		color: #ffffa0;
		background: var(--button-face-hover);
		border-color: var(--button-border-hover);
		box-shadow:
			inset 2px 2px 0 var(--button-highlight-hover),
			inset -2px -2px 0 var(--button-shadow-hover);
	}

	@media (max-width: 520px) {
		.tabs.stack {
			grid-template-columns: 1fr;
		}
	}
</style>
