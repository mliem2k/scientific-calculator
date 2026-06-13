import { experimental_AstroContainer as AstroContainer } from 'astro/container'; // experimental_ prefix is Astro 6.x's stable container API name
import { describe, it, expect } from 'vitest';
import FeatureSection from '../components/FeatureSection.astro';

describe('FeatureSection', () => {
  it('renders headline and body', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(FeatureSection, {
      props: {
        headline: 'Faster than the rest.',
        body: 'No more pressing buttons.',
        imageSrc: '/screenshots/screenshot-1.webp',
        imageAlt: 'Calculator tap-to-edit',
        imageRight: true,
      },
    });
    expect(html).toContain('Faster than the rest.');
    expect(html).toContain('No more pressing buttons.');
  });

  it('renders image with correct alt text', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(FeatureSection, {
      props: {
        headline: 'Test',
        body: 'Body',
        imageSrc: '/screenshots/screenshot-1.webp',
        imageAlt: 'Calculator tap-to-edit',
        imageRight: false,
      },
    });
    expect(html).toContain('Calculator tap-to-edit');
    expect(html).toContain('screenshot-1.webp');
  });

  it('applies md:order-last class when imageRight is true', async () => {
    const container = await AstroContainer.create();
    const html = await container.renderToString(FeatureSection, {
      props: {
        headline: 'Test',
        body: 'Body',
        imageSrc: '/screenshots/screenshot-1.webp',
        imageAlt: 'Alt text',
        imageRight: true,
      },
    });
    expect(html).toContain('md:order-last');
  });
});
