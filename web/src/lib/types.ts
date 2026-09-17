export type PlayerSummary = {
	name: string;
	uuid: string;
};

export type PlayerStat = {
	value: number;
	display: string;
	unit: string;
	category: string;
	rank: number;
};

export type PlayerDetail = PlayerSummary & {
	championScore: number;
	championDisplay: string;
	championRank: number;
	playHours: number;
	playHoursDisplay: string;
	scorePerHour: number;
	scorePerHourDisplay: string;
	trackedPlayers: number;
	grades: Record<string, number>;
	stats: Record<string, PlayerStat>;
	vanilla?: Record<string, VanillaStat[]>;
};

export type VanillaStat = {
	id: string;
	value: number;
	display: string;
	unit: string;
	rank: number;
};

export type PlayersResponse = {
	players: PlayerSummary[];
};

export type HealthResponse = {
	ok: boolean;
	mod: string;
	version: string;
};

export type StatusResponse = {
	bind: string;
	trackedPlayers: number;
};

export type LeaderboardInfo = {
	id: string;
	title: string;
	category: string;
};

export type LeaderboardsResponse = {
	leaderboards: LeaderboardInfo[];
	vanilla?: Partial<Record<string, string[]>>;
};

export type LeaderboardEntry = {
	rank: number;
	name: string;
	uuid: string;
	value: number;
	display: string;
};

export type LeaderboardResponse = {
	id: string;
	title: string;
	category: string;
	unit: string;
	entries: LeaderboardEntry[];
};

export type Crown = {
	name: string;
	uuid: string;
	score: number;
	display: string;
} | null;

export type CrownsResponse = {
	champion: Crown;
	dedicated: Crown;
	efficient: Crown;
};

export type ApiError = {
	error: string;
	message: string;
};
