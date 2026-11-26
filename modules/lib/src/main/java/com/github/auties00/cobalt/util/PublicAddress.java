package com.github.auties00.cobalt.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Checks that a url only reaches the public internet before the client fetches it on the user's behalf (the page and
 * the thumbnail of a link preview).
 *
 * <p>The url of a link preview is written by whoever sends the text, so fetched as is it let the client request
 * internal addresses: loopback, private networks, link-local (cloud metadata at {@code 169.254.169.254}), CGNAT,
 * IPv6 unique-local and multicast (SSRF, CB-02 of the 2026-11-14 review). Every address the host resolves to must
 * be public.
 *
 * <p>Limits: the check runs before the connection, so a DNS answer that changes in between (rebinding) or a
 * redirect followed inside a third-party fetcher is not covered here; callers that own the HTTP client should also
 * disable redirects.
 */
public final class PublicAddress {

    private PublicAddress() {
    }

    /**
     * Whether the url is {@code http} or {@code https}, without user info, and its host resolves only to public
     * addresses.
     *
     * @param uri the url to check
     * @return {@code true} only when every resolved address is public
     */
    public static boolean isPublic(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        var scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        try {
            var host = uri.getHost();
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }
            var addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (var address : addresses) {
                if (!isPublic(address)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException | SecurityException e) {
            return false;
        }
    }

    /**
     * Whether a single address is public.
     *
     * @param address the address
     * @return {@code false} for loopback, any-local, link-local, site-local, multicast, CGNAT ({@code 100.64.0.0/10}),
     *         {@code 0.0.0.0/8}, IPv6 unique-local ({@code fc00::/7}) and IPv4-mapped private addresses
     */
    static boolean isPublic(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        var bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            var first = bytes[0] & 0xff;
            var second = bytes[1] & 0xff;
            return first != 0 && !(first == 100 && second >= 64 && second <= 127);
        }
        if (address instanceof Inet6Address) {
            if ((bytes[0] & 0xfe) == 0xfc) {
                return false;   // fc00::/7
            }
            var mapped = true;   // ::ffff:a.b.c.d
            for (var i = 0; i < 10; i++) {
                mapped &= bytes[i] == 0;
            }
            if (mapped && (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff) {
                try {
                    return isPublic(InetAddress.getByAddress(java.util.Arrays.copyOfRange(bytes, 12, 16)));
                } catch (UnknownHostException e) {
                    return false;
                }
            }
        }
        return true;
    }
}
