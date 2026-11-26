package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.TestSignalSession;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryption;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link ChatMessageReceiver} by driving a full encrypt-then-receive cycle: a sender-side
 * {@link MessageEncryption} produces a PKMSG ciphertext, it is wrapped in a synthetic inbound
 * {@code <message>} stanza, and handed to the recipient's {@link ChatMessageReceiver}, which must
 * decrypt, decode the protobuf, and return a populated
 * {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo} carrying the sender's original
 * {@link LinkedMessageContainer}. Both sides share libsignal session state installed by
 * {@link TestSignalSession#establishSession} before the first encrypt.
 */
@DisplayName("ChatMessageReceiver")
class ChatMessageReceiverTest {

    private static final Jid SENDER_PRIMARY = Jid.of("12025550100:0@s.whatsapp.net");
    private static final Jid RECIPIENT_PRIMARY = Jid.of("19254863482:0@s.whatsapp.net");
    private static final Jid RECIPIENT_BARE = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SENDER_BARE = Jid.of("12025550100@s.whatsapp.net");

    @Test
    @DisplayName("PKMSG receive: decrypts to the sender's LinkedMessageContainer and returns a populated ChatMessageInfo")
    void receivePkmsgRoundTrip() {
        var senderStore = MessageFixtures.temporaryStore(SENDER_BARE, null);
        var recipientStore = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        TestSignalSession.establishSession(senderStore, RECIPIENT_PRIMARY, recipientStore);

        var senderContainer = LinkedMessageContainer.of("hello from sender");
        var senderEncryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var payload = senderEncryption.encryptForDevice(
                RECIPIENT_PRIMARY,
                LinkedMessageContainerSpec.encode(senderContainer));

        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0RCV0001")
                .attribute("from", SENDER_PRIMARY)
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("enc")
                        .attribute("v", "2")
                        .attribute("type", "pkmsg")
                        .content(payload.ciphertext())
                        .build())
                .build();

        var recipientDecryption = new MessageDecryption(recipientStore,
                new SignalSessionCipher(recipientStore.signalStore()),
                new SignalGroupCipher(recipientStore.signalStore()),
                new SignalCryptoLocks());
        var receiver = new ChatMessageReceiver(recipientStore, recipientDecryption, TestWamService.create(TestWhatsAppClient.create().withStore(recipientStore)));

        var info = receiver.receive(inbound, SENDER_PRIMARY);

        assertNotNull(info, "ChatMessageReceiver must return a populated ChatMessageInfo for a valid PKMSG");
        assertEquals("3EB0RCV0001", info.key().id().orElseThrow(),
                "message id must round-trip from the inbound stanza");

        var receivedContent = info.message().content();
        var receivedText = assertInstanceOf(ExtendedTextMessage.class, receivedContent,
                "the conversation field decodes into an ExtendedTextMessage on receive");
        assertEquals("hello from sender", receivedText.text().orElseThrow(),
                "decrypted payload bytes equal the sender's plaintext");
    }

    // Pins the WA Web behaviour that the sender keeps emitting PKMSG until the recipient's first
    // ack lands; the send-side mirror is MessageEncryptionTest.typeSwitchesAfterFirstAck.
    @Test
    @DisplayName("PKMSG receive: subsequent decrypts switch to MSG and recover correctly")
    void msgEnvelopeAfterPrekeyConsumed() {
        var senderStore = MessageFixtures.temporaryStore(SENDER_BARE, null);
        var recipientStore = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        TestSignalSession.establishSession(senderStore, RECIPIENT_PRIMARY, recipientStore);
        var senderEncryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var recipientDecryption = new MessageDecryption(recipientStore,
                new SignalSessionCipher(recipientStore.signalStore()),
                new SignalGroupCipher(recipientStore.signalStore()),
                new SignalCryptoLocks());
        var receiver = new ChatMessageReceiver(recipientStore, recipientDecryption, TestWamService.create(TestWhatsAppClient.create().withStore(recipientStore)));

        var firstPayload = senderEncryption.encryptForDevice(
                RECIPIENT_PRIMARY,
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("first")));
        receiver.receive(buildInbound("3EB0RCV0010", "pkmsg", firstPayload.ciphertext()), SENDER_PRIMARY);

        var secondPayload = senderEncryption.encryptForDevice(
                RECIPIENT_PRIMARY,
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("second")));
        var info = receiver.receive(buildInbound("3EB0RCV0011", "pkmsg", secondPayload.ciphertext()), SENDER_PRIMARY);

        var receivedText = (ExtendedTextMessage) info.message().content();
        assertEquals("second", receivedText.text().orElseThrow(),
                "second decrypt must recover the new plaintext");
    }

    private static Stanza buildInbound(String id, String encType, byte[] ciphertext) {
        return new StanzaBuilder()
                .description("message")
                .attribute("id", id)
                .attribute("from", SENDER_PRIMARY)
                // pins a deterministic stanza age so the expired-status branch never fires
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("enc")
                        .attribute("v", "2")
                        .attribute("type", encType)
                        .content(ciphertext)
                        .build())
                .build();
    }

    // Live defect of 2026-11-23: the first message from a sender in a new group came with two encs (a per-device
    // one holding only the sender-key distribution and an SKMSG with the text); the receiver kept the first
    // plaintext and lost the text, because the SKMSG was tried before the distribution was imported.
    @Test
    @DisplayName("Group first message: imports the sender key from the PKMSG and returns the SKMSG content")
    void groupFirstMessageKeepsTheSkmsgContent() {
        var group = Jid.of("120363000000000000@g.us");
        var senderStore = MessageFixtures.temporaryStore(SENDER_BARE, null);
        var recipientStore = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        TestSignalSession.establishSession(senderStore, RECIPIENT_PRIMARY, recipientStore);
        var senderEncryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());

        var distribution = LinkedMessageContainer.of(
                new com.github.auties00.cobalt.wire.linked.message.group.SenderKeyDistributionMessageBuilder()
                        .groupJid(group)
                        .axolotlSenderKeyDistributionMessage(senderEncryption.getSenderKeyBytes(group, SENDER_PRIMARY))
                        .build());
        var perDevice = senderEncryption.encryptForDevice(RECIPIENT_PRIMARY,
                LinkedMessageContainerSpec.encode(distribution));
        var groupText = senderEncryption.encryptForGroup(group, SENDER_PRIMARY,
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("primeira mensagem no grupo")));

        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0GRP0001")
                .attribute("from", group)
                .attribute("participant", SENDER_PRIMARY)
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .content(new StanzaBuilder().description("enc").attribute("v", "2").attribute("type", "pkmsg")
                                .content(perDevice.ciphertext()).build(),
                        new StanzaBuilder().description("enc").attribute("v", "2").attribute("type", "skmsg")
                                .content(groupText.ciphertext()).build())
                .build();

        var recipientDecryption = new MessageDecryption(recipientStore,
                new SignalSessionCipher(recipientStore.signalStore()),
                new SignalGroupCipher(recipientStore.signalStore()),
                new SignalCryptoLocks());
        var receiver = new ChatMessageReceiver(recipientStore, recipientDecryption,
                TestWamService.create(TestWhatsAppClient.create().withStore(recipientStore)));

        var info = receiver.receive(inbound, SENDER_PRIMARY);

        assertNotNull(info, "the first group message must be received");
        var text = assertInstanceOf(ExtendedTextMessage.class, info.message().content(),
                "the content is the SKMSG text, not the empty sender-key container");
        assertEquals("primeira mensagem no grupo", text.text().orElseThrow());
    }

    // Live defect of 2026-11-25: current clients send an edit as a secretEncMessage (MESSAGE_EDIT) encrypted with
    // the edited message's messageSecret; it reached listeners still encrypted and empty.
    @Test
    @DisplayName("Message edit: decrypts the secretEncMessage into a MESSAGE_EDIT protocol message")
    void messageEditIsDecrypted() {
        var senderStore = MessageFixtures.temporaryStore(SENDER_BARE, null);
        var recipientStore = MessageFixtures.temporaryStore(RECIPIENT_BARE, null);
        TestSignalSession.establishSession(senderStore, RECIPIENT_PRIMARY, recipientStore);

        var secret = new byte[32];
        java.util.Arrays.fill(secret, (byte) 7);
        var targetKey = new com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder()
                .id("3EB0ORIGINAL")
                .fromMe(false)
                .parentJid(SENDER_BARE)
                .senderJid(SENDER_BARE)
                .build();
        recipientStore.chatStore().addNewChat(SENDER_BARE).addMessage(
                new com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder()
                        .key(targetKey)
                        .message(LinkedMessageContainer.of("texto errado"))
                        .messageSecret(secret)
                        .build());

        var addon = com.github.auties00.cobalt.message.addon.MessageAddonEncryption.encrypt(
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("texto corrigido")),
                secret, "3EB0ORIGINAL", SENDER_BARE, SENDER_BARE,
                com.github.auties00.cobalt.message.addon.MessageAddonType.MESSAGE_EDIT);
        var edit = new com.github.auties00.cobalt.wire.linked.message.security.SecretEncMessageBuilder()
                .targetMessageKey(targetKey)
                .encPayload(addon.ciphertext())
                .encIv(addon.iv())
                .secretEncType(com.github.auties00.cobalt.wire.linked.message.security.SecretEncMessage.SecretEncType.MESSAGE_EDIT)
                .build();
        var senderEncryption = new MessageEncryption(senderStore,
                new SignalSessionCipher(senderStore.signalStore()),
                new SignalGroupCipher(senderStore.signalStore()),
                new SignalCryptoLocks());
        var payload = senderEncryption.encryptForDevice(RECIPIENT_PRIMARY,
                LinkedMessageContainerSpec.encode(LinkedMessageContainer.of(edit)));

        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0EDIT0001")
                .attribute("from", SENDER_PRIMARY)
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .attribute("edit", "1")
                .content(new StanzaBuilder().description("enc").attribute("v", "2").attribute("type", "pkmsg")
                        .content(payload.ciphertext()).build())
                .build();
        var recipientDecryption = new MessageDecryption(recipientStore,
                new SignalSessionCipher(recipientStore.signalStore()),
                new SignalGroupCipher(recipientStore.signalStore()),
                new SignalCryptoLocks());
        var receiver = new ChatMessageReceiver(recipientStore, recipientDecryption,
                TestWamService.create(TestWhatsAppClient.create().withStore(recipientStore)));

        var info = receiver.receive(inbound, SENDER_PRIMARY);

        assertNotNull(info);
        var protocol = assertInstanceOf(com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage.class,
                info.message().content(), "the edit becomes a protocol message");
        assertEquals(com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage.Type.MESSAGE_EDIT,
                protocol.type().orElseThrow());
        assertEquals("3EB0ORIGINAL", protocol.key().orElseThrow().id().orElseThrow());
        var text = assertInstanceOf(ExtendedTextMessage.class,
                protocol.editedMessage().orElseThrow().content());
        assertEquals("texto corrigido", text.text().orElseThrow());
    }
}
