package com.github.auties00.cobalt.telemetry.metrics;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * JFR event recording a Cobalt client connection lifecycle transition.
 *
 * <p>Emitted on connect, disconnect, and reconnect transitions without carrying any PII.
 */
@Name("cobalt.Connection")
@Label("Cobalt Connection Lifecycle")
@Description("Records a Cobalt client connection lifecycle transition.")
@Category({"Cobalt", "Connection"})
@StackTrace(false)
public final class CobaltConnectionEvent extends Event {
    /**
     * Transport family (for example {@code linked} or {@code cloud}).
     */
    @Label("Transport")
    @Description("The Cobalt transport family handling the connection.")
    public String transport;

    /**
     * Lifecycle action (for example {@code connect}, {@code reconnect}, or {@code disconnect}).
     */
    @Label("Action")
    @Description("The lifecycle transition recorded by this event.")
    public String action;

    /**
     * Outcome or disconnect reason label.
     */
    @Label("Reason")
    @Description("The reason or outcome associated with the transition.")
    public String reason;

    /**
     * Whether the transition completed successfully.
     */
    @Label("Success")
    @Description("Whether the lifecycle operation succeeded.")
    public boolean success;

    /**
     * Constructs an unpopulated connection lifecycle JFR event.
     */
    public CobaltConnectionEvent() {
    }
}
