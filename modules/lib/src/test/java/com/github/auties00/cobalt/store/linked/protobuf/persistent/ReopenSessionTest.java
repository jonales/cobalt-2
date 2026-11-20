package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Reopening a session in the same process")
class ReopenSessionTest {

    // Observed live (2026-11-20): after a client ended for good (a proxy outage), opening the same session
    // again in the same process failed with OverlappingFileLockException, because nothing closed the MVStore.
    @Test
    void aSessionCanBeOpenedAgainInTheSameProcess(@TempDir Path directory) throws Exception {
        var factory = new PersistentLinkedWhatsAppStoreFactory(directory);
        var uuid = UUID.randomUUID();
        var created = factory.create(LinkedWhatsAppClientType.WEB, uuid);
        created.await();

        var first = assertDoesNotThrow(() -> factory.load(LinkedWhatsAppClientType.WEB, uuid));
        assertTrue(first.isPresent());
        // A second factory, as a new client would build, opening the session the first one holds
        var second = assertDoesNotThrow(() -> new PersistentLinkedWhatsAppStoreFactory(directory)
                .load(LinkedWhatsAppClientType.WEB, uuid));
        assertTrue(second.isPresent());
    }
}
