<script lang="ts">
	import Icon from '$lib/components/Icon.svelte';
	import { closeMenu, headerMenu, registerMenuRoot, toggleMenu } from '$lib/components/headerMenu.svelte';
	import type { IconName } from '$lib/components/iconNames';

	export type HeaderSelectOption = {
		id: string;
		label: string;
		icon: IconName;
	};

	let {
		id,
		label,
		value,
		options,
		onselect
	}: {
		id: string;
		label: string;
		value: string;
		options: HeaderSelectOption[];
		onselect: (id: string) => void;
	} = $props();

	const open = $derived(headerMenu.open === id);
	const current = $derived(options.find((option) => option.id === value) ?? options[0]);

	function choose(next: string) {
		onselect(next);
		closeMenu();
	}
</script>

<div class="wrap" use:registerMenuRoot>
	<button
		type="button"
		class="mc-btn trigger"
		aria-haspopup="listbox"
		aria-expanded={open}
		aria-label="{label}: {current.label}"
		onclick={() => toggleMenu(id)}
	>
		<Icon name={current.icon} />
		<span>{current.label}</span>
		<span class="chevron" class:up={open}><Icon name="chevron" /></span>
	</button>
	{#if open}
		<ul class="panel" role="listbox" aria-label={label}>
			{#each options as option}
				<li>
					<button
						type="button"
						role="option"
						aria-selected={option.id === value}
						class:on={option.id === value}
						onclick={() => choose(option.id)}
					>
						<Icon name={option.icon} />
						<span>{option.label}</span>
					</button>
				</li>
			{/each}
		</ul>
	{/if}
</div>

<style>
	.wrap {
		position: relative;
		width: 100%;
	}

	.trigger {
		position: relative;
		width: 100%;
		justify-content: center;
	}

	.chevron {
		position: absolute;
		right: 0.8rem;
		display: flex;
	}

	.chevron.up {
		transform: rotate(180deg);
	}

	.panel {
		position: absolute;
		top: calc(100% + 0.35rem);
		right: 0;
		left: 0;
		z-index: 30;
		margin: 0;
		padding: 0.2rem;
		list-style: none;
		background: var(--color-card);
		border: 2px solid var(--color-border);
	}

	.panel button {
		width: 100%;
		justify-content: center;
		min-height: 2.2rem;
	}

	.panel li + li {
		margin-top: 0.2rem;
	}
</style>
