package com.github.auties00.cobalt.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// CB-02: the link preview only fetches public addresses (IP literals: no DNS in the test)
class PublicAddressTest {

    @Test
    @DisplayName("Internal addresses are refused, public ones pass")
    void internalAddressesAreRefused() {
        for (var internal : new String[] {
                "http://127.0.0.1/", "http://localhost:8080/manager", "http://10.0.0.5/", "http://172.16.1.1/",
                "http://192.168.65.1/", "http://169.254.169.254/latest/meta-data", "http://100.64.0.1/",
                "http://0.0.0.0/", "http://[::1]/", "http://[fd00::1]/", "http://[fe80::1]/", "http://[::ffff:10.0.0.1]/",
                "http://224.0.0.1/"}) {
            assertFalse(PublicAddress.isPublic(URI.create(internal)), internal);
        }
        assertFalse(PublicAddress.isPublic(URI.create("ftp://8.8.8.8/")));          // só http(s)
        assertFalse(PublicAddress.isPublic(URI.create("http://user@8.8.8.8/")));    // sem user info
        assertFalse(PublicAddress.isPublic(URI.create("file:///etc/passwd")));
        assertFalse(PublicAddress.isPublic((URI) null));

        assertTrue(PublicAddress.isPublic(URI.create("https://8.8.8.8/")));
        assertTrue(PublicAddress.isPublic(URI.create("http://[2001:4860:4860::8888]/")));
        assertTrue(PublicAddress.isPublic(URI.create("http://100.128.0.1/")));      // fora do CGNAT
    }
}
