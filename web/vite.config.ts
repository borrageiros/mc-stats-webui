import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { serverIcon } from './server-icon-plugin.js';

export default defineConfig({
	server: {
		port: 5173,
		proxy: {
			'/api': 'http://127.0.0.1:25580',
			'/mojang/session': {
				target: 'https://sessionserver.mojang.com',
				changeOrigin: true,
				secure: true,
				rewrite: (path) => path.replace(/^\/mojang/, '')
			},
			'/mojang/textures': {
				target: 'https://textures.minecraft.net',
				changeOrigin: true,
				secure: true,
				rewrite: (path) => path.replace(/^\/mojang\/textures/, '')
			}
		}
	},
	preview: {
		port: 4173
	},
	plugins: [
		serverIcon(),
		sveltekit({
			compilerOptions: {
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},
			paths: {
				relative: false
			},
			adapter: adapter({
				pages: 'build',
				assets: 'build',
				fallback: '200.html',
				precompress: false,
				strict: true
			})
		})
	]
});
