package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.status.IqSetAboutRequest;
import com.github.auties00.cobalt.wire.stanza.iq.status.IqSetAboutResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IqSetAboutResponse")
class IqSetAboutResponseTest {
    private static Stanza request(String id) {
        return new IqSetAboutRequest("recado").toStanza().attribute("id", id).build();
    }

    // Observed live (2026-11-19): the relay answers the about update with a bare result whose id
    // is the request id, in hexadecimal. Parsing it as a long made every real success unparseable.
    @Test
    void hexIdResultIsSuccess() {
        var reply = new StanzaBuilder().description("iq")
                .attribute("from", "s.whatsapp.net")
                .attribute("type", "result")
                .attribute("id", "C783B2327A")
                .build();

        var parsed = IqSetAboutResponse.of(reply, request("C783B2327A"));

        assertTrue(parsed.isPresent());
        var success = assertInstanceOf(IqSetAboutResponse.Success.class, parsed.get());
        assertEquals("C783B2327A", success.aboutId());
    }

    @Test
    void resultForAnotherRequestIsNotSuccess() {
        var reply = new StanzaBuilder().description("iq")
                .attribute("type", "result")
                .attribute("id", "AAAA")
                .build();

        assertTrue(IqSetAboutResponse.Success.of(reply, request("BBBB")).isEmpty());
    }
}
