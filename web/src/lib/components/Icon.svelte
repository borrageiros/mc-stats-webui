<script lang="ts">
	import type { IconName } from '$lib/components/iconNames';

	let { name }: { name: IconName } = $props();
</script>

{#if name === 'sun'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="12" r="4" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M12 3v2M12 19v2M5 12H3M21 12h-2M6.2 6.2 4.8 4.8M19.2 19.2l-1.4-1.4M17.8 6.2l1.4-1.4M6.2 17.8 4.8 19.2"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
		/>
	</svg>
{:else if name === 'moon'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<path
			d="M20 14.5A8.5 8.5 0 1 1 9.5 4 7 7 0 0 0 20 14.5Z"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'monitor'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<rect
			x="3"
			y="4"
			width="18"
			height="13"
			rx="2"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
		/>
		<path d="M8 21h8M12 17v4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
	</svg>
{:else if name === 'chevron'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
	</svg>
{:else if name === 'back'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<path
			d="M19 12H7M11 7l-5 5 5 5"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'es'}
	<svg viewBox="0 0 18 12" aria-hidden="true">
		<rect width="18" height="12" rx="2" fill="#c60b1e" />
		<rect y="3.2" width="18" height="5.6" fill="#ffc400" />
	</svg>
{:else if name === 'en'}
	<svg viewBox="0 0 18 12" aria-hidden="true">
		<rect width="18" height="12" fill="#012169" />
		<path d="M0 0 18 12M18 0 0 12" stroke="#fff" stroke-width="2.4" />
		<path d="M0 0 18 12M18 0 0 12" stroke="#c8102e" stroke-width="1.2" />
		<path d="M9 0v12M0 6h18" stroke="#fff" stroke-width="4" />
		<path d="M9 0v12M0 6h18" stroke="#c8102e" stroke-width="2" />
	</svg>
{:else if name === 'fr'}
	<svg viewBox="0 0 18 12" aria-hidden="true">
		<rect width="18" height="12" fill="#fff" />
		<rect width="6" height="12" fill="#0055a4" />
		<rect x="12" width="6" height="12" fill="#ef4135" />
	</svg>
{:else if name === 'poseIdle'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="5" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M12 8.2v6.2M8.2 11.2h7.6M9.4 20.2 12 14.4l2.6 5.8"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'poseWalk'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="5" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M12 8.2v5.4M8 10.4 12 12.2l4.4-2.4M8.2 20.2 12 14.4l4.2 2.2"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'poseRun'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="13.2" cy="4.8" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M12.2 8 10.4 13.4M7.2 9.6l5 2.6 5.2-3.6M7.8 20.2l2.8-6.2 6.2.8"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'poseFly'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="6.2" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M12 9.4v4.2M4.4 11.2h15.2M9.4 20.2 12 13.6l2.6 6.6"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'pause'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<path
			d="M8 6.5v11M16 6.5v11"
			fill="none"
			stroke="currentColor"
			stroke-width="2.2"
			stroke-linecap="round"
		/>
	</svg>
{:else if name === 'play'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<path
			d="M8.4 6.4v11.2L18.2 12Z"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'rotate'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<path
			d="M4.6 12A7.4 7.4 0 0 1 16.4 6.4M16.2 4.2v3.4h-3.4M19.4 12A7.4 7.4 0 0 1 7.6 17.6M7.8 19.8v-3.4h3.4"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'zoom'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="10.4" cy="10.4" r="5.4" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M14.4 14.4 20 20M10.4 8v4.8M8 10.4h4.8"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
		/>
	</svg>
{:else if name === 'cape'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="5" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M9.2 8.2c.6.8 1.6 1.2 2.8 1.2s2.2-.4 2.8-1.2c.6 2.2 2.2 8.2 2.2 10.2 0 1.8-3.2 2.6-5 2.6s-5-.8-5-2.6c0-2 1.6-8 2.2-10.2Z"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'elytra'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="5.2" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M12 8.4C8.6 8.8 5.2 11 4.2 18.2c2.2-1.4 4.8-2.8 7.8-3.2M12 8.4c3.4.4 6.8 2.6 7.8 9.8-2.2-1.4-4.8-2.8-7.8-3.2"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
			stroke-linejoin="round"
		/>
	</svg>
{:else if name === 'capeOff'}
	<svg viewBox="0 0 24 24" aria-hidden="true">
		<circle cx="12" cy="5" r="2.1" fill="none" stroke="currentColor" stroke-width="1.8" />
		<path
			d="M9.2 8.2c.6.8 1.6 1.2 2.8 1.2s2.2-.4 2.8-1.2c.6 2.2 2.2 8.2 2.2 10.2 0 1.8-3.2 2.6-5 2.6s-5-.8-5-2.6c0-2 1.6-8 2.2-10.2Z"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linejoin="round"
		/>
		<path
			d="M6 18.5 18 5.5"
			fill="none"
			stroke="currentColor"
			stroke-width="1.8"
			stroke-linecap="round"
		/>
	</svg>
{/if}

<style>
	svg {
		width: 1.05rem;
		height: 1.05rem;
		flex-shrink: 0;
		display: block;
	}

	svg[viewBox='0 0 18 12'] {
		width: 1.15rem;
		height: 0.78rem;
		border-radius: 2px;
		overflow: hidden;
	}
</style>
