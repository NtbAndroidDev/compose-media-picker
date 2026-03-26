package com.example.picker_photo.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Primary ──────────────────────────────────────────────────────────
val Primary             = Color(0xFF006E2E)   // #006e2e
val PrimaryContainer    = Color(0xFF00B14F)   // #00B14F
val PrimaryFixed        = Color(0xFF71FE91)   // #71fe91 — glow accent
val OnPrimary           = Color(0xFFFFFFFF)
val OnPrimaryContainer  = Color(0xFF002111)

// ── Surface Hierarchy ────────────────────────────────────────────────────────
val Surface                  = Color(0xFFF8F9FB)   // base layer
val SurfaceContainerLow      = Color(0xFFF2F4F6)   // secondary sectioning
val SurfaceContainerLowest   = Color(0xFFFFFFFF)   // interactive cards / lift
val SurfaceContainerHigh     = Color(0xFFE6E8EA)   // inactive / recessed
val SurfaceContainerHighest  = Color(0xFFDADCDE)

// ── On-Surface ───────────────────────────────────────────────────────────────
val OnSurface           = Color(0xFF191C1E)   // primary headlines
val OnSurfaceVariant    = Color(0xFF3D4A3D)   // secondary body text

// ── Outline ───────────────────────────────────────────────────────────────────
val OutlineVariant      = Color(0xFFBEC8BE)   // ghost border @ 15 % opacity

// ── Selection overlay ────────────────────────────────────────────────────────
val SelectionOverlay    = Color(0xFF006E2E).copy(alpha = 0.55f)