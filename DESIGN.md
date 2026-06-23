# Design

## Theme

IsPlayer uses a restrained light product UI with a cinematic blue accent, tinted neutral surfaces, and compact Material 3 components. The interface should feel mature and calm rather than playful or decorative.

## Color

- `primary`: deep cinematic blue for selected state, primary actions, progress, and focus.
- `secondary`: muted slate-blue for secondary emphasis.
- `background`: lightly tinted cool neutral, never pure white.
- `surface`: warm-neutral white for cards and sheets.
- `surfaceVariant`: subtle blue-gray for controls, chips, search, and dividers.
- `tertiary`: soft amber used sparingly for media-quality badges.
- `error`: muted red for recoverable errors.

## Typography

Use the platform sans-serif with a tighter Material 3 scale. Headings are semibold, titles are medium to semibold, metadata uses label styles, and long video titles clamp cleanly.

## Shape

Use moderate radii: 8dp for small badges and thumbnails, 12dp for cards and inputs, 16dp for large surfaces, and 20dp only for prominent sheets or search containers.

## Components

- Video cards use surface layers, thumbnail contrast, and small metadata chips instead of heavy elevation.
- Search and icon controls share the same 48dp touch target and surface container vocabulary.
- Folder filters use selected containers so the active library is visible at a glance.
- Drawer sections are calm grouped surfaces with clear selected folder feedback.
- Player overlays use translucent black surfaces, readable labels, and compact controls.

## Motion

Motion is limited to short pressed feedback, playlist slide-in, and controls fade. It should communicate state, not decorate the app.
