package com.github.auties00.cobalt.media.transcode.audio;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessageBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MF-04: the media goes to ffmpeg through a custom reader, but a "media" that is in fact a playlist (HLS) made the
 * demuxer open the addresses listed in it (file://, http://): reading local files or calling internal addresses.
 * With no protocol allowed, the nested open fails and the media is refused.
 */
class NestedProtocolTest {

    @Test
    @DisplayName("an HLS playlist pointing to a local file is refused, not read")
    void aPlaylistDoesNotOpenNestedAddresses() throws Exception {
        var secret = Files.createTempFile("segredo", ".wav");
        var playlist = Files.createTempFile("lista", ".m3u8");
        try {
            Files.write(secret, wav());
            Files.writeString(playlist, "#EXTM3U\n#EXT-X-TARGETDURATION:3\n#EXTINF:3,\n" + secret.toUri() + "\n#EXT-X-ENDLIST\n",
                    StandardCharsets.UTF_8);
            var audio = new AudioMessageBuilder().mimetype("audio/mpeg").build();
            try (var channel = Files.newByteChannel(playlist, StandardOpenOption.READ)) {
                assertThrows(Exception.class,
                        () -> new AudioPipeline().run(audio, channel, SettingsSyncAction.MediaQualitySetting.STANDARD).close());
            }
        } finally {
            Files.deleteIfExists(secret);
            Files.deleteIfExists(playlist);
        }
    }

    private static byte[] wav() {
        var rate = 44_100;
        var frames = rate * 3;
        var data = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (var i = 0; i < frames; i++) {
            data.putShort((short) (Math.sin(2 * Math.PI * 330 * i / rate) * 8_000));
        }
        var header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes()).putInt(36 + frames * 2).put("WAVE".getBytes())
                .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes()).putInt(frames * 2);
        var out = new ByteArrayOutputStream();
        out.writeBytes(header.array());
        out.writeBytes(data.array());
        return out.toByteArray();
    }

    @Test
    @DisplayName("an ffconcat list naming a file in the working directory is refused, not read")
    void aConcatListDoesNotOpenFiles() throws Exception {
        var secret = java.nio.file.Path.of("segredo-mf04.wav").toAbsolutePath();
        var list = Files.createTempFile("lista", ".txt");
        try {
            Files.write(secret, wav());
            Files.writeString(list, "ffconcat version 1.0\nfile segredo-mf04.wav\n", StandardCharsets.UTF_8);
            var audio = new AudioMessageBuilder().mimetype("audio/mpeg").build();
            try (var channel = Files.newByteChannel(list, StandardOpenOption.READ)) {
                assertThrows(Exception.class,
                        () -> new AudioPipeline().run(audio, channel, SettingsSyncAction.MediaQualitySetting.STANDARD).close());
            }
        } finally {
            Files.deleteIfExists(secret);
            Files.deleteIfExists(list);
        }
    }
}
