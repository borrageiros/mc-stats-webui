<script lang="ts">
	import Icon from '$lib/components/Icon.svelte';
	import type { IconName } from '$lib/components/iconNames';
	import Loader from '$lib/components/Loader.svelte';
	import { t, type MessageKey } from '$lib/i18n/i18n.svelte';
	import {
		applyBodyPose,
		applyPodiumPresentation,
		bodyPoses,
		createBodyViewer,
		createPodiumViewer,
		renderHeadSkin,
		updateBodySkin,
		type BodyHandle,
		type BodyPose,
		type SkinVariant
	} from '$lib/skins/render';
	import type { BackEquipment, SkinViewer } from '@daidr/minecraft-skin-renderer';

	let {
		uuid,
		variant,
		scale = 8,
		controls = true,
		presentation = 'default'
	}: {
		uuid: string;
		variant: SkinVariant;
		scale?: number;
		controls?: boolean;
		presentation?: 'default' | 'podium';
	} = $props();

	let canvas = $state<HTMLCanvasElement | undefined>();
	let host = $state<HTMLDivElement | undefined>();
	let failed = $state(false);
	let loading = $state(true);
	let viewer = $state<SkinViewer | null>(null);
	let pose = $state<BodyPose>('run');
	let paused = $state(false);
	let autoRotate = $state(false);
	let equipment = $state<BackEquipment>('cape');

	const poseKeys: Record<BodyPose, MessageKey> = {
		idle: 'skin.pose.idle',
		walk: 'skin.pose.walk',
		run: 'skin.pose.run',
		fly: 'skin.pose.fly'
	};

	const poseIcons: Record<BodyPose, IconName> = {
		idle: 'poseIdle',
		walk: 'poseWalk',
		run: 'poseRun',
		fly: 'poseFly'
	};

	const equipmentKeys: Record<BackEquipment, MessageKey> = {
		cape: 'skin.equipment.cape',
		elytra: 'skin.equipment.elytra',
		none: 'skin.equipment.none'
	};

	const equipmentIcons: Record<BackEquipment, IconName> = {
		cape: 'cape',
		elytra: 'elytra',
		none: 'capeOff'
	};

	const equipmentOrder: BackEquipment[] = ['cape', 'elytra', 'none'];

	const showControls = $derived(variant === 'body' && presentation === 'default' && controls && viewer && !failed);

	$effect(() => {
		if (variant !== 'head') {
			return;
		}
		const node = canvas;
		const id = uuid;
		const pixelScale = scale;
		if (!node || !id) {
			return;
		}
		failed = false;
		loading = true;
		viewer = null;
		let cancelled = false;
		renderHeadSkin(node, id, pixelScale)
			.then((cleanup) => {
				if (cancelled) {
					cleanup();
					return;
				}
				loading = false;
			})
			.catch(() => {
				if (!cancelled) {
					failed = true;
					loading = false;
				}
			});
		return () => {
			cancelled = true;
		};
	});

	$effect(() => {
		if (variant === 'head') {
			return;
		}
		const root = host;
		const view = presentation;
		if (!root) {
			return;
		}
		failed = false;
		loading = true;
		viewer = null;
		pose = view === 'podium' ? 'idle' : 'run';
		paused = view === 'podium';
		autoRotate = false;
		equipment = 'none';
		let cancelled = false;
		let handle: BodyHandle | null = null;
		let queue = Promise.resolve();

		function run(task: () => Promise<void>) {
			queue = queue.then(task).catch(() => {
				if (!cancelled && !handle) {
					failed = true;
					loading = false;
				}
			});
		}

		$effect(() => {
			const id = uuid;
			if (!id) {
				return;
			}
			failed = false;
			loading = true;
			run(async () => {
				if (cancelled) {
					return;
				}
				if (handle) {
					const hasCape = await updateBodySkin(handle.viewer, id);
					if (cancelled) {
						return;
					}
					equipment = hasCape ? 'cape' : 'none';
					if (view === 'podium') {
						applyPodiumPresentation(handle.viewer);
					}
					loading = false;
					return;
				}
				const node = document.createElement('canvas');
				node.setAttribute('aria-label', t('skin.body'));
				root.replaceChildren(node);
				const created =
					view === 'podium' ? await createPodiumViewer(node, id) : await createBodyViewer(node, id);
				if (cancelled) {
					created.dispose();
					root.replaceChildren();
					return;
				}
				handle = created;
				viewer = created.viewer;
				equipment = created.hasCape ? 'cape' : 'none';
				loading = false;
			});
		});

		return () => {
			cancelled = true;
			viewer = null;
			handle?.dispose();
			root.replaceChildren();
		};
	});

	function cyclePose() {
		const next = bodyPoses[(bodyPoses.indexOf(pose) + 1) % bodyPoses.length];
		pose = next;
		paused = false;
		if (!viewer) {
			return;
		}
		applyBodyPose(viewer, next);
	}

	function togglePaused() {
		if (!viewer) {
			return;
		}
		if (paused) {
			viewer.resumeAnimation();
			paused = false;
			return;
		}
		viewer.pauseAnimation();
		paused = true;
	}

	function toggleAutoRotate() {
		autoRotate = !autoRotate;
		viewer?.setAutoRotate(autoRotate);
	}

	function cycleEquipment() {
		const next = equipmentOrder[(equipmentOrder.indexOf(equipment) + 1) % equipmentOrder.length];
		equipment = next;
		viewer?.setBackEquipment(next);
	}

	function stopOrbit(event: PointerEvent) {
		event.stopPropagation();
	}
</script>

<div class="skin" class:head={variant === 'head'} class:body={variant === 'body'} class:podium={presentation === 'podium'} class:failed class:loading>
	<div class="stage">
		{#if variant === 'head'}
			<canvas bind:this={canvas} aria-label={t('skin.head')}></canvas>
		{:else}
			<div class="viewport" bind:this={host}></div>
		{/if}
		{#if loading && !failed}
			<Loader embed />
		{/if}
		{#if failed}
			<span class="error">{t('skin.loadError')}</span>
		{/if}
		{#if showControls}
			<div class="actions" role="group" aria-label={t('skin.actions')}>
				<button
					type="button"
					title={t(poseKeys[pose])}
					aria-label={t(poseKeys[pose])}
					onpointerdown={stopOrbit}
					onclick={cyclePose}
				>
					<Icon name={poseIcons[pose]} />
				</button>
				<button
					type="button"
					class:on={paused}
					aria-pressed={paused}
					title={paused ? t('skin.resume') : t('skin.pause')}
					aria-label={paused ? t('skin.resume') : t('skin.pause')}
					onpointerdown={stopOrbit}
					onclick={togglePaused}
				>
					<Icon name={paused ? 'play' : 'pause'} />
				</button>
				<button
					type="button"
					class:on={autoRotate}
					aria-pressed={autoRotate}
					title={t('skin.autoRotate')}
					aria-label={t('skin.autoRotate')}
					onpointerdown={stopOrbit}
					onclick={toggleAutoRotate}
				>
					<Icon name="rotate" />
				</button>
				<button
					type="button"
					title={t(equipmentKeys[equipment])}
					aria-label={t(equipmentKeys[equipment])}
					onpointerdown={stopOrbit}
					onclick={cycleEquipment}
				>
					<Icon name={equipmentIcons[equipment]} />
				</button>
			</div>
		{/if}
	</div>
</div>

<style>
	.skin {
		position: relative;
		display: inline-flex;
		flex-direction: column;
		align-items: stretch;
		flex-shrink: 0;
	}

	.stage {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	canvas {
		display: block;
		image-rendering: pixelated;
	}

	.head .stage canvas {
		width: 2.5rem;
		height: 2.5rem;
	}

	.body .stage {
		width: 14rem;
		height: 20rem;
		background: var(--color-card);
		border: 2px solid var(--color-border);
		box-shadow: inset 0 0 0 2px var(--button-shadow);
		overflow: hidden;
		cursor: grab;
	}

	.body .stage:active {
		cursor: grabbing;
	}

	.viewport {
		width: 100%;
		height: 100%;
	}

	.viewport :global(canvas) {
		display: block;
		image-rendering: pixelated;
	}

	.body .viewport :global(canvas) {
		width: 100%;
		height: 100%;
	}

	.body.podium {
		width: 100%;
		height: 100%;
		overflow: visible;
	}

	.body.podium .stage {
		width: 100%;
		height: 100%;
		background: transparent;
		border: none;
		box-shadow: none;
		overflow: visible;
		cursor: default;
		align-items: flex-end;
	}

	.body.podium .stage:active {
		cursor: default;
	}

	.body.podium .viewport {
		display: flex;
		align-items: flex-end;
		overflow: visible;
	}

	.body.podium .viewport :global(canvas) {
		width: 100%;
		height: 138%;
		flex-shrink: 0;
	}

	.loading canvas,
	.loading .viewport :global(canvas),
	.failed canvas,
	.failed .viewport :global(canvas) {
		visibility: hidden;
	}

	.head .stage {
		width: 2.5rem;
		height: 2.5rem;
	}

	.error {
		position: absolute;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0.4rem;
		text-align: center;
		font-size: 0.72rem;
		color: var(--color-muted);
	}

	.actions {
		position: absolute;
		left: 0.45rem;
		bottom: 0.45rem;
		display: flex;
		gap: 0.28rem;
		z-index: 1;
	}

	.actions button {
		width: 2rem;
		height: 2rem;
		min-height: 0;
		padding: 0;
		display: grid;
		place-items: center;
	}

	.actions button:hover,
	.actions button.on {
		color: var(--color-hover);
	}

	.actions :global(svg) {
		width: 1rem;
		height: 1rem;
	}
</style>
