package com.github.auties00.cobalt.stream.newsletter;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O status de um contato chega como {@code <status from="status@broadcast">}. Antes ele era
 * confirmado e descartado (nenhum onNewStatus); agora segue para o caminho das mensagens, como no
 * WAWebCommsHandleMessagingStanza. Post de canal continua no tratamento de canal.
 */
class ContactStatusRoutingTest {

    private static Stanza status(String from) {
        return new StanzaBuilder().description("status")
                .attribute("from", Jid.of(from))
                .attribute("id", "ABC")
                .attribute("type", "text")
                .build();
    }

    @Test
    @DisplayName("status de contato vai para o caminho das mensagens")
    void contactStatusIsForwarded() {
        var forwarded = new ArrayList<Stanza>();
        var handler = new NewsletterStatusStreamHandler(null, null, forwarded::add);
        handler.handle(status("status@broadcast"));
        assertEquals(1, forwarded.size());
        assertEquals("ABC", forwarded.getFirst().getAttributeAsString("id", null));
    }

    @Test
    @DisplayName("post de canal não é encaminhado")
    void newsletterStatusIsNotForwarded() {
        var forwarded = new ArrayList<Stanza>();
        var handler = new NewsletterStatusStreamHandler(null, null, forwarded::add);
        handler.handle(status("120363000000000000@newsletter"));
        assertTrue(forwarded.isEmpty());
    }
}
