<script lang="ts">
	import fallbackFavicon from '$lib/assets/favicon.svg';
	import LanguageSwitcher from '$lib/components/LanguageSwitcher.svelte';
	import ThemeSwitcher from '$lib/components/ThemeSwitcher.svelte';
	import { branding, loadBranding } from '$lib/branding.svelte';
	import { t } from '$lib/i18n/i18n.svelte';
	import '$lib/theme/theme.css';
	import '$lib/theme/theme.svelte';
	import { page } from '$app/state';

	let { children } = $props();

	let faviconHref = $state('/server-icon.png');

	$effect(() => {
		loadBranding();
	});

	$effect(() => {
		const image = new Image();
		image.onerror = () => {
			faviconHref = fallbackFavicon;
		};
		image.src = '/server-icon.png';
	});

	const home = $derived(page.url.pathname === '/');

	const links = $derived([
		{ href: '/', label: t('nav.home') },
		{ href: '/leaderboards', label: t('nav.leaderboards') },
		{ href: '/compare', label: t('nav.compare') },
		{ href: '/about', label: t('nav.about') }
	]);

	function active(href: string) {
		if (href === '/') {
			return page.url.pathname === '/';
		}
		return page.url.pathname === href || page.url.pathname.startsWith(`${href}/`);
	}
</script>

<svelte:head>
	<link rel="icon" href={faviconHref} />
	<title>{branding.title}</title>
	<meta name="description" content={branding.slogan} />
</svelte:head>

<div class="shell">
	<header class="top">
		{#if home}
			<section class="hero">
				<div class="title">
					<h1>{branding.title}</h1>
					<p class="splash">{branding.tagline}</p>
				</div>
				<p class="lead">{branding.slogan}</p>
			</section>
		{/if}
		<nav class="menu">
			{#each links as link}
				<a class="mc-btn" class:on={active(link.href)} href={link.href}>{link.label}</a>
			{/each}
			<LanguageSwitcher />
			<ThemeSwitcher />
		</nav>
	</header>
	<main>
		{@render children()}
	</main>
</div>

<style>
	.shell {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
	}

	.top {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
		padding: 1.5rem 1rem 1.25rem;
	}

	.hero {
		text-align: center;
	}

	.title {
		position: relative;
		display: inline-block;
		margin-bottom: 0.85rem;
	}

	.hero h1 {
		margin: 0;
		font-size: clamp(1.6rem, 4vw, 2.4rem);
		line-height: 1.1;
	}

	.splash {
		position: absolute;
		left: 78%;
		top: 120%;
		margin: 0;
		color: var(--color-gold);
		transform: rotate(-18deg);
		transform-origin: left 70%;
		font-size: 0.72rem;
		line-height: 1;
		white-space: nowrap;
		pointer-events: none;
	}

	.lead {
		margin: 0;
		color: var(--color-muted);
	}

	.menu {
		display: grid;
		grid-template-columns: repeat(2, min(20rem, calc(50vw - 1.2rem)));
		gap: 0.5rem;
		justify-content: center;
	}

	.menu :global(.mc-btn) {
		width: 100%;
	}

	main {
		width: min(52rem, calc(100% - 1.5rem));
		margin: 0 auto;
		padding: 0.85rem 0 3rem;
		flex: 1;
	}

	@media (max-width: 520px) {
		.menu {
			grid-template-columns: 1fr;
			width: min(20rem, 100%);
		}
	}
</style>
