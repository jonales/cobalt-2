package com.github.auties00.cobalt.media.transcode.video;

import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessageBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vídeo comum (H.264 baseline com AAC, 160x120, 1 s) passa pelo transcodificador. Antes, três
 * falhas em sequência impediam qualquer envio de vídeo: o perfil e o nível iam por nome ao
 * libopenh264 ("Invalid argument"), o {@code +faststart} não achava a saída para reabrir ("No such
 * file or directory") e a miniatura saía sem o cabeçalho JFIF ("Missing magic bytes marker").
 */
class VideoPipelineEncoderTest {
    private static final Path FIXTURE = Path.of("src/test/resources/fixtures/media/video-160x120.mp4");

    @Test
    @DisplayName("vídeo H.264 com áudio vira MP4 para o WhatsApp")
    void videoTranscodesWithTheBaselineProfile() throws Exception {
        var video = new VideoMessageBuilder().mimetype("video/mp4").build();
        MediaPayload payload = null;
        try (var channel = Files.newByteChannel(FIXTURE, StandardOpenOption.READ)) {
            payload = new VideoPipeline().run(video, channel, SettingsSyncAction.MediaQualitySetting.STANDARD);
            assertTrue(video.mediaSize().orElseThrow() > 0, "saída vazia");
            assertEquals(1, video.seconds().orElseThrow());
            assertEquals(160, video.width().orElseThrow());
            assertEquals(120, video.height().orElseThrow());
            var thumbnail = video.jpegThumbnail().orElseThrow();
            assertEquals((byte) 0xFF, thumbnail[0]);
            assertEquals((byte) 0xD8, thumbnail[1]);
            // +faststart: o moov vem antes do mdat (o WhatsApp toca enquanto baixa)
            byte[] mp4;
            try (var in = payload.openPlaintext()) {
                mp4 = in.readAllBytes();
            }
            var boxes = new String(mp4, StandardCharsets.ISO_8859_1);
            assertEquals("ftyp", boxes.substring(4, 8));
            assertTrue(boxes.indexOf("moov") > 0 && boxes.indexOf("moov") < boxes.indexOf("mdat"),
                    "moov depois do mdat: o faststart não rodou");
        } finally {
            if (payload != null) {
                payload.close();
            }
        }
    }
}
