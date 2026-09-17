<script lang="ts">
	import Icon from '$lib/components/Icon.svelte';
	import PlayerSkin from '$lib/components/PlayerSkin.svelte';
	import SearchField from '$lib/components/SearchField.svelte';
	import { closeMenu, headerMenu, registerMenuRoot, toggleMenu } from '$lib/components/headerMenu.svelte';
	import { t } from '$lib/i18n/i18n.svelte';
	import type { PlayerSummary } from '$lib/types';

	let {
		id,
		label,
		value,
		players,
		placeholder,
		onselect
	}: {
		id: string;
		label: string;
		value: string;
		players: PlayerSummary[];
		placeholder: string;
		onselect: (name: string) => void;
	} = $props();

	let query = $state('');
	const open = $derived(headerMenu.open === id);
	const current = $derived(players.find((player) => player.name === value) ?? null);
	const shown = $derived.by(() => {
		const needle = query.trim().toLowerCase();
		if (!needle) {
			return players;
		}
		return players.filter((player) => player.name.toLowerCase().includes(needle));
	});
	const searching = $derived(query.trim().length > 0);

	$effect(() => {
		if (!open) {
			query = '';
		}
	});

	function choose(name: string) {
		onselect(name);
		closeMenu();
	}

	function onSearchKey(event: KeyboardEvent) {
		if (event.key !== 'Enter') {
			return;
		}
		event.preventDefault();
		if (shown.length === 1) {
			choose(shown[0].name);
		}
	}
</script>

<div class="wrap" class:raised={open} use:registerMenuRoot>
	<button
		type="button"
		class="mc-btn trigger"
		aria-haspopup="listbox"
		aria-expanded={open}
		aria-label="{label}: {current?.name ?? (value || placeholder)}"
		onclick={() => toggleMenu(id)}
	>
		{#if current}
			<PlayerSkin uuid={current.uuid} variant="head" scale={5} />
			<span>{current.name}</span>
		{:else if value}
			<span>{value}</span>
		{:else}
			<span>{placeholder}</span>
		{/if}
		<span class="chevron" class:up={open}><Icon name="chevron" /></span>
	</button>
	{#if open}
		<div class="panel">
			<div class="finder">
				<SearchField
					bind:value={query}
					placeholder={t('home.search')}
					label={t('home.search')}
					autofocus
					onkeydown={onSearchKey}
				/>
			</div>
			<ul class="list" role="listbox" aria-label={label}>
				{#if !searching}
					<li>
						<button
							type="button"
							role="option"
							aria-selected={!value}
							class:on={!value}
							onclick={() => choose('')}
						>
							<span>{placeholder}</span>
						</button>
					</li>
				{/if}
				{#each shown as player}
					<li>
						<button
							type="button"
							role="option"
							aria-selected={player.name === value}
							class:on={player.name === value}
							onclick={() => choose(player.name)}
						>
							<PlayerSkin uuid={player.uuid} variant="head" scale={5} />
							<span>{player.name}</span>
						</button>
					</li>
				{/each}
				{#if shown.length === 0 && searching}
					<li class="empty">{t('player.noMatches')}</li>
				{/if}
			</ul>
		</div>
	{/if}
</div>

<style>
	.wrap {
		position: relative;
		width: 100%;
	}

	.wrap.raised {
		z-index: 40;
	}

	.trigger {
		position: relative;
		width: 100%;
		justify-content: center;
		gap: 0.55rem;
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
		display: flex;
		flex-direction: column;
		margin: 0;
		padding: 0.2rem;
		max-height: 18rem;
		background: var(--color-card);
		border: 2px solid var(--color-border);
	}

	.finder {
		flex: 0 0 auto;
		margin-bottom: 0.2rem;
	}

	.finder :global(.search) {
		min-height: 2.2rem;
	}

	.list,
	.list li {
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.list {
		overflow: auto;
	}

	.list button {
		width: 100%;
		justify-content: center;
		gap: 0.55rem;
		min-height: 2.8rem;
	}

	.list li + li {
		margin-top: 0.2rem;
	}

	.empty {
		padding: 0.55rem 0.7rem;
		color: var(--color-muted);
	}
</style>
