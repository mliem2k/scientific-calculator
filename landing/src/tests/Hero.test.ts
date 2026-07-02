import { experimental_AstroContainer as AstroContainer } from 'astro/container';
import { describe, it, expect } from 'vitest';
import Hero from '../components/Hero.astro';

describe('Hero', () => {
  it('renders headline', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Hero);
    expect(html).toContain('Math the way');
  });

  it('renders subheadline', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Hero);
    expect(html).toContain('Natural textbook notation');
  });

  it('renders download CTA and GitHub link', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Hero);
    expect(html).toContain('Download APK');
    expect(html).toContain('github.com/mliem2k/scientific-calculator');
  });
});
