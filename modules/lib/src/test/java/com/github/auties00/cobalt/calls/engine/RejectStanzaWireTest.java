package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.signaling.session.RejectStanza;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The decline the phone sends is {@code <reject call-id call-creator count="0"/>}, with no reason. A reject
 * with {@code reason="rejected"} and no count was accepted by the server and ignored.
 */
class RejectStanzaWireTest {

    private static final Jid LID = Jid.of("277412457795677@lid");

    @Test
    @DisplayName("plain decline: count=\"0\", no reason, creator as given")
    void plainDeclineLooksLikeThePhone() {
        var stanza = RejectStanza.of("007BDC78C974F10DB72C3688C7F707BF", LID, CallEndReason.REJECTED).toStanza();
        assertEquals("reject", stanza.description());
        assertEquals("0", stanza.getAttributeAsString("count", null));
        assertNull(stanza.getAttributeAsString("reason", null));
        assertEquals(LID, stanza.getAttributeAsJid("call-creator", null));
    }
}
