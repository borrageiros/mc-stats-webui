import type {
	AdvancementDetailResponse,
	AdvancementsCatalogResponse,
	CrownsResponse,
	HealthResponse,
	LeaderboardBoard,
	LeaderboardEntry,
	LeaderboardResponse,
	LeaderboardsResponse,
	PlayerDetail,
	PlayerStat,
	PlayerSummary,
	PlayersResponse,
	StatusResponse
} from './types';
import { parseVanillaBoardId } from './vanillaStats';

async function getJson<T>(path: string): Promise<T> {
	const response = await fetch(path);
	const contentType = response.headers.get('content-type') ?? '';
	if (!contentType.includes('application/json') || !response.ok) {
		throw new Error('unavailable');
	}
	return response.json() as Promise<T>;
}

export function getHealth() {
	return getJson<HealthResponse>('/api/health');
}

export function getStatus() {
	return getJson<StatusResponse>('/api/status');
}

export function getPlayers() {
	return getJson<PlayersResponse>('/api/players');
}

export async function getRoster(): Promise<PlayerSummary[]> {
	const data = await getPlayers();
	if (data.players.some((player) => typeof player.playHours === 'number')) {
		return sortRoster(data.players);
	}
	const [hours, score, rate, most, catalog] = await Promise.all([
		getLeaderboard('dedicated').catch(() => null),
		getLeaderboard('champion').catch(() => null),
		getLeaderboard('efficient').catch(() => null),
		getAdvancementMost().catch(() => null),
		getAdvancements().catch(() => null)
	]);
	const hoursBy = indexEntries(hours);
	const scoreBy = indexEntries(score);
	const rateBy = indexEntries(rate);
	const mostBy = indexEntries(most);
	return sortRoster(
		data.players.map((player) => {
			const time = hoursBy.get(player.uuid);
			const crown = scoreBy.get(player.uuid);
			const efficiency = rateBy.get(player.uuid);
			const done = mostBy.get(player.uuid);
			return {
				...player,
				playHours: time?.value ?? 0,
				playHoursDisplay: time?.display ?? '0 h',
				championScore: crown?.value ?? 0,
				championDisplay: crown?.display ?? '0',
				scorePerHour: efficiency?.value ?? 0,
				scorePerHourDisplay: efficiency?.display ?? '0/h',
				advancements: {
					done: done?.value ?? 0,
					total: catalog?.total ?? 0
				}
			};
		})
	);
}

function indexEntries(board: LeaderboardResponse | null) {
	const map = new Map<string, LeaderboardEntry>();
	for (const entry of board?.entries ?? []) {
		map.set(entry.uuid, entry);
	}
	return map;
}

function sortRoster(players: PlayerSummary[]) {
	return [...players].sort(
		(left, right) =>
			(right.playHours ?? 0) - (left.playHours ?? 0) ||
			left.name.localeCompare(right.name, undefined, { sensitivity: 'base' })
	);
}

export function getPlayer(name: string) {
	return getJson<PlayerSummary & Partial<PlayerDetail>>(`/api/players/${encodeURIComponent(name)}`);
}

export async function getPlayerProfile(name: string): Promise<PlayerDetail> {
	const player = await getPlayer(name);
	if (player.stats) {
		return player as PlayerDetail;
	}
	const catalog = await getLeaderboards();
	const boards = await Promise.all(
		catalog.boards.map((board) => getLeaderboard(board.id).catch(() => null))
	);
	const stats: Record<string, PlayerStat> = {};
	let championScore = 0;
	let championDisplay = '0';
	let championRank = catalog.boards.length;
	let playHours = 0;
	let playHoursDisplay = '0 h';
	let scorePerHour = 0;
	let scorePerHourDisplay = '0/h';
	let trackedPlayers = 0;
	for (const board of boards) {
		if (!board) {
			continue;
		}
		trackedPlayers = Math.max(trackedPlayers, board.entries.length);
		const entry = board.entries.find(
			(row) => row.name.toLowerCase() === player.name.toLowerCase() || row.uuid === player.uuid
		);
		const value = entry?.value ?? 0;
		const display = entry?.display ?? '0';
		const rank = entry?.rank ?? Math.max(board.entries.length, 1);
		stats[board.id] = {
			value,
			display,
			unit: board.unit,
			category: board.category,
			rank
		};
		if (board.id === 'champion' && entry) {
			championScore = value;
			championDisplay = display;
			championRank = rank;
		}
		if (board.id === 'dedicated' && entry) {
			playHours = value;
			playHoursDisplay = display;
		}
		if (board.id === 'efficient' && entry) {
			scorePerHour = value;
			scorePerHourDisplay = display;
		}
	}
	return {
		name: player.name,
		uuid: player.uuid,
		championScore,
		championDisplay,
		championRank,
		playHours,
		playHoursDisplay,
		scorePerHour,
		scorePerHourDisplay,
		trackedPlayers,
		grades: {},
		stats,
		vanilla: undefined,
		advancements: player.advancements
	};
}

export async function getLeaderboards() {
	const data = await getJson<LeaderboardsResponse & { leaderboards?: Array<{ id: string; category: string }> }>(
		'/api/leaderboards'
	);
	if (data.categories && data.boards) {
		return data;
	}
	const legacy = data.leaderboards ?? [];
	const boards: LeaderboardBoard[] = legacy.map((board) => ({
		id: board.id,
		category: board.category,
		unit: '',
		icon: '',
		listed: board.category !== 'crowns',
		compare: board.id !== 'advancements' && board.id !== 'play-time',
		lowerWins: board.category === 'deaths'
	}));
	const seen = new Set<string>();
	const categories = [];
	for (const board of boards) {
		if (!board.listed || seen.has(board.category)) {
			continue;
		}
		seen.add(board.category);
		categories.push({
			id: board.category,
			icon: '',
			boards: boards.filter((item) => item.listed && item.category === board.category)
		});
	}
	return { categories, boards, vanilla: data.vanilla };
}

export function getLeaderboard(id: string) {
	const path = id
		.split('/')
		.map((part) => encodeURIComponent(part))
		.join('/');
	return getJson<LeaderboardResponse>(`/api/leaderboards/${path}`).catch(async (error) => {
		const vanilla = parseVanillaBoardId(id);
		if (!vanilla) {
			throw error;
		}
		return leaderboardFromProfiles(vanilla.group, vanilla.id);
	});
}

async function leaderboardFromProfiles(group: string, statId: string): Promise<LeaderboardResponse> {
	const list = await getPlayers();
	const profiles = await Promise.all(list.players.map((player) => getPlayerProfile(player.name)));
	const ranked = profiles
		.map((player) => {
			const stat = player.vanilla?.[group]?.find((row) => row.id === statId);
			return {
				name: player.name,
				uuid: player.uuid,
				value: stat?.value ?? 0,
				display: stat?.display ?? '0',
				unit: stat?.unit ?? 'count'
			};
		})
		.filter((entry) => entry.value > 0)
		.sort((left, right) => right.value - left.value || left.name.localeCompare(right.name));
	return {
		id: `${group}/${statId}`,
		title: statId,
		category: group,
		unit: ranked[0]?.unit ?? 'count',
		icon: '',
		entries: ranked.map((entry, index) => ({
			rank: index + 1,
			name: entry.name,
			uuid: entry.uuid,
			value: entry.value,
			display: entry.display
		}))
	};
}

export function getCrowns() {
	return getJson<CrownsResponse>('/api/crowns');
}

export function getAdvancements() {
	return getJson<AdvancementsCatalogResponse>('/api/advancements');
}

export function getAdvancementMost() {
	return getJson<LeaderboardResponse>('/api/advancements/most');
}

export function getAdvancement(id: string) {
	const path = id
		.replace(':', '/')
		.split('/')
		.map((part) => encodeURIComponent(part))
		.join('/');
	return getJson<AdvancementDetailResponse>(`/api/advancements/${path}`);
}
