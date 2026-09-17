<script lang="ts">
	import { t } from '$lib/i18n/i18n.svelte';

	let {
		compact = false,
		embed = false
	}: {
		compact?: boolean;
		embed?: boolean;
	} = $props();
</script>

<div
	class="loader"
	class:compact
	class:embed
	role="status"
	aria-live="polite"
	aria-busy="true"
	aria-label={t('common.loading')}
>
	<span class="blocks" aria-hidden="true">
		<i></i>
		<i></i>
		<i></i>
	</span>
</div>

<style>
	.loader {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 2.4rem 1rem;
	}

	.loader.compact {
		padding: 0.85rem 0.4rem;
	}

	.loader.embed {
		position: absolute;
		inset: 0;
		padding: 0;
		pointer-events: none;
	}

	.blocks {
		display: inline-flex;
		align-items: flex-end;
		gap: 0.28rem;
		height: 1.35rem;
	}

	.embed .blocks {
		gap: 0.12rem;
		height: 0.72rem;
		transform: scale(0.72);
		transform-origin: center bottom;
	}

	.blocks i {
		display: block;
		width: 0.72rem;
		height: 0.72rem;
		background: var(--color-gold);
		border: 2px solid var(--color-border);
		box-shadow: inset 1px 1px 0 var(--button-highlight), inset -1px -1px 0 var(--button-shadow);
		animation: bounce 0.72s ease-in-out infinite;
	}

	.embed .blocks i {
		width: 0.42rem;
		height: 0.42rem;
		border-width: 1px;
	}

	.blocks i:nth-child(2) {
		animation-delay: 0.12s;
		background: var(--color-accent);
	}

	.blocks i:nth-child(3) {
		animation-delay: 0.24s;
		background: var(--color-bronze);
	}

	@keyframes bounce {
		0%,
		100% {
			transform: translateY(0);
		}
		40% {
			transform: translateY(-0.55rem);
		}
	}

	.embed .blocks i {
		animation-name: bounce-embed;
	}

	@keyframes bounce-embed {
		0%,
		100% {
			transform: translateY(0);
		}
		40% {
			transform: translateY(-0.28rem);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.blocks i {
			animation: none;
		}
	}
</style>
