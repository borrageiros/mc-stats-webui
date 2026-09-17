<script lang="ts">
	import { pageTitle, t } from '$lib/i18n/i18n.svelte';
</script>

<svelte:head>
	<title>{pageTitle(t('nav.about'))}</title>
</svelte:head>

<h1>{t('about.heading')}</h1>
<p>{t('about.lead')}</p>

<section class="panel">
	<h2>{t('about.howHeading')}</h2>
	<p>{t('about.how')}</p>
</section>

<section class="panel">
	<h2>{t('about.categoriesHeading')}</h2>
	<ul>
		<li>{t('about.category.combat')}</li>
		<li>{t('about.category.milestones')}</li>
		<li>{t('about.category.mining')}</li>
		<li>{t('about.category.explore')}</li>
		<li>{t('about.category.produce')}</li>
		<li>{t('about.category.survive')}</li>
		<li>{t('about.category.flex')}</li>
	</ul>
</section>

<section class="panel">
	<h2>{t('about.farmsHeading')}</h2>
	<p>{t('about.farms')}</p>
</section>

<section class="panel">
	<h2>{t('about.crownsHeading')}</h2>
	<ul>
		<li>{t('about.crownChampion')}</li>
		<li>{t('about.crownDedicated')}</li>
		<li>{t('about.crownEfficient')}</li>
	</ul>
</section>

<section class="panel">
	<h2>{t('about.unitsHeading')}</h2>
	<p>{t('about.units')}</p>
</section>

<style>
	h1 {
		text-align: center;
	}

	p,
	li {
		color: var(--color-text);
		line-height: 1.45;
	}

	p {
		max-width: 42rem;
		margin-left: auto;
		margin-right: auto;
	}

	.panel {
		max-width: 42rem;
		margin: 1.1rem auto 0;
		padding: 0.85rem 1rem;
	}

	.panel p {
		margin: 0;
		max-width: none;
	}

	.panel ul {
		margin: 0;
		padding-left: 1.1rem;
	}

	.panel li + li {
		margin-top: 0.45rem;
	}

	h2 {
		margin: 0 0 0.6rem;
		font-size: 1rem;
		color: var(--color-gold);
	}
</style>
