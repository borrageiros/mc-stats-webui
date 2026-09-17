import type {
	CrownsResponse,
	HealthResponse,
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
		catalog.leaderboards.map((board) => getLeaderboard(board.id).catch(() => null))
	);
	const stats: Record<string, PlayerStat> = {};
	let championScore = 0;
	let championDisplay = '0';
	let championRank = catalog.leaderboards.length;
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
		vanilla: undefined
	};
}

export function getLeaderboards() {
	return getJson<LeaderboardsResponse>('/api/leaderboards');
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
