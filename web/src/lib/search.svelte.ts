const DEFAULT_DELAY_MS = 140;

export function deferredValue<T>(read: () => T, delayMs = DEFAULT_DELAY_MS) {
	let current = $state(read());
	$effect(() => {
		const next = read();
		const timer = setTimeout(() => {
			current = next;
		}, delayMs);
		return () => clearTimeout(timer);
	});
	return {
		get current() {
			return current;
		}
	};
}

export const SEARCH_RESULT_CAP = 48;

export function cappedResults<T>(items: T[], searching: boolean, cap = SEARCH_RESULT_CAP): T[] {
	if (!searching || items.length <= cap) {
		return items;
	}
	return items.slice(0, cap);
}
