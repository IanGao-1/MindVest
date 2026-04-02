# Design System Strategy: The Financial Precision Manifesto

## 1. Overview & Creative North Star: "The Editorial Ledger"
This design system moves away from the cluttered, "dashboard-heavy" aesthetics of traditional fintech. Our Creative North Star is **The Editorial Ledger**. We treat financial data with the reverence of a high-end luxury periodical. 

By leveraging intentional asymmetry, expansive white space, and a high-contrast red-on-white palette, we create an environment that feels both surgically precise and intellectually sophisticated. We break the "generic SaaS" mold by treating the screen as a canvas where data is the art, and the UI is the gallery. We prioritize tonal depth over structural lines, ensuring the interface feels integrated and premium rather than "constructed."

---

## 2. Colors: High-Contrast Authority
The palette is dominated by `surface` (#f9f9f9) and `primary` (#a30113). The red is not a warning; it is an anchor of authority.

### The "No-Line" Rule
**Traditional 1px borders are strictly prohibited for sectioning.** To define boundaries, you must use background color shifts. For example, a `surface-container-low` (#f3f3f3) module should sit atop a `surface` (#f9f9f9) background. The eye perceives the edge through the shift in value, creating a cleaner, more sophisticated architectural feel.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. 
- **Foundation:** `surface` (#f9f9f9) or `surface-container-lowest` (#ffffff).
- **Secondary Modules:** `surface-container` (#eeeeee).
- **Interactive Accents:** `primary-container` (#c72628) for high-impact data points.
- **Nesting Logic:** An inner card should always be "lighter" than its parent container to suggest it is "lifted" toward the user.

### The "Glass & Gradient" Rule
To prevent the high-contrast red from feeling flat, use subtle radial gradients on hero CTAs (transitioning from `primary` #a30113 to `primary_container` #c72628). For floating navigation or modals, utilize Glassmorphism: `surface` at 80% opacity with a `24px` backdrop blur.

---

## 3. Typography: The Voice of Data
We use a dual-font strategy to balance character with readability.

- **The Display & Headline Scale (Manrope):** A geometric sans-serif that feels modern and technical. Use `display-lg` (3.5rem) for total portfolio values to create an unapologetic focal point.
- **The Body & Title Scale (Work Sans):** Chosen for its exceptional legibility in dense data environments.
- **Intentional Scale:** Use `headline-sm` (1.5rem) in `primary` (#a30113) for section headers to provide a clear, rhythmic "stop" for the eye. 
- **Hierarchy via Weight:** In financial tables, use `label-md` in `secondary` (#546067) for metadata, ensuring the primary figures in `title-md` command the most attention.

---

## 4. Elevation & Depth: Tonal Layering
We do not use heavy shadows to create depth. We use light.

- **The Layering Principle:** Avoid the "floating box" look. Place a `surface-container-lowest` (#ffffff) card on a `surface-container-low` (#f3f3f3) background. This creates a soft, natural "lift."
- **Ambient Shadows:** If a component must float (e.g., a dropdown), use a shadow color tinted with the primary hue: `rgba(163, 1, 19, 0.06)` with a `32px` blur. This mimics how light interacts with a premium surface.
- **The "Ghost Border" Fallback:** If a border is required for accessibility, use `outline-variant` (#e4beba) at **15% opacity**. It should be felt, not seen.
- **Glassmorphism:** Use for floating action bars. The transparency allows `primary` accents to bleed through, maintaining a sense of place within the app.

---

## 5. Components: Precision Primitives

### Buttons
- **Primary:** Solid `primary` (#a30113) with `on-primary` (#ffffff) text. Corner radius: `md` (0.375rem). No shadow; use a subtle inset glow on hover.
- **Secondary:** `surface-container-high` (#e8e8e8) with `on-surface` (#1a1c1c) text.
- **Tertiary:** No background. `primary` text with a `2px` underline that appears only on hover.

### Cards & Data Lists
- **Rule:** Forbid divider lines.
- **Spacing:** Use `spacing-8` (1.75rem) to separate list items. 
- **The "Alternating Shift":** Instead of lines, use a subtle `surface-container-lowest` for even rows and `surface` for odd rows in large tables.

### Input Fields
- **State:** Avoid boxing the input. Use a bottom-only border in `outline` (#8f706c). 
- **Focus:** Transition the bottom border to `primary` (#a30113) and add a subtle `primary-fixed` (#ffdad6) background tint.

### Performance Gauges (Context Specific)
- Use thin, high-precision strokes. A "Loss" state uses `error` (#ba1a1a), but a "Gain" state should use `tertiary` (#00557a) rather than green to maintain the sophisticated, non-traditional color story.

---

## 6. Do’s and Don'ts

### Do
- **Do** use `spacing-20` (4.5rem) and `spacing-24` (5.5rem) for "hero" margins. Space is a luxury.
- **Do** use `primary` (#a30113) as a surgical tool. Only the most important data or action on a screen should be red.
- **Do** use `roundedness-sm` (0.125rem) for data-heavy elements to maintain a sense of "technical precision."

### Don't
- **Don't** ever use a 100% black (#000000) border or text. Use `on-surface` (#1a1c1c).
- **Don't** use standard "Success Green." In this system, growth is represented by the deep, professional `tertiary` (#00557a) or `tertiary-container` (#006e9d).
- **Don't** use drop shadows on cards. Use background tonal shifts (`surface-container` tiers).