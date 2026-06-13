import { experimental_AstroContainer as AstroContainer } from 'astro/container'; // experimental_ prefix is Astro 6.x's stable container API name
import { describe, it, expect } from 'vitest';
import Hero from '../components/Hero.astro';

describe('Hero', () => {
  it('renders app name', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Hero);
    expect(html).toContain('Scientific Calculator');
  });

  it('renders subheadline', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Hero);
    expect(html).toContain('Natural textbook display');
  });

  it('renders download CTA linking to GitHub releases', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Hero);
    expect(html).toContain('Download APK');
    expect(html).toContain('github.com/mliem2k/scientific-calculator/releases/latest');
  });
});
