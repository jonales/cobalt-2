package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.exception.linked.WhatsAppFacebookGraphQlException;
import com.github.auties00.cobalt.exception.linked.WhatsAppStreamException;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * As renovações de credenciais do bootstrap rodam em threads virtuais próprias. Uma falha que não
 * seja do GraphQL (o timeout do {@code fb:thrift_iq} sem resposta, por exemplo) escapava da thread
 * e caía no tratador padrão da JVM ("Exception in thread") a cada login de conta Business.
 */
class SuccessStreamHandlerCredentialsRefreshTest {
    @Test
    @DisplayName("timeout de stanza na renovação vira aviso no log, não exceção solta")
    void stanzaTimeoutDoesNotEscape() {
        var stanza = new StanzaBuilder().description("iq").attribute("xmlns", "fb:thrift_iq").build();
        assertDoesNotThrow(() -> SuccessStreamHandler.runCredentialsRefresh("facebook graphql", () -> {
            throw new WhatsAppStreamException.NodeTimeout(stanza);
        }));
    }

    @Test
    @DisplayName("sessão ainda não inicializada é ignorada")
    void unseededSessionIsIgnored() {
        assertDoesNotThrow(() -> SuccessStreamHandler.runCredentialsRefresh("facebook graphql", () -> {
            throw new WhatsAppFacebookGraphQlException.SessionUnseeded();
        }));
    }

    @Test
    @DisplayName("qualquer outra falha de execução também não escapa da thread")
    void anyRuntimeFailureDoesNotEscape() {
        assertDoesNotThrow(() -> SuccessStreamHandler.runCredentialsRefresh("whatsapp web graphql", () -> {
            throw new IllegalStateException("falha qualquer");
        }));
    }
}
