package dev.worldgen.tectonic;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TectonicRepositorySourceTest {
    @BeforeAll
    static void detectGameVersion() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void duplicatePackIdsAreRegisteredOnce() {
        String id = "tectonic/test-duplicate";
        Pack first = pack(id);

        TectonicRepositorySource.register(first);
        TectonicRepositorySource.register(pack(id));

        List<Pack> packs = discovered().stream().filter(pack -> pack.getId().equals(id)).toList();
        assertEquals(1, packs.size());
        assertSame(first, packs.get(0));
    }

    @Test
    void registrationIsSafeWhenRepeatedInParallel() {
        String id = "tectonic/test-parallel";

        IntStream.range(0, 100).parallel().forEach(index -> TectonicRepositorySource.register(pack(id)));

        assertEquals(1, discovered().stream().filter(pack -> pack.getId().equals(id)).count());
    }

    @Test
    void packsKeepRegistrationOrder() {
        String firstId = "tectonic/test-order-first";
        String secondId = "tectonic/test-order-second";
        TectonicRepositorySource.register(pack(firstId));
        TectonicRepositorySource.register(pack(secondId));

        List<String> ids = discovered().stream()
            .map(Pack::getId)
            .filter(id -> id.equals(firstId) || id.equals(secondId))
            .toList();

        assertEquals(List.of(firstId, secondId), ids);
    }

    private static List<Pack> discovered() {
        List<Pack> packs = new ArrayList<>();
        new TectonicRepositorySource().loadPacks(packs::add);
        return packs;
    }

    private static Pack pack(String id) {
        return Pack.create(
            id,
            Component.literal(id),
            false,
            ignored -> null,
            new Pack.Info(Component.literal(id), 15, FeatureFlagSet.of()),
            PackType.SERVER_DATA,
            Pack.Position.TOP,
            false,
            PackSource.BUILT_IN
        );
    }
}
