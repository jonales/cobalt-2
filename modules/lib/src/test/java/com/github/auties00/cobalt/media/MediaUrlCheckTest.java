package com.github.auties00.cobalt.media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// CB-01: the url of a received media message is written by the sender; only the WhatsApp CDN is fetched
class MediaUrlCheckTest {

    @Test
    @DisplayName("Only https urls on the WhatsApp CDN are downloaded")
    void onlyTheWhatsAppCdn() {
        assertTrue(LiveMediaConnectionService.isWhatsAppMediaUrl("https://mmg.whatsapp.net/v/t62.7118-24/abc.enc?ccb=11-4"));
        assertTrue(LiveMediaConnectionService.isWhatsAppMediaUrl("https://media-gru1-1.cdn.whatsapp.net/v/t62/x.enc"));
        assertTrue(LiveMediaConnectionService.isWhatsAppMediaUrl("https://MMG.WhatsApp.net/v/x.enc"));

        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("http://mmg.whatsapp.net/v/x.enc"));          // sem TLS
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("https://169.254.169.254/latest/meta-data"));  // rede interna
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("https://localhost:8080/manager"));
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("https://whatsapp.net.evil.com/x"));          // sufixo falso
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("https://evilwhatsapp.net/x"));
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("https://user@mmg.whatsapp.net/x"));          // user info
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("https://mmg.whatsapp.net:8443/x"));         // outra porta
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("file:///etc/passwd"));
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl("not a url"));
        assertFalse(LiveMediaConnectionService.isWhatsAppMediaUrl(null));
    }
}
