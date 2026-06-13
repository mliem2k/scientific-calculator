import { experimental_AstroContainer as AstroContainer } from 'astro/container'; // experimental_ prefix is Astro 6.x's stable container API name
import { describe, it, expect } from 'vitest';
import Footer from '../components/Footer.astro';

describe('Footer', () => {
  it('renders app name', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Footer);
    expect(html).toContain('Scientific Calculator');
  });

  it('renders GitHub link', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Footer);
    expect(html).toContain('github.com/mliem2k/scientific-calculator');
  });

  it('renders MIT license text', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(Footer);
    expect(html).toContain('MIT');
  });
});
