package com.cinecraft.director;

/** Editorial metadata used to score cuts and offset aim from dead-center framing. */
public record ShotComposition(
        Framing framing,
        ScreenPlacement placement,
        int movementDirection,
        EntityAction action,
        boolean rackFocus
) { }
