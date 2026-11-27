package com.github.auties00.cobalt.telemetry.metrics;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * JFR event measuring the latency and outcome of a binary-XMPP stanza or IQ exchange.
 *
 * <p>Callers invoke {@link #begin()} before dispatch and {@link #commit()} when the response arrives or times
 * out so JFR captures the exact duration.
 */
@Name("cobalt.Stanza")
@Label("Cobalt Stanza Exchange")
@Description("Records the duration and outcome of a Cobalt stanza or IQ round-trip.")
@Category({"Cobalt", "Stanza"})
@StackTrace(false)
public final class CobaltStanzaEvent extends Event {
    /**
     * Stanza root tag (for example {@code iq}, {@code message}, or {@code receipt}).
     */
    @Label("Tag")
    @Description("The root XML tag of the stanza.")
    public String tag;

    /**
     * Stanza namespace or type attribute.
     */
    @Label("Namespace")
    @Description("The xmlns or type classifying the stanza.")
    public String namespace;

    /**
     * Exchange direction ({@code outbound} or {@code inbound}).
     */
    @Label("Direction")
    @Description("Whether the stanza was outbound or inbound.")
    public String direction;

    /**
     * Outcome status (for example {@code ok}, {@code error}, or {@code timeout}).
     */
    @Label("Outcome")
    @Description("The completion outcome of the stanza exchange.")
    public String outcome;

    /**
     * Constructs an unpopulated stanza exchange JFR event.
     */
    public CobaltStanzaEvent() {
    }
}
