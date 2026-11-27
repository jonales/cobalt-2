/**
 * Defines the telemetry facade for Cobalt.
 *
 * <p>This module provides two dependency-free observability pillars: privacy-redacted logging in
 * {@code com.github.auties00.cobalt.telemetry.log} and JDK Flight Recorder (JFR) metrics events in
 * {@code com.github.auties00.cobalt.telemetry.metrics}. Sensitive values (phone numbers, JIDs, tokens,
 * codes, and key material) are masked before reaching any sink.
 */
module com.github.auties00.cobalt.telemetry {
    // JUL is the backing logging framework
    requires java.logging;

    // JFR backs the metrics pillar without external dependencies
    requires jdk.jfr;

    // Logs: the Log facade, the redaction engine, and the LogRedactable contract
    exports com.github.auties00.cobalt.telemetry.log;

    // Metrics: JFR event definitions for connection, stanza, message, and media telemetry
    exports com.github.auties00.cobalt.telemetry.metrics;
}
