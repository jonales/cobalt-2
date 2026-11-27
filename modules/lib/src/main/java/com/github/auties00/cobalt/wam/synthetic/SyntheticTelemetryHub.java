package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wam.WamService;

import java.util.Objects;

/**
 * Aggregates all synthetic session telemetry emitters and triggers them together once a linked socket opens.
 *
 * <p>Each emitter captures a one-shot session snapshot and commits its WAM events through the shared
 * {@link WamService}. Grouping them behind this hub keeps {@code LiveLinkedWhatsAppClient} free of
 * per-emitter wiring and lifecycle boilerplate.
 */
public final class SyntheticTelemetryHub {
    /**
     * Emitter that reports the business-account snapshot once per connect.
     */
    private final SyntheticBusinessTelemetry business;

    /**
     * Emitter that reports the messaging-subsystem snapshot once per connect.
     */
    private final SyntheticMessagingTelemetry messaging;

    /**
     * Emitter that reports the browser-performance snapshot once per connect.
     */
    private final SyntheticBrowserPerfTelemetry browserPerf;

    /**
     * Emitter that reports the media-subsystem snapshot once per connect.
     */
    private final SyntheticMediaTelemetry media;

    /**
     * Emitter that reports the privacy-and-safety snapshot once per connect.
     */
    private final SyntheticPrivacySafetyTelemetry privacySafety;

    /**
     * Emitter that reports the UI-interaction snapshot once per connect.
     */
    private final SyntheticUiInteractionTelemetry uiInteraction;

    /**
     * Emitter that reports the group-and-community snapshot once per connect.
     */
    private final SyntheticGroupTelemetry group;

    /**
     * Emitter that reports the settings snapshot once per connect.
     */
    private final SyntheticSettingsTelemetry settings;

    /**
     * Emitter that reports the channel snapshot once per connect.
     */
    private final SyntheticChannelTelemetry channel;

    /**
     * Emitter that reports the status snapshot once per connect.
     */
    private final SyntheticStatusTelemetry status;

    /**
     * Emitter that reports the user-activity snapshot once per connect.
     */
    private final SyntheticUserActivityTelemetry userActivity;

    /**
     * Emitter that reports the AI-bot snapshot once per connect.
     */
    private final SyntheticAiBotTelemetry aiBot;

    /**
     * Emitter that reports the contacts snapshot once per connect.
     */
    private final SyntheticContactsTelemetry contacts;

    /**
     * Emitter that reports the crash snapshot once per connect.
     */
    private final SyntheticCrashTelemetry crash;

    /**
     * Emitter that reports the voice-note snapshot once per connect.
     */
    private final SyntheticVoiceNoteTelemetry voiceNote;

    /**
     * Emitter that reports the call snapshot once per connect.
     */
    private final SyntheticCallTelemetry call;

    /**
     * Constructs the hub and instantiates every synthetic telemetry emitter bound to {@code client} and
     * {@code wamService}.
     *
     * @param client     the owning linked client
     * @param wamService the WAM service that receives committed telemetry events
     * @throws NullPointerException if {@code client} or {@code wamService} is {@code null}
     */
    public SyntheticTelemetryHub(LinkedWhatsAppClient client, WamService wamService) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(wamService, "wamService cannot be null");
        this.business = new SyntheticBusinessTelemetry(client, wamService);
        this.messaging = new SyntheticMessagingTelemetry(client, wamService);
        this.browserPerf = new SyntheticBrowserPerfTelemetry(client, wamService);
        this.media = new SyntheticMediaTelemetry(client, wamService);
        this.privacySafety = new SyntheticPrivacySafetyTelemetry(client, wamService);
        this.uiInteraction = new SyntheticUiInteractionTelemetry(client, wamService);
        this.group = new SyntheticGroupTelemetry(client, wamService);
        this.settings = new SyntheticSettingsTelemetry(client, wamService);
        this.channel = new SyntheticChannelTelemetry(client, wamService);
        this.status = new SyntheticStatusTelemetry(client, wamService);
        this.userActivity = new SyntheticUserActivityTelemetry(client, wamService);
        this.aiBot = new SyntheticAiBotTelemetry(client, wamService);
        this.contacts = new SyntheticContactsTelemetry(client, wamService);
        this.crash = new SyntheticCrashTelemetry(client, wamService);
        this.voiceNote = new SyntheticVoiceNoteTelemetry(client, wamService);
        this.call = new SyntheticCallTelemetry(client, wamService);
    }

    /**
     * Emits all per-session synthetic telemetry snapshots in deterministic order.
     */
    public void emitAll() {
        business.emitSessionTelemetry();
        messaging.emitSessionTelemetry();
        browserPerf.emitSessionTelemetry();
        media.emitSessionTelemetry();
        privacySafety.emitPrivacySafetyTelemetry();
        uiInteraction.emitSessionTelemetry();
        group.emitGroupTelemetry();
        settings.emitSettingsTelemetry();
        channel.emitSessionTelemetry();
        status.emitSessionTelemetry();
        userActivity.emitSessionTelemetry();
        aiBot.emitSessionTelemetry();
        contacts.emitSessionTelemetry();
        crash.emitSessionTelemetry();
        voiceNote.emitSessionTelemetry();
        call.emitSessionTelemetry();
    }
}
