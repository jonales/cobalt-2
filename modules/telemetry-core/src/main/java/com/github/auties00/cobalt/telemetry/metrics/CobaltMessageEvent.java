package com.github.auties00.cobalt.telemetry.metrics;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * JFR event recording an inbound or outbound message processing operation.
 *
 * <p>Captures structural metadata only (direction, chat kind, encryption mode, and outcome) without any
 * message content or user identifiers.
 */
@Name("cobalt.Message")
@Label("Cobalt Message Processing")
@Description("Records inbound or outbound message processing latency and outcome.")
@Category({"Cobalt", "Message"})
@StackTrace(false)
public final class CobaltMessageEvent extends Event {
    /**
     * Message direction ({@code outbound} or {@code inbound}).
     */
    @Label("Direction")
    @Description("Whether the message was sent or received.")
    public String direction;

    /**
     * Target conversation kind (for example {@code direct}, {@code group}, {@code status}, or {@code newsletter}).
     */
    @Label("Chat Kind")
    @Description("The category of conversation for the message.")
    public String chatKind;

    /**
     * Encryption mode (for example {@code msg}, {@code pkmsg}, {@code skmsg}, or {@code plain}).
     */
    @Label("Encryption")
    @Description("The Signal or transport encryption mode used.")
    public String encryption;

    /**
     * Processing outcome (for example {@code ok}, {@code retry}, or {@code failed}).
     */
    @Label("Outcome")
    @Description("The outcome of processing the message.")
    public String outcome;

    /**
     * Constructs an unpopulated message processing JFR event.
     */
    public CobaltMessageEvent() {
    }
}
