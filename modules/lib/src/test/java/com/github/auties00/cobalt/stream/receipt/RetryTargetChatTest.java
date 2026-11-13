package com.github.auties00.cobalt.stream.receipt;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * O recibo de retry vem de um aparelho ({@code user:device}), mas a conversa é guardada por
 * usuário. Com o JID do aparelho, a mensagem original não era encontrada e o reenvio era recusado
 * como "mensagem não existe". Espelha o {@code getTargetChat} do WhatsApp Web.
 */
class RetryTargetChatTest {
    private static final Jid SELF_PN = Jid.of("556182243991:21@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("249533976023044:21@lid");

    @Test
    @DisplayName("aparelho de um contato por LID: a conversa é a do usuário, sem o aparelho")
    void contactDeviceByLid() {
        assertEquals(Jid.of("277412457795677@lid"),
                MessageReceiptStreamHandler.retryTargetChat(Jid.of("277412457795677:46@lid"), null, SELF_PN, SELF_LID));
    }

    @Test
    @DisplayName("aparelho de um contato pelo número: a conversa é a do usuário, sem o aparelho")
    void contactDeviceByPhone() {
        assertEquals(Jid.of("556198462220@s.whatsapp.net"),
                MessageReceiptStreamHandler.retryTargetChat(Jid.of("556198462220:46@s.whatsapp.net"), null, SELF_PN, SELF_LID));
    }

    @Test
    @DisplayName("outro aparelho da própria conta: a conversa é a do recipient")
    void ownOtherDeviceUsesRecipient() {
        var recipient = Jid.of("277412457795677@lid");
        assertEquals(recipient,
                MessageReceiptStreamHandler.retryTargetChat(Jid.of("249533976023044@lid"), recipient, SELF_PN, SELF_LID));
        assertEquals(recipient,
                MessageReceiptStreamHandler.retryTargetChat(Jid.of("556182243991@s.whatsapp.net"), recipient, SELF_PN, SELF_LID));
    }

    @Test
    @DisplayName("outro aparelho da própria conta sem recipient: sem conversa")
    void ownOtherDeviceWithoutRecipient() {
        assertNull(MessageReceiptStreamHandler.retryTargetChat(Jid.of("249533976023044@lid"), null, SELF_PN, SELF_LID));
    }

    @Test
    @DisplayName("grupo: o JID vem como está")
    void groupIsKept() {
        var group = Jid.of("120363404900199480@g.us");
        assertEquals(group, MessageReceiptStreamHandler.retryTargetChat(group, null, SELF_PN, SELF_LID));
    }
}
