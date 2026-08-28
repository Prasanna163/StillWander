package com.cinecraft.director;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneFixtureCatalogTest {
    @Test
    void loadsTheFixedVisualMatrix() {
        SceneFixtureCatalog catalog = SceneFixtureCatalog.load();
        assertEquals(8, catalog.scenes().size());
        assertEquals(
                Set.of("open_plains", "dense_forest", "small_interior", "cave",
                        "ocean_coast", "village_build", "nether", "moving_entity"),
                catalog.scenes().stream().map(SceneFixtureCatalog.SceneFixture::name).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void fixtureExpectationsMatchEnvironmentRules() {
        for (SceneFixtureCatalog.SceneFixture fixture : SceneFixtureCatalog.load().scenes()) {
            assertEquals(fixture.expectedWide(), fixture.profile().supportsWideShots(), fixture.name());
            assertEquals(3, fixture.profile().landscapeAnchors().size(), fixture.name());
            if (fixture.underground()) assertFalse(fixture.profile().supportsWideShots(), fixture.name());
            else assertTrue(fixture.profile().maxCameraDistance() > 0.0, fixture.name());
        }
    }
}
