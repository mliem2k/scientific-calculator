# Landing Page Design

**Date:** 2026-06-13
**Scope:** Static Astro landing page for the Scientific Calculator Android app, deployed from `landing/` to Vercel.

---

## 1. Stack

- **Framework:** Astro (static output, `output: 'static'`)
- **Styles:** Tailwind CSS via `@astrojs/tailwind`
- **Package manager:** Bun (`bun.lockb`)
- **Deployment:** Vercel, Root Directory = `landing`

No client-side JavaScript, no component library, no external fonts.

---

## 2. Repository Structure

```
landing/
├── src/
│   ├── pages/
│   │   └── index.astro
│   ├── components/
│   │   ├── Hero.astro
│   │   ├── FeatureSection.astro
│   │   └── Footer.astro
│   └── assets/
│       └── screenshots/
│           ├── screenshot-1.webp
│           ├── screenshot-3.webp
│           ├── screenshot-4.webp
│           └── screenshot-5.webp
├── public/
├── astro.config.mjs
├── package.json
├── bun.lockb
└── vercel.json
```

Screenshots are copied from `../reference-photos/` at scaffold time. screenshot-2.webp is excluded (shows a different app's UI).

---

## 3. Page Sections

### Hero
- Headline: **Scientific Calculator**
- Subhead: "Natural textbook display. For Android."
- CTA button: "Download APK" → `https://github.com/mliem2k/scientific-calculator/releases/latest`
- No screenshot in hero; full-width black panel

### Feature sections (4 total, alternating image side)

| # | Screenshot | Headline | Body | Image side |
|---|---|---|---|---|
| 1 | screenshot-1.webp | Faster than the rest. | No more pressing buttons. Tap to edit. Pinch to zoom. Fly through long equations. | right |
| 2 | screenshot-3.webp | Extreme Battery Life. | You use your calculator a lot. Use it 2x longer with our AMOLED theme. | left |
| 3 | screenshot-4.webp | Share and discuss. | Share equations with Favorites and History. Share them with friends or classmates. | right |
| 4 | screenshot-5.webp | Always with you. | Leave your bulky scientific calculator at home. Always in the palm of your hand. | left |

All copy lifted verbatim from the reference photos.

### Footer
- Text: "Scientific Calculator · [GitHub](https://github.com/mliem2k/scientific-calculator) · MIT"

---

## 4. Design Tokens

| Token | Value |
|---|---|
| Background | `#000000` |
| Headline text | `#FFFFFF` |
| Body text | `#A1A1AA` (zinc-400) |
| CTA bg | `#FFFFFF` |
| CTA text | `#000000` |
| Section divider | `#27272A` (zinc-800) |

---

## 5. Typography

| Element | Tailwind class |
|---|---|
| Hero title | `text-5xl font-bold` |
| Section h2 | `text-4xl font-bold` |
| Body | `text-lg leading-relaxed` |
| Font stack | system-ui (no download) |

---

## 6. Layout

- **Mobile:** single column, screenshot stacked above text
- **≥ md (768px):** two-column grid, image and text side-by-side, alternating per section
- Screenshot images: `max-w-[280px]`, vertically centered, `object-contain`

---

## 7. FeatureSection Component API

```astro
<FeatureSection
  headline="Faster than the rest."
  body="No more pressing buttons..."
  imageSrc={screenshot1}
  imageAlt="Calculator showing tap-to-edit feature"
  imageRight={true}
/>
```

`imageRight` controls which side the phone image appears on desktop. On mobile, image is always on top.

---

## 8. Vercel Configuration

`landing/vercel.json`:
```json
{ "framework": "astro" }
```

Vercel dashboard settings:
- **Root Directory:** `landing`
- **Build Command:** `bun run build`
- **Install Command:** `bun install`
- **Output Directory:** `dist`

---

## 9. Out of Scope

- Dark/light mode toggle
- i18n
- Analytics
- Contact form
- Google Play badge (no Play Store listing yet)
- Any JavaScript interactivity
