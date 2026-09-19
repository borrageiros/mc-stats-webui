<script lang="ts">
	let {
		disabled = false,
		onVisible
	}: {
		disabled?: boolean;
		onVisible: () => void;
	} = $props();

	let node = $state<HTMLElement | null>(null);

	$effect(() => {
		const target = node;
		if (!target || disabled) {
			return;
		}
		let alive = true;
		const probe = () => {
			if (!alive || disabled) {
				return;
			}
			const rect = target.getBoundingClientRect();
			if (rect.top < window.innerHeight + 240) {
				onVisible();
			}
		};
		const observer = new IntersectionObserver(
			(entries) => {
				if (entries.some((entry) => entry.isIntersecting)) {
					onVisible();
				}
			},
			{ root: null, rootMargin: '240px 0px', threshold: 0 }
		);
		observer.observe(target);
		queueMicrotask(probe);
		return () => {
			alive = false;
			observer.disconnect();
		};
	});
</script>

<div class="sentinel" bind:this={node} aria-hidden="true"></div>

<style>
	.sentinel {
		width: 100%;
		height: 1px;
		pointer-events: none;
	}
</style>
