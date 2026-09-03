package dev.worldgen.tectonic;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TectonicRepositorySource implements RepositorySource {
    private static final Map<String, Pack> PACKS = new LinkedHashMap<>();

    public static void register(Pack pack) {
        synchronized (PACKS) {
            PACKS.putIfAbsent(pack.getId(), pack);
        }
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> consumer) {
        List<Pack> snapshot;
        synchronized (PACKS) {
            snapshot = List.copyOf(PACKS.values());
        }
        snapshot.forEach(consumer);
    }
}
