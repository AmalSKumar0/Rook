---
name: Bubbly API System
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f3'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1a1c1c'
  on-surface-variant: '#404945'
  inverse-surface: '#2f3131'
  inverse-on-surface: '#f1f1f1'
  outline: '#707974'
  outline-variant: '#bfc9c3'
  surface-tint: '#2c6956'
  primary: '#2c6956'
  on-primary: '#ffffff'
  primary-container: '#a8e6cf'
  on-primary-container: '#2c6957'
  inverse-primary: '#96d3bd'
  secondary: '#5f5b77'
  on-secondary: '#ffffff'
  secondary-container: '#e2dcfd'
  on-secondary-container: '#635f7b'
  tertiary: '#785741'
  on-tertiary: '#ffffff'
  tertiary-container: '#fdd1b4'
  on-tertiary-container: '#785841'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#b1efd8'
  primary-fixed-dim: '#96d3bd'
  on-primary-fixed: '#002118'
  on-primary-fixed-variant: '#0d503f'
  secondary-fixed: '#e5deff'
  secondary-fixed-dim: '#c8c3e3'
  on-secondary-fixed: '#1b1830'
  on-secondary-fixed-variant: '#47445e'
  tertiary-fixed: '#ffdcc5'
  tertiary-fixed-dim: '#e8bea2'
  on-tertiary-fixed: '#2c1605'
  on-tertiary-fixed-variant: '#5e402b'
  background: '#f9f9f9'
  on-background: '#1a1c1c'
  surface-variant: '#e2e2e2'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  code-label:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  button-text:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 20px
  lg: 32px
  xl: 48px
  margin-mobile: 20px
  gutter-mobile: 16px
---

## Brand & Style

The design system is built on a "Soft-Brutalist" aesthetic that transforms the typically complex world of API testing into a friendly, approachable experience. By combining high-contrast line art with a "bubbly" physical metaphor, the interface feels like a tangible workspace rather than a dry technical tool. 

The brand personality is optimistic, clear, and reassuring. It targets developers who value efficiency but prefer an environment that reduces cognitive load through distinct visual cues and generous breathing room. The emotional response is one of "playful precision"—where heavy technical tasks feel light and manageable.

Key style principles:
- **Defined Outlines:** Every interactive element is anchored by a thick, uniform black stroke.
- **Pastel Depth:** Color is used sparingly as a highlight, never overwhelming the white base.
- **Soft Geometry:** Sharp corners are avoided in favor of high-radius curves that suggest a "squishy" tactile nature.

## Colors

The palette uses a light-mode default to maintain a "clean paper" feel. The core of the system is a stark black-and-white foundation, with secondary pastels used to categorize API methods (e.g., GET, POST, DELETE) and provide visual relief.

- **Primary (Mint Green):** Used for "Success" states and primary action highlights.
- **Secondary (Pale Lavender):** Used for secondary utility actions and informational badges.
- **Tertiary (Light Peach):** Used for warnings, pending states, or attention-grabbing details.
- **Stroke (Deep Carbon):** A near-black used for all outlines and primary text to ensure high legibility and a hand-drawn feel.

## Typography

This design system utilizes **Plus Jakarta Sans** for its friendly, rounded terminals that mirror the UI's physical shapes. For technical data—such as JSON responses or endpoint paths—**JetBrains Mono** is introduced to provide a clear, functional distinction between "interface" and "data."

Headlines should use tight letter-spacing to emphasize the "bubbly" density of the letters. Body text remains airy to ensure long-form documentation is easy to digest.

## Layout & Spacing

The layout philosophy follows a **fluid grid** with significant outer margins to create a "floating" container effect. 

- **Generous Whitespace:** Elements are never cramped. The system uses a 20px base margin on mobile to ensure the thick borders don't feel overwhelming.
- **Vertical Rhythm:** A consistent 12px or 20px gap is maintained between cards to allow the "soft shadows" space to breathe.
- **Information Density:** While the vibe is playful, data-heavy views (like header lists) use a tighter "sm" spacing to maintain utility without sacrificing the rounded aesthetic.

## Elevation & Depth

This design system avoids traditional realistic shadows in favor of **Offset Tonal Shadows**. 

Depth is conveyed through:
1.  **Thick Outlines:** A 2px solid black border on all primary containers.
2.  **Directional Offset:** Instead of a centered blur, "shadows" are created by a secondary, slightly darker pastel layer or a very soft, low-opacity neutral blur shifted 4px down and 2px to the right. 
3.  **Layering:** High-priority modals and tooltips use a thicker 3px border to visually "pop" against the 2px-bordered background cards.

## Shapes

The shape language is consistently "bubbly." There are no sharp 90-degree angles in the system. 

- **Primary Cards:** Use `rounded-lg` (16px) for a friendly, chunky appearance.
- **Buttons & Inputs:** Use `rounded-xl` (24px) to create a pill-like or heavily rounded feel that invites interaction.
- **Icons:** Should be enclosed in circular or "squircle" containers with 2px outlines.

## Components

- **Buttons:** High-contrast with a 2px black border. The "Primary" button uses a Mint Green fill and a hard-offset shadow (2px down) that disappears when pressed (mimicking a physical click).
- **Cards:** White background, 2px black outline, and 16px corner radius. Include a subtle "accent tab" of color (Lavender or Peach) at the top or side to denote the API method type.
- **Input Fields:** Rounded containers with a subtle gray fill (F9F9F9). The border thickens to 3px on focus.
- **Chips/Badges:** Small, fully pill-shaped indicators for status codes (e.g., 200 OK in Mint Green).
- **Illustrative Icons:** 2px stroke weight, non-filled line art. Use "blob" shapes behind icons in a pastel color to add character.
- **Response Trays:** Large white containers with "dotted" dividers for separating JSON keys, maintaining the light, airy feel even in data-heavy sections.