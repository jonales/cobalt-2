package com.github.auties00.cobalt.telemetry.metrics;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * JFR event recording a media upload or download transfer.
 *
 * <p>Captures transfer direction, media type, byte count, and outcome so JFR tooling can profile bandwidth
 * and media pipeline failures without recording URLs or keys.
 */
@Name("cobalt.MediaTransfer")
@Label("Cobalt Media Transfer")
@Description("Records a media upload or download transfer duration, size, and outcome.")
@Category({"Cobalt", "Media"})
@StackTrace(false)
public final class CobaltMediaTransferEvent extends Event {
    /**
     * Transfer operation ({@code upload} or {@code download}).
     */
    @Label("Operation")
    @Description("Whether the media was uploaded or downloaded.")
    public String operation;

    /**
     * Media category (for example {@code image}, {@code video}, {@code audio}, or {@code document}).
     */
    @Label("Media Type")
    @Description("The logical media category.")
    public String mediaType;

    /**
     * Transferred payload size in bytes.
     */
    @Label("Bytes")
    @DataAmount
    @Description("Number of payload bytes transferred.")
    public long bytes;

    /**
     * Whether the transfer completed successfully.
     */
    @Label("Success")
    @Description("Whether the media transfer succeeded.")
    public boolean success;

    /**
     * Constructs an unpopulated media transfer JFR event.
     */
    public CobaltMediaTransferEvent() {
    }
}
