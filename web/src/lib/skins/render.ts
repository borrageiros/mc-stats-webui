import {
	createSkinViewer,
	use,
	type BackEquipment,
	type SkinViewer
} from '@daidr/minecraft-skin-renderer';
import { renderAvatar } from '@daidr/minecraft-skin-renderer/canvas2d';
import { WebGLRendererPlugin } from '@daidr/minecraft-skin-renderer/webgl';
import { getPlayerTextures } from './mojang';

let pluginsReady = false;

function ensurePlugins() {
	if (pluginsReady) {
		return;
	}
	use(WebGLRendererPlugin);
	pluginsReady = true;
}

export type SkinVariant = 'head' | 'body';

export const bodyPoses = ['idle', 'walk', 'run', 'fly'] as const;

export type BodyPose = (typeof bodyPoses)[number];

export const defaultBodyTheta = 0.55;
export const defaultBodyPhi = Math.PI / 2;
export const defaultBodyZoom = 48;
export const podiumTheta = -0.58;
export const podiumPhi = 1.42;
export const podiumZoom = 25;
export const podiumFov = 90;

export type BodyHandle = {
	viewer: SkinViewer;
	hasCape: boolean;
	dispose: () => void;
};

const walkSpeed = 0.4;

export function applyBodyPose(viewer: SkinViewer, pose: BodyPose) {
	viewer.playAnimation(pose, pose === 'walk' ? { speed: walkSpeed } : undefined);
}

export function resetBodyCamera(viewer: SkinViewer) {
	viewer.setAutoRotate(false);
	viewer.setRotation(defaultBodyTheta, defaultBodyPhi);
	viewer.setZoom(defaultBodyZoom);
}

export async function renderHeadSkin(
	canvas: HTMLCanvasElement,
	uuid: string,
	scale = 8
): Promise<() => void> {
	const textures = await getPlayerTextures(uuid);
	await renderAvatar(canvas, {
		skin: textures.skinUrl,
		slim: textures.slim,
		scale,
		overlayInflated: true
	});
	return () => undefined;
}

export async function createBodyViewer(canvas: HTMLCanvasElement, uuid: string): Promise<BodyHandle> {
	const textures = await getPlayerTextures(uuid);
	ensurePlugins();
	const width = Math.max(canvas.clientWidth, 160);
	const height = Math.max(canvas.clientHeight, 240);
	canvas.width = width;
	canvas.height = height;
	const viewer = await createSkinViewer({
		canvas,
		skin: textures.skinUrl,
		cape: textures.capeUrl ?? undefined,
		backEquipment: textures.capeUrl ? 'cape' : 'none',
		slim: textures.slim,
		enableRotate: true,
		enableZoom: false,
		autoRotate: false,
		zoom: defaultBodyZoom
	});
	resetBodyCamera(viewer);
	applyBodyPose(viewer, 'run');
	viewer.startRenderLoop();
	return {
		viewer,
		hasCape: Boolean(textures.capeUrl),
		dispose: () => viewer.dispose()
	};
}

export async function createPodiumViewer(canvas: HTMLCanvasElement, uuid: string): Promise<BodyHandle> {
	const textures = await getPlayerTextures(uuid);
	ensurePlugins();
	await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
	const width = Math.max(Math.round(canvas.clientWidth), 1);
	const height = Math.max(Math.round(canvas.clientHeight), 1);
	canvas.width = width;
	canvas.height = height;
	const viewer = await createSkinViewer({
		canvas,
		skin: textures.skinUrl,
		cape: textures.capeUrl ?? undefined,
		backEquipment: textures.capeUrl ? 'cape' : 'none',
		slim: textures.slim,
		enableRotate: false,
		enableZoom: false,
		autoRotate: false,
		zoom: podiumZoom,
		fov: podiumFov
	});
	viewer.setPartVisibility('leftLeg', 'both', false);
	viewer.setPartVisibility('rightLeg', 'both', false);
	viewer.setRotation(podiumTheta, podiumPhi);
	viewer.setZoom(podiumZoom);
	applyBodyPose(viewer, 'idle');
	viewer.startRenderLoop();
	requestAnimationFrame(() => {
		const nextWidth = Math.max(Math.round(canvas.clientWidth), width);
		const nextHeight = Math.max(Math.round(canvas.clientHeight), height);
		canvas.width = nextWidth;
		canvas.height = nextHeight;
		viewer.pauseAnimation();
		viewer.resize(nextWidth, nextHeight);
		viewer.render();
	});
	return {
		viewer,
		hasCape: Boolean(textures.capeUrl),
		dispose: () => viewer.dispose()
	};
}

export async function updateBodySkin(viewer: SkinViewer, uuid: string): Promise<boolean> {
	const textures = await getPlayerTextures(uuid);
	await viewer.setSkin(textures.skinUrl);
	await viewer.setCape(textures.capeUrl);
	viewer.setSlim(textures.slim);
	viewer.setBackEquipment(textures.capeUrl ? 'cape' : 'none');
	return Boolean(textures.capeUrl);
}

export function applyPodiumPresentation(viewer: SkinViewer) {
	viewer.setPartVisibility('leftLeg', 'both', false);
	viewer.setPartVisibility('rightLeg', 'both', false);
	viewer.setRotation(podiumTheta, podiumPhi);
	viewer.setZoom(podiumZoom);
	applyBodyPose(viewer, 'idle');
	viewer.pauseAnimation();
	viewer.render();
}

export type { BackEquipment };
