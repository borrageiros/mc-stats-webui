import { existsSync, createReadStream } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

export function serverIcon() {
	const files = [
		path.join(repoRoot, 'minecraft-server', 'server-icon.png'),
		path.join(repoRoot, 'server-icon.png')
	];
	return {
		name: 'server-icon',
		configureServer(server) {
			server.middlewares.use((req, res, next) => {
				const url = req.url ?? '';
				if (url.split('?')[0] !== '/server-icon.png') {
					next();
					return;
				}
				const file = files.find((candidate) => existsSync(candidate));
				if (!file) {
					next();
					return;
				}
				res.setHeader('Content-Type', 'image/png');
				res.setHeader('Cache-Control', 'no-cache');
				createReadStream(file).pipe(res);
			});
		}
	};
}
