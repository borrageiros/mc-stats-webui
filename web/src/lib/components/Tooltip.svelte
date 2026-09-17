<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		text,
		align = 'center',
		placement = 'top',
		focusable = false,
		children
	}: {
		text: string;
		align?: 'start' | 'center' | 'end';
		placement?: 'top' | 'bottom';
		focusable?: boolean;
		children: Snippet;
	} = $props();
</script>

{#if text}
	<span class="wrap {align} {placement}">
		{#if focusable}
			<button type="button" class="hit marked">
				{@render children()}
			</button>
		{:else}
			<span class="hit">
				{@render children()}
			</span>
		{/if}
		<span class="bubble" role="tooltip">{text}</span>
	</span>
{:else}
	{@render children()}
{/if}

<style>
	.wrap {
		position: relative;
		display: inline-flex;
		align-items: center;
		max-width: 100%;
	}

	.hit {
		display: inline-flex;
		align-items: center;
		max-width: 100%;
		min-height: 0;
		padding: 0;
		color: inherit;
		font: inherit;
		text-align: inherit;
		text-shadow: inherit;
		background: none;
		border: none;
		box-shadow: none;
		cursor: help;
	}

	button.hit:hover,
	button.hit:focus-visible {
		color: inherit;
		background: none;
		border: none;
		box-shadow: none;
	}

	.hit.marked {
		border-bottom: 2px dotted color-mix(in srgb, currentColor 55%, transparent);
	}

	.hit:focus-visible {
		outline: 2px solid var(--button-border-hover);
		outline-offset: 2px;
	}

	.bubble {
		position: absolute;
		z-index: 50;
		width: max-content;
		max-width: min(16.5rem, 70vw);
		padding: 0.4rem 0.55rem;
		color: var(--color-fg);
		font-size: 0.85rem;
		line-height: 1.35;
		text-align: left;
		text-shadow: var(--text-shadow);
		background: var(--color-card);
		border: 2px solid var(--color-border);
		box-shadow: inset 0 0 0 2px var(--button-shadow);
		opacity: 0;
		visibility: hidden;
		pointer-events: none;
	}

	.top .bubble {
		bottom: calc(100% + 0.4rem);
	}

	.bottom .bubble {
		top: calc(100% + 0.4rem);
	}

	.start .bubble {
		left: 0;
	}

	.center .bubble {
		left: 50%;
		transform: translateX(-50%);
	}

	.end .bubble {
		right: 0;
	}

	.wrap:hover,
	.wrap:focus-within {
		z-index: 60;
	}

	.wrap:hover .bubble,
	.wrap:focus-within .bubble {
		opacity: 1;
		visibility: visible;
	}
</style>
