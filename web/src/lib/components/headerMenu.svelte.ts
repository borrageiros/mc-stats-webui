export const headerMenu = $state({
	open: null as string | null
});

const roots = new Set<HTMLElement>();

export function registerMenuRoot(node: HTMLElement) {
	roots.add(node);
	return {
		destroy() {
			roots.delete(node);
		}
	};
}

export function toggleMenu(id: string) {
	headerMenu.open = headerMenu.open === id ? null : id;
}

export function closeMenu() {
	headerMenu.open = null;
}

function isInsideMenu(target: EventTarget | null) {
	if (!(target instanceof Node)) {
		return false;
	}
	for (const root of roots) {
		if (root.contains(target)) {
			return true;
		}
	}
	return false;
}

if (typeof document !== 'undefined') {
	document.addEventListener('pointerdown', (event) => {
		if (!isInsideMenu(event.target)) {
			closeMenu();
		}
	});
	document.addEventListener('keydown', (event) => {
		if (event.key === 'Escape') {
			closeMenu();
		}
	});
}
