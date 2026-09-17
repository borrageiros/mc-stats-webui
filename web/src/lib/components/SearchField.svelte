<script lang="ts">
	let {
		value = $bindable(''),
		placeholder,
		label,
		autofocus = false,
		onkeydown
	}: {
		value: string;
		placeholder: string;
		label?: string;
		autofocus?: boolean;
		onkeydown?: (event: KeyboardEvent) => void;
	} = $props();

	let input = $state<HTMLInputElement | undefined>();

	$effect(() => {
		if (autofocus) {
			input?.focus();
		}
	});
</script>

<input
	bind:this={input}
	class="search"
	type="search"
	bind:value
	{placeholder}
	aria-label={label ?? placeholder}
	autocomplete="off"
	spellcheck="false"
	{onkeydown}
/>

<style>
	.search {
		width: 100%;
		min-height: 2.5rem;
		padding: 0.35rem 0.8rem;
		color: inherit;
		font: inherit;
		background: var(--slot-bg);
		border: 2px solid var(--color-border);
		box-shadow: inset 2px 2px 0 var(--button-shadow);
		outline: none;
	}

	.search:focus-visible {
		border-color: var(--button-border-hover);
	}
</style>
