/**
 * Defines Cobalt's metrics telemetry, the second telemetry pillar alongside the logging facade in
 * {@code com.github.auties00.cobalt.telemetry.log}.
 *
 * <p>Metrics are surfaced as JFR (JDK Flight Recorder) events so applications can record and analyse
 * Cobalt's runtime behaviour (connection lifecycle, stanza round-trips, message throughput, and media
 * transfers) with standard JDK tooling without shipping an external metrics pipeline. All event fields are
 * privacy-safe and never carry raw PII, phone numbers, JIDs, or message bodies.
 */
package com.github.auties00.cobalt.telemetry.metrics;
