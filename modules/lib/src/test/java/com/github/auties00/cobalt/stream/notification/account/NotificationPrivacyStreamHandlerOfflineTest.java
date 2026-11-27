package com.github.auties00.cobalt.stream.notification.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O {@code privacy_token} esperava o fim da entrega offline na thread do stanza, antes do ACK. O servidor só
 * manda o resto do acumulado (e o {@code <offline/>} que encerra a espera) depois dos ACKs do que já entregou:
 * cada lote desses avisos travava a entrega até o teto de 5 min da espera.
 */
class NotificationPrivacyStreamHandlerOfflineTest {
    @Test
    @DisplayName("durante o acumulado, a espera roda fora da thread do stanza (o ACK não espera)")
    void waitDoesNotBlockTheStanzaThread() throws InterruptedException {
        var offlineEnded = new CountDownLatch(1);
        var applied = new CountDownLatch(1);

        NotificationPrivacyStreamHandler.runAfterOfflineDelivery(false, () -> {
            try {
                offlineEnded.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "privacy-token-test", applied::countDown);

        // a chamada voltou sem o fim da entrega offline: o ACK sai na hora
        assertFalse(applied.await(200, TimeUnit.MILLISECONDS), "aplicou antes do fim do acumulado");
        offlineEnded.countDown();
        assertTrue(applied.await(5, TimeUnit.SECONDS), "não aplicou depois do fim do acumulado");
    }

    @Test
    @DisplayName("com a entrega offline encerrada, aplica na hora, na mesma thread")
    void appliesInlineWhenOfflineDeliveryIsOver() {
        var applied = new AtomicBoolean();
        NotificationPrivacyStreamHandler.runAfterOfflineDelivery(true, () -> {
            throw new AssertionError("não deveria esperar");
        }, "privacy-token-test", () -> applied.set(true));
        assertTrue(applied.get());
    }

    @Test
    @DisplayName("falha ao aplicar depois da espera não escapa da thread virtual")
    void failureAfterTheWaitDoesNotEscape() throws InterruptedException {
        var done = new CountDownLatch(1);
        NotificationPrivacyStreamHandler.runAfterOfflineDelivery(false, () -> { }, "privacy-token-test", () -> {
            done.countDown();
            throw new IllegalStateException("falha qualquer");
        });
        assertTrue(done.await(5, TimeUnit.SECONDS));
    }
}
