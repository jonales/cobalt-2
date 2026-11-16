package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** "Aparelhos conectados" mostrava "Windows" para toda integração web: agora vai o nome configurado. */
class DeviceOsNameTest {

    @Test
    @DisplayName("companion web ou desktop usa o nome configurado")
    void webCompanionUsesTheConfiguredName() {
        assertEquals("Msgforge", WhatsAppSocketClient.deviceOsName(ClientPlatformType.WINDOWS, " Msgforge "));
        assertEquals("Msgforge", WhatsAppSocketClient.deviceOsName(ClientPlatformType.WEB, "Msgforge"));
        assertEquals("Windows", WhatsAppSocketClient.deviceOsName(ClientPlatformType.WINDOWS, null));
        assertEquals("Mac OS", WhatsAppSocketClient.deviceOsName(ClientPlatformType.MACOS, ""));
        assertEquals("Android", WhatsAppSocketClient.deviceOsName(ClientPlatformType.ANDROID, "Msgforge"));
    }
}
