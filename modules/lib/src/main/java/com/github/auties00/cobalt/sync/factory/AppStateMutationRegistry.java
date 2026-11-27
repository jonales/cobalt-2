package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;

import java.util.Objects;

/**
 * Composes and holds every outgoing app-state (syncd) mutation factory used by the linked client.
 *
 * <p>Centralising factory construction here keeps inter-factory dependencies (such as archive and lock
 * mutations delegating to pin mutations) in one place and removes forty-plus factory fields from the client
 * facade.
 */
public final class AppStateMutationRegistry {
    /**
     * Factory that builds outgoing pin-chat sync mutations.
     */
    private final PinChatMutationFactory pinChat;

    /**
     * Factory that builds outgoing mute-chat sync mutations.
     */
    private final MuteChatMutationFactory muteChat;

    /**
     * Factory that builds outgoing archive-chat sync mutations.
     */
    private final ArchiveChatMutationFactory archiveChat;

    /**
     * Factory that builds outgoing mark-chat-as-read sync mutations.
     */
    private final MarkChatAsReadMutationFactory markChatAsRead;

    /**
     * Factory that builds outgoing clear-chat sync mutations.
     */
    private final ClearChatMutationFactory clearChat;

    /**
     * Factory that builds outgoing delete-chat sync mutations.
     */
    private final DeleteChatMutationFactory deleteChat;

    /**
     * Factory that builds outgoing lock-chat sync mutations.
     */
    private final LockChatMutationFactory lockChat;

    /**
     * Factory that builds outgoing label-edit sync mutations.
     */
    private final LabelEditMutationFactory labelEdit;

    /**
     * Factory that builds outgoing label-jid association sync mutations.
     */
    private final LabelAssociationMutationFactory labelAssociation;

    /**
     * Factory that builds outgoing label-reordering sync mutations.
     */
    private final LabelReorderingMutationFactory labelReordering;

    /**
     * Factory that builds outgoing quick-reply sync mutations.
     */
    private final QuickReplyMutationFactory quickReply;

    /**
     * Factory that builds outgoing chat-assignment sync mutations.
     */
    private final ChatAssignmentMutationFactory chatAssignment;

    /**
     * Factory that builds outgoing chat-assignment-opened-status sync mutations.
     */
    private final ChatAssignmentOpenedStatusMutationFactory chatAssignmentOpenedStatus;

    /**
     * Factory that builds outgoing business-broadcast-list sync mutations.
     */
    private final BusinessBroadcastListMutationFactory businessBroadcastList;

    /**
     * Factory that builds outgoing favourite-sticker sync mutations.
     */
    private final FavoriteStickerMutationFactory favoriteSticker;

    /**
     * Factory that builds outgoing remove-recent-sticker sync mutations.
     */
    private final RemoveRecentStickerMutationFactory removeRecentSticker;

    /**
     * Factory that builds outgoing bot-welcome-request sync mutations.
     */
    private final BotWelcomeRequestMutationFactory botWelcomeRequest;

    /**
     * Factory that builds outgoing AI-thread-rename sync mutations.
     */
    private final AiThreadRenameMutationFactory aiThreadRename;

    /**
     * Factory that builds outgoing AI-thread-delete sync mutations.
     */
    private final AiThreadDeleteMutationFactory aiThreadDelete;

    /**
     * Factory that builds outgoing favourites sync mutations.
     */
    private final FavoritesMutationFactory favorites;

    /**
     * Factory that builds outgoing note-edit sync mutations.
     */
    private final NoteEditMutationFactory noteEdit;

    /**
     * Factory that builds outgoing locale-setting sync mutations.
     */
    private final LocaleSettingMutationFactory localeSetting;

    /**
     * Factory that builds outgoing disable-link-previews sync mutations.
     */
    private final DisableLinkPreviewsMutationFactory disableLinkPreviews;

    /**
     * Factory that builds outgoing time-format sync mutations.
     */
    private final TimeFormatMutationFactory timeFormat;

    /**
     * Factory that builds outgoing Maiba-AI-features-control sync mutations.
     */
    private final MaibaAIFeaturesControlMutationFactory maibaAIFeaturesControl;

    /**
     * Factory that builds outgoing unarchive-chats-setting sync mutations.
     */
    private final UnarchiveChatsSettingMutationFactory unarchiveChatsSetting;

    /**
     * Factory that builds outgoing notification-activity-setting sync mutations.
     */
    private final NotificationActivitySettingMutationFactory notificationActivitySetting;

    /**
     * Factory that builds outgoing status-privacy sync mutations.
     */
    private final StatusPrivacyMutationFactory statusPrivacy;

    /**
     * Factory that builds outgoing push-name-setting sync mutations.
     */
    private final PushNameSettingMutationFactory pushNameSetting;

    /**
     * Factory that builds outgoing contact-action sync mutations.
     */
    private final ContactActionMutationFactory contactAction;

    /**
     * Factory that builds outgoing call-log sync mutations.
     */
    private final CallLogMutationFactory callLog;

    /**
     * Factory that builds outgoing NUX (onboarding-hint) sync mutations.
     */
    private final NuxActionMutationFactory nuxAction;

    /**
     * Factory that builds outgoing always-relay-calls sync mutations.
     */
    private final VoipRelayAllCallsMutationFactory voipRelayAllCalls;

    /**
     * Factory that builds outgoing custom-payment-methods sync mutations.
     */
    private final CustomPaymentMethodsMutationFactory customPaymentMethods;

    /**
     * Factory that builds outgoing payment-terms-of-service sync mutations.
     */
    private final PaymentTosMutationFactory paymentTos;

    /**
     * Factory that builds outgoing CTWA per-customer data-sharing sync mutations.
     */
    private final CtwaPerCustomerDataSharingMutationFactory ctwaPerCustomerDataSharing;

    /**
     * Factory that builds outgoing Web/Desktop beta opt-in sync mutations.
     */
    private final ExternalWebBetaMutationFactory externalWebBeta;

    /**
     * Factory that builds outgoing Private Processing privacy sync mutations.
     */
    private final PrivateProcessingSettingMutationFactory privateProcessingSetting;

    /**
     * Factory that builds outgoing automated-detections sync mutations.
     */
    private final DetectedOutcomesStatusMutationFactory detectedOutcomesStatus;

    /**
     * Factory that builds outgoing interactive-message disable-button sync mutations.
     */
    private final InteractiveMessageMutationFactory interactiveMessage;

    /**
     * Factory that builds outgoing recent-emoji-usage sync mutations.
     */
    private final RecentEmojiWeightsMutationFactory recentEmojiWeights;

    /**
     * Constructs and wires every outgoing app-state mutation factory.
     *
     * @param client         the owning linked client
     * @param abPropsService the AB-props service used by feature-gated factories
     * @param wamService     the WAM service used by telemetry-emitting factories
     * @throws NullPointerException if any argument is {@code null}
     */
    public AppStateMutationRegistry(LinkedWhatsAppClient client, ABPropsService abPropsService, WamService wamService) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        Objects.requireNonNull(wamService, "wamService cannot be null");
        this.pinChat = new PinChatMutationFactory(wamService);
        this.archiveChat = new ArchiveChatMutationFactory(pinChat);
        this.muteChat = new MuteChatMutationFactory(abPropsService);
        this.markChatAsRead = new MarkChatAsReadMutationFactory();
        this.clearChat = new ClearChatMutationFactory();
        this.deleteChat = new DeleteChatMutationFactory();
        this.lockChat = new LockChatMutationFactory(archiveChat, pinChat);
        this.labelEdit = new LabelEditMutationFactory();
        this.labelAssociation = new LabelAssociationMutationFactory();
        this.labelReordering = new LabelReorderingMutationFactory();
        this.quickReply = new QuickReplyMutationFactory();
        this.chatAssignment = new ChatAssignmentMutationFactory();
        this.chatAssignmentOpenedStatus = new ChatAssignmentOpenedStatusMutationFactory();
        this.businessBroadcastList = new BusinessBroadcastListMutationFactory();
        this.favoriteSticker = new FavoriteStickerMutationFactory();
        this.removeRecentSticker = new RemoveRecentStickerMutationFactory();
        this.botWelcomeRequest = new BotWelcomeRequestMutationFactory(wamService);
        this.aiThreadRename = new AiThreadRenameMutationFactory(client, wamService);
        this.aiThreadDelete = new AiThreadDeleteMutationFactory();
        this.favorites = new FavoritesMutationFactory();
        this.noteEdit = new NoteEditMutationFactory();
        this.localeSetting = new LocaleSettingMutationFactory();
        this.disableLinkPreviews = new DisableLinkPreviewsMutationFactory();
        this.timeFormat = new TimeFormatMutationFactory();
        this.maibaAIFeaturesControl = new MaibaAIFeaturesControlMutationFactory();
        this.unarchiveChatsSetting = new UnarchiveChatsSettingMutationFactory();
        this.notificationActivitySetting = new NotificationActivitySettingMutationFactory();
        this.statusPrivacy = new StatusPrivacyMutationFactory();
        this.pushNameSetting = new PushNameSettingMutationFactory();
        this.contactAction = new ContactActionMutationFactory();
        this.callLog = new CallLogMutationFactory();
        this.nuxAction = new NuxActionMutationFactory();
        this.voipRelayAllCalls = new VoipRelayAllCallsMutationFactory();
        this.customPaymentMethods = new CustomPaymentMethodsMutationFactory();
        this.paymentTos = new PaymentTosMutationFactory();
        this.ctwaPerCustomerDataSharing = new CtwaPerCustomerDataSharingMutationFactory();
        this.externalWebBeta = new ExternalWebBetaMutationFactory();
        this.privateProcessingSetting = new PrivateProcessingSettingMutationFactory();
        this.detectedOutcomesStatus = new DetectedOutcomesStatusMutationFactory();
        this.interactiveMessage = new InteractiveMessageMutationFactory();
        this.recentEmojiWeights = new RecentEmojiWeightsMutationFactory();
    }

    /**
     * Returns the pin-chat mutation factory.
     *
     * @return the {@link PinChatMutationFactory}
     */
    public PinChatMutationFactory pinChat() {
        return pinChat;
    }

    /**
     * Returns the mute-chat mutation factory.
     *
     * @return the {@link MuteChatMutationFactory}
     */
    public MuteChatMutationFactory muteChat() {
        return muteChat;
    }

    /**
     * Returns the archive-chat mutation factory.
     *
     * @return the {@link ArchiveChatMutationFactory}
     */
    public ArchiveChatMutationFactory archiveChat() {
        return archiveChat;
    }

    /**
     * Returns the mark-chat-as-read mutation factory.
     *
     * @return the {@link MarkChatAsReadMutationFactory}
     */
    public MarkChatAsReadMutationFactory markChatAsRead() {
        return markChatAsRead;
    }

    /**
     * Returns the clear-chat mutation factory.
     *
     * @return the {@link ClearChatMutationFactory}
     */
    public ClearChatMutationFactory clearChat() {
        return clearChat;
    }

    /**
     * Returns the delete-chat mutation factory.
     *
     * @return the {@link DeleteChatMutationFactory}
     */
    public DeleteChatMutationFactory deleteChat() {
        return deleteChat;
    }

    /**
     * Returns the lock-chat mutation factory.
     *
     * @return the {@link LockChatMutationFactory}
     */
    public LockChatMutationFactory lockChat() {
        return lockChat;
    }

    /**
     * Returns the label-edit mutation factory.
     *
     * @return the {@link LabelEditMutationFactory}
     */
    public LabelEditMutationFactory labelEdit() {
        return labelEdit;
    }

    /**
     * Returns the label-association mutation factory.
     *
     * @return the {@link LabelAssociationMutationFactory}
     */
    public LabelAssociationMutationFactory labelAssociation() {
        return labelAssociation;
    }

    /**
     * Returns the label-reordering mutation factory.
     *
     * @return the {@link LabelReorderingMutationFactory}
     */
    public LabelReorderingMutationFactory labelReordering() {
        return labelReordering;
    }

    /**
     * Returns the quick-reply mutation factory.
     *
     * @return the {@link QuickReplyMutationFactory}
     */
    public QuickReplyMutationFactory quickReply() {
        return quickReply;
    }

    /**
     * Returns the chat-assignment mutation factory.
     *
     * @return the {@link ChatAssignmentMutationFactory}
     */
    public ChatAssignmentMutationFactory chatAssignment() {
        return chatAssignment;
    }

    /**
     * Returns the chat-assignment-opened-status mutation factory.
     *
     * @return the {@link ChatAssignmentOpenedStatusMutationFactory}
     */
    public ChatAssignmentOpenedStatusMutationFactory chatAssignmentOpenedStatus() {
        return chatAssignmentOpenedStatus;
    }

    /**
     * Returns the business-broadcast-list mutation factory.
     *
     * @return the {@link BusinessBroadcastListMutationFactory}
     */
    public BusinessBroadcastListMutationFactory businessBroadcastList() {
        return businessBroadcastList;
    }

    /**
     * Returns the favourite-sticker mutation factory.
     *
     * @return the {@link FavoriteStickerMutationFactory}
     */
    public FavoriteStickerMutationFactory favoriteSticker() {
        return favoriteSticker;
    }

    /**
     * Returns the remove-recent-sticker mutation factory.
     *
     * @return the {@link RemoveRecentStickerMutationFactory}
     */
    public RemoveRecentStickerMutationFactory removeRecentSticker() {
        return removeRecentSticker;
    }

    /**
     * Returns the bot-welcome-request mutation factory.
     *
     * @return the {@link BotWelcomeRequestMutationFactory}
     */
    public BotWelcomeRequestMutationFactory botWelcomeRequest() {
        return botWelcomeRequest;
    }

    /**
     * Returns the AI-thread-rename mutation factory.
     *
     * @return the {@link AiThreadRenameMutationFactory}
     */
    public AiThreadRenameMutationFactory aiThreadRename() {
        return aiThreadRename;
    }

    /**
     * Returns the AI-thread-delete mutation factory.
     *
     * @return the {@link AiThreadDeleteMutationFactory}
     */
    public AiThreadDeleteMutationFactory aiThreadDelete() {
        return aiThreadDelete;
    }

    /**
     * Returns the favourites mutation factory.
     *
     * @return the {@link FavoritesMutationFactory}
     */
    public FavoritesMutationFactory favorites() {
        return favorites;
    }

    /**
     * Returns the note-edit mutation factory.
     *
     * @return the {@link NoteEditMutationFactory}
     */
    public NoteEditMutationFactory noteEdit() {
        return noteEdit;
    }

    /**
     * Returns the locale-setting mutation factory.
     *
     * @return the {@link LocaleSettingMutationFactory}
     */
    public LocaleSettingMutationFactory localeSetting() {
        return localeSetting;
    }

    /**
     * Returns the disable-link-previews mutation factory.
     *
     * @return the {@link DisableLinkPreviewsMutationFactory}
     */
    public DisableLinkPreviewsMutationFactory disableLinkPreviews() {
        return disableLinkPreviews;
    }

    /**
     * Returns the time-format mutation factory.
     *
     * @return the {@link TimeFormatMutationFactory}
     */
    public TimeFormatMutationFactory timeFormat() {
        return timeFormat;
    }

    /**
     * Returns the Maiba-AI-features-control mutation factory.
     *
     * @return the {@link MaibaAIFeaturesControlMutationFactory}
     */
    public MaibaAIFeaturesControlMutationFactory maibaAIFeaturesControl() {
        return maibaAIFeaturesControl;
    }

    /**
     * Returns the unarchive-chats-setting mutation factory.
     *
     * @return the {@link UnarchiveChatsSettingMutationFactory}
     */
    public UnarchiveChatsSettingMutationFactory unarchiveChatsSetting() {
        return unarchiveChatsSetting;
    }

    /**
     * Returns the notification-activity-setting mutation factory.
     *
     * @return the {@link NotificationActivitySettingMutationFactory}
     */
    public NotificationActivitySettingMutationFactory notificationActivitySetting() {
        return notificationActivitySetting;
    }

    /**
     * Returns the status-privacy mutation factory.
     *
     * @return the {@link StatusPrivacyMutationFactory}
     */
    public StatusPrivacyMutationFactory statusPrivacy() {
        return statusPrivacy;
    }

    /**
     * Returns the push-name-setting mutation factory.
     *
     * @return the {@link PushNameSettingMutationFactory}
     */
    public PushNameSettingMutationFactory pushNameSetting() {
        return pushNameSetting;
    }

    /**
     * Returns the contact-action mutation factory.
     *
     * @return the {@link ContactActionMutationFactory}
     */
    public ContactActionMutationFactory contactAction() {
        return contactAction;
    }

    /**
     * Returns the call-log mutation factory.
     *
     * @return the {@link CallLogMutationFactory}
     */
    public CallLogMutationFactory callLog() {
        return callLog;
    }

    /**
     * Returns the NUX-action mutation factory.
     *
     * @return the {@link NuxActionMutationFactory}
     */
    public NuxActionMutationFactory nuxAction() {
        return nuxAction;
    }

    /**
     * Returns the always-relay-calls mutation factory.
     *
     * @return the {@link VoipRelayAllCallsMutationFactory}
     */
    public VoipRelayAllCallsMutationFactory voipRelayAllCalls() {
        return voipRelayAllCalls;
    }

    /**
     * Returns the custom-payment-methods mutation factory.
     *
     * @return the {@link CustomPaymentMethodsMutationFactory}
     */
    public CustomPaymentMethodsMutationFactory customPaymentMethods() {
        return customPaymentMethods;
    }

    /**
     * Returns the payment-terms-of-service mutation factory.
     *
     * @return the {@link PaymentTosMutationFactory}
     */
    public PaymentTosMutationFactory paymentTos() {
        return paymentTos;
    }

    /**
     * Returns the CTWA per-customer data-sharing mutation factory.
     *
     * @return the {@link CtwaPerCustomerDataSharingMutationFactory}
     */
    public CtwaPerCustomerDataSharingMutationFactory ctwaPerCustomerDataSharing() {
        return ctwaPerCustomerDataSharing;
    }

    /**
     * Returns the external-web-beta mutation factory.
     *
     * @return the {@link ExternalWebBetaMutationFactory}
     */
    public ExternalWebBetaMutationFactory externalWebBeta() {
        return externalWebBeta;
    }

    /**
     * Returns the private-processing-setting mutation factory.
     *
     * @return the {@link PrivateProcessingSettingMutationFactory}
     */
    public PrivateProcessingSettingMutationFactory privateProcessingSetting() {
        return privateProcessingSetting;
    }

    /**
     * Returns the detected-outcomes-status mutation factory.
     *
     * @return the {@link DetectedOutcomesStatusMutationFactory}
     */
    public DetectedOutcomesStatusMutationFactory detectedOutcomesStatus() {
        return detectedOutcomesStatus;
    }

    /**
     * Returns the interactive-message mutation factory.
     *
     * @return the {@link InteractiveMessageMutationFactory}
     */
    public InteractiveMessageMutationFactory interactiveMessage() {
        return interactiveMessage;
    }

    /**
     * Returns the recent-emoji-weights mutation factory.
     *
     * @return the {@link RecentEmojiWeightsMutationFactory}
     */
    public RecentEmojiWeightsMutationFactory recentEmojiWeights() {
        return recentEmojiWeights;
    }
}
