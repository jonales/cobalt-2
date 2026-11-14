package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reject of a ringing call must carry the creator the offer used (a LID), not the phone number the
 * application received: with the phone number the server accepts the reject but the caller keeps ringing.
 */
class RejectCreatorTest {

    private static final Jid LID = Jid.of("277412457795677@lid");
    private static final Jid PHONE = Jid.of("556198462220@s.whatsapp.net");

    @Test
    @DisplayName("tracked call: the reject uses the offer's LID creator, not the phone number from the app")
    void trackedCallUsesTheOfferCreator() {
        assertEquals(LID, LifecycleController.rejectCreator(LID, PHONE));
    }

    @Test
    @DisplayName("no tracked call: the reject falls back to the creator the application supplied")
    void untrackedCallUsesTheSuppliedCreator() {
        assertEquals(PHONE, LifecycleController.rejectCreator(null, PHONE));
    }
}
