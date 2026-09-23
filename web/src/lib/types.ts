export type PlayerSummary = {
	name: string;
	uuid: string;
	playHours?: number;
	playHoursDisplay?: string;
	championScore?: number;
	championDisplay?: string;
	scorePerHour?: number;
	scorePerHourDisplay?: string;
	advancements?: {
		done: number;
		total: number;
		rank?: number;
		ids?: string[];
	};
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
	advancements?: {
		done: number;
		total: number;
		rank: number;
		ids: string[];
	};
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
	branding?: {
		title: string;
		slogan: string;
		tagline: string;
		windowTitle: string;
	};
};

export type LeaderboardBoard = {
	id: string;
	category: string;
	unit: string;
	icon: string;
	listed: boolean;
	compare: boolean;
	lowerWins: boolean;
};

export type LeaderboardCategory = {
	id: string;
	icon: string;
	boards: LeaderboardBoard[];
};

export type LeaderboardsResponse = {
	categories: LeaderboardCategory[];
	boards: LeaderboardBoard[];
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
	icon?: string;
	lowerWins?: boolean;
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

export type AdvancementInfo = {
	id: string;
	tab: string;
	title: string;
	description: string;
	icon: string;
	frame: string;
	hidden: boolean;
	holders: number;
};

export type AdvancementTab = {
	id: string;
	title: string;
	icon: string;
};

export type AdvancementsCatalogResponse = {
	total: number;
	tabs: AdvancementTab[];
	advancements: AdvancementInfo[];
};

export type AdvancementDetailResponse = AdvancementInfo & {
	entries: LeaderboardEntry[];
};

export type ApiError = {
	error: string;
	message: string;
};
