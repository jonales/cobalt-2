package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.stanza.iq.profilepicture.IqSendProfilePictureRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O pedido de foto como o WAWebSendProfilePictureJob monta: {@code target} só para grupo e um único
 * {@code <picture type="image">}. A foto da própria conta saía com {@code target} = o próprio número e
 * uma {@code <picture type="preview">}, e o servidor não respondia (visto ao vivo em 2026-11-15).
 */
class ProfilePictureStanzaTest {

    @Test
    @DisplayName("foto da conta: sem target e com uma picture só")
    void ownPictureHasNoTargetAndOnePicture() {
        var stanza = new IqSendProfilePictureRequest(null, SizedInputStream.of(new byte[] {1, 2, 3}))
                .toStanza().build();
        assertFalse(stanza.hasAttribute("target"));
        assertEquals("w:profile:picture", stanza.getAttributeAsString("xmlns", null));
        assertEquals(1, stanza.streamChildren("picture").count());
        assertTrue(stanza.streamChildren("picture").allMatch(p -> p.hasAttribute("type", "image")));
    }

    @Test
    @DisplayName("foto de grupo: com target do grupo")
    void groupPictureCarriesTheGroupAsTarget() {
        var group = Jid.of("120363000000000000@g.us");
        var stanza = new IqSendProfilePictureRequest(group, SizedInputStream.of(new byte[] {1, 2, 3}))
                .toStanza().build();
        assertTrue(stanza.hasAttribute("target", group));
        assertEquals(1, stanza.streamChildren("picture").count());
    }
}
