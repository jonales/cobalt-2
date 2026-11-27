package com.github.auties00.cobalt.telemetry.metrics;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cobalt JFR metrics events")
class CobaltJfrMetricsTest {

    @Test
    @DisplayName("records connection, stanza, message, and media transfer events with expected metadata")
    void recordsAllCobaltMetricsEvents(@TempDir Path tempDir) throws IOException {
        var dumpFile = tempDir.resolve("cobalt-metrics.jfr");
        try (var recording = new Recording()) {
            recording.enable(CobaltConnectionEvent.class);
            recording.enable(CobaltStanzaEvent.class);
            recording.enable(CobaltMessageEvent.class);
            recording.enable(CobaltMediaTransferEvent.class);
            recording.start();

            var conn = new CobaltConnectionEvent();
            conn.begin();
            conn.transport = "linked";
            conn.action = "connect";
            conn.reason = "initial";
            conn.success = true;
            conn.commit();

            var stanza = new CobaltStanzaEvent();
            stanza.begin();
            stanza.tag = "iq";
            stanza.namespace = "usync";
            stanza.direction = "outbound";
            stanza.outcome = "ok";
            stanza.commit();

            var msg = new CobaltMessageEvent();
            msg.begin();
            msg.direction = "outbound";
            msg.chatKind = "direct";
            msg.encryption = "msg";
            msg.outcome = "ok";
            msg.commit();

            var media = new CobaltMediaTransferEvent();
            media.begin();
            media.operation = "upload";
            media.mediaType = "image";
            media.bytes = 4096L;
            media.success = true;
            media.commit();

            recording.stop();
            recording.dump(dumpFile);
        }

        List<RecordedEvent> events = RecordingFile.readAllEvents(dumpFile);
        assertEquals(4, events.size());

        assertTrue(events.stream().anyMatch(e ->
                "cobalt.Connection".equals(e.getEventType().getName())
                        && "linked".equals(e.getString("transport"))
                        && "connect".equals(e.getString("action"))
                        && e.getBoolean("success")));
        assertTrue(events.stream().anyMatch(e ->
                "cobalt.Stanza".equals(e.getEventType().getName())
                        && "iq".equals(e.getString("tag"))
                        && "usync".equals(e.getString("namespace"))));
        assertTrue(events.stream().anyMatch(e ->
                "cobalt.Message".equals(e.getEventType().getName())
                        && "msg".equals(e.getString("encryption"))));
        assertTrue(events.stream().anyMatch(e ->
                "cobalt.MediaTransfer".equals(e.getEventType().getName())
                        && e.getLong("bytes") == 4096L
                        && e.getBoolean("success")));
    }
}
