# Design System Specification

## 1. Overview & Creative North Star: "The Fluid Navigator"
This design system moves beyond the functional utility of a transport app into the realm of a premium lifestyle concierge. Our Creative North Star is **The Fluid Navigator**. We reject the rigid, "boxed-in" layout of traditional utility apps in favor of an editorial, layered experience that feels as seamless as a clear road.

By utilizing intentional asymmetry, expansive negative space, and a sophisticated "tonal stacking" approach, we create an interface that feels lightweight yet authoritative. The goal is to guide the user’s eye through a narrative flow, where the most important actions feel naturally elevated rather than forced.

---

## 2. Colors: Tonal Depth & The "No-Line" Rule
The palette is rooted in our signature `primary` (#006e2e) and `primary_container` (#00B14F), balanced by a sophisticated range of neutral surfaces.

### The "No-Line" Rule
To achieve a high-end, bespoke feel, **1px solid borders are strictly prohibited for sectioning.** Physical boundaries must be defined solely through background color shifts. For example, a card or section should be distinguished by placing a `surface_container_lowest` (#ffffff) element against a `surface_container_low` (#f2f4f6) background.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. Use the surface-container tiers to create depth:
- **Base Layer:** `surface` (#f8f9fb).
- **Secondary Sectioning:** `surface_container_low` (#f2f4f6).
- **Interactive Components/Cards:** `surface_container_lowest` (#ffffff) to provide a subtle "lift."
- **Prominence:** Use `surface_container_high` (#e6e8ea) for inactive or recessed states.

### The "Glass & Gradient" Rule
Flatness is the enemy of premium design. 
- **Signature Textures:** For hero sections and primary CTAs, use a subtle linear gradient from `primary` (#006e2e) to `primary_container` (#00B14F) at a 135-degree angle.
- **Glassmorphism:** Floating navigation bars or modal headers must use a semi-transparent `surface_container_lowest` with a `backdrop-blur` of 20px–40px. This ensures the app feels integrated and "airy."

---

## 3. Typography: Editorial Authority
We use **Inter** as our typographic backbone, treated with editorial weight to establish clear hierarchy.

| Role | Token | Size | Weight | Intent |
| :--- | :--- | :--- | :--- | :--- |
| **Display** | `display-lg` | 3.5rem | 700 | High-impact marketing and hero moments. |
| **Headline** | `headline-md` | 1.75rem | 600 | Key section headers; provides structural anchors. |
| **Title** | `title-lg` | 1.375rem | 600 | Card titles and primary navigation labels. |
| **Body** | `body-md` | 0.875rem | 400 | Standard reading text; optimized for legibility. |
| **Label** | `label-md` | 0.75rem | 500 | Micro-copy and secondary metadata. |

**Hierarchy Note:** Use `on_surface_variant` (#3d4a3d) for secondary body text to reduce visual noise, reserving `on_surface` (#191c1e) for primary headlines.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are often a crutch for poor layout. In this design system, hierarchy is achieved through **Tonal Layering**.

- **The Layering Principle:** Place a `surface_container_lowest` card on a `surface_container_low` background to create a "soft lift." This mimics natural light hitting high-quality paper.
- **Ambient Shadows:** When a floating element (like a bottom sheet) requires a shadow, use a hyper-diffused style: `Y: 12px, Blur: 48px, Color: on_surface (opacity 4%)`. Never use pure black for shadows; always tint the shadow with the `on_surface` token.
- **The "Ghost Border" Fallback:** If accessibility requirements demand a border, use the `outline_variant` token at **15% opacity**. It should be felt, not seen.
- **Nesting:** Never stack more than three levels of surface containers. Too much nesting creates visual "mush."

---

## 5. Components: Precision & Softness

### Buttons
- **Primary:** Background uses the signature `primary` to `primary_container` gradient. Border-radius: `full` (9999px) or `xl` (3rem). 
- **Secondary:** `surface_container_high` background with `on_surface` text. No border.
- **Tertiary:** Transparent background, `primary` text. Use for low-emphasis actions.

### Input Fields
- **Container:** Use `surface_container_low` with a `none` border. 
- **Interaction:** On focus, the container shifts to `surface_container_lowest` with a 1px "Ghost Border" using `primary`.
- **Labels:** Floating labels using `label-md` for maximum space efficiency.

### Chips
- **Selection:** Unselected chips use `surface_container_high`. Selected chips use `primary` with `on_primary` text.
- **Shape:** Always `full` roundedness.

### Cards & Lists: The "No-Divider" Mandate
**Explicit Instruction:** Do not use horizontal lines to separate list items or card sections. 
- Use **Spacing Scale 4 (1rem)** or **6 (1.5rem)** to create "white space dividers."
- For complex lists, use a subtle background shift (`surface_container_low` vs `surface_container_lowest`) to group related items.

### Signature Component: The Motion-Track Card
A unique component for this system: A card that uses a `primary_fixed` (#71fe91) glow on its left edge to indicate active status or "moving" progress, replacing standard progress bars with a more integrated, high-end visual cue.

---

## 6. Do's and Don'ts

### Do
- **Do** use `primary_container` (#00B14F) as your primary action color to ensure brand recognition.
- **Do** leverage the Spacing Scale religiously. Consistent gaps of `8 (2rem)` between sections create the "Editorial" feel.
- **Do** ensure all interactive elements have a minimum touch target of 44x44dp for accessibility.
- **Do** use asymmetric layouts for hero images to break the "template" feel.

### Don't
- **Don't** use 1px solid lines to separate content. 
- **Don't** use "Drop Shadows" with high opacity or small blur radii. 
- **Don't** use `on_background` (#191c1e) for long-form body text; use `on_surface_variant` (#3d4a3d) to improve reading comfort.
- **Don't** crowd the interface. If in doubt, increase the spacing by one level on the scale.