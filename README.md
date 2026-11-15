<!-- Link + badge definitions (maintained in one place) -->
[upstream]: https://github.com/Auties00/Cobalt
[license]: LICENSE
[cloud-api]: https://developers.facebook.com/docs/whatsapp/cloud-api
[java25]: https://openjdk.org/projects/jdk/25/

[badge-license]: https://img.shields.io/badge/LICENSE-MIT-2f81f7?style=for-the-badge&labelColor=0b0e14
[badge-java]: https://img.shields.io/badge/JAVA-25-f89820?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=0b0e14
[badge-upstream]: https://img.shields.io/badge/FORK%20OF-Auties00%2FCobalt-2f81f7?style=for-the-badge&logo=github&labelColor=0b0e14

<div align="center">

<a href="README.md"><img src="https://flagcdn.com/24x18/us.png" width="24" height="18" alt="English"> <b>English</b></a>
&nbsp;|&nbsp;
<a href="README.pt-BR.md"><img src="https://flagcdn.com/24x18/br.png" width="24" height="18" alt="Português"> Português</a>

<br/>

# C O B A L T - 2

### The Java WhatsApp engine behind Msgforge: WhatsApp Web, Desktop, Mobile and Cloud API for the JVM

<br/>

[![Java][badge-java]][java25]
[![License][badge-license]][license]
[![Upstream][badge-upstream]][upstream]

</div>

## About this repository

**cobalt-2** is an independent continuation of [Cobalt][upstream], the WhatsApp library written by Alessandro Autiero (Auties00) and released under the MIT license. It came from the `jonales/Cobalt` fork and is maintained as the WhatsApp engine of **Msgforge** (`jonales/msgforge-api`).

What changes compared with upstream:

- **Fixes found in live tests.** Most of the work here comes from running Msgforge against real phones: message retry, call rejection, group metadata, video and voice-note sending, device lists and the security-code (ICDC) warning, Signal sessions addressed by LID, and others. The state of each item is tracked in [`docs/engine-review/STATUS.md`](docs/engine-review/STATUS.md), and what still needs a real phone is listed in [`docs/engine-review/VALIDACAO.md`](docs/engine-review/VALIDACAO.md).
- **No Maven Central release.** The fork keeps upstream's coordinates (`com.github.auties00:cobalt-lib:0.1.0`) but is not published anywhere; you build it from source (see [Install](#install)).
- **Separate issue tracker.** Report problems with this fork here, not on the upstream repository.

Credit for the design and for most of the code belongs to the upstream project. This fork does not speak for it.

## Disclaimer

Cobalt is an independent, unofficial project. It is not affiliated with, authorized, maintained, sponsored, or endorsed by WhatsApp LLC or Meta Platforms, Inc., or any of their affiliates. All product and company names are trademarks of their respective holders.

The reverse-engineered Linked transport talks to WhatsApp's private protocol. WhatsApp does not support third-party clients, so this transport may breach its Terms of Service, lead to account restrictions, or stop working at any time. Good-faith use of your own account carries little risk, but sending unsolicited or automated bulk messages, especially from new numbers, is exactly what WhatsApp's anti-spam systems act on. Use Cobalt responsibly and at your own risk. For commercial messaging, prefer the official Cloud API.

---

## What is Cobalt

Cobalt lets a JVM application be a WhatsApp client. It supports two completely different ways of reaching WhatsApp, behind one sealed client API:

- **The Linked client** is a clean-room reimplementation of the real WhatsApp Web, Desktop and Mobile apps. No browser, no Selenium, no bridge process.
- **The Cloud client** drives Meta's official, hosted [WhatsApp Cloud API][cloud-api] over plain HTTPS, with a built-in webhook server for inbound traffic.

> **_IMPORTANT:_** Cobalt is pre-`1.0`: expect breaking changes until the API is frozen.

---

## Install

Cobalt targets **Java 25** and is built with Maven. Since this fork is not published, install it into your local Maven repository:

```bash
git clone https://github.com/jonales/cobalt-2.git
cd cobalt-2
mvn -DskipTests install -pl modules/lib -am
```

Then depend on it as usual.

**Maven**

```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>cobalt-lib</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle (Kotlin DSL)** (needs `mavenLocal()` in `repositories`)

```kotlin
implementation("com.github.auties00:cobalt-lib:0.1.0")
```

To build a fixed revision (as Msgforge does, in `scripts/install-cobalt.sh`), check out the commit you want before `mvn install`.

### Native libraries

Calls and media use native components (FFmpeg, codecs, crypto). The binaries for every platform are versioned in this repository under `modules/lib/natives/bin/<classifier>/`, but the default `cobalt-lib` jar does not include them: on first use the loader downloads the binary for the host from this repository (`raw.githubusercontent.com/jonales/cobalt-2/<commit>/...`), at the commit recorded in `META-INF/native-checksums.json`, checks its SHA-256 and caches it under `~/.cobalt/cache/natives/`. Set `-Dcobalt.natives.cache=<path>` to move the cache, and `-Dcobalt.natives.repo=<url>` to download from a mirror that serves the same `<commit>/<path>` layout. The `build-natives` workflow rebuilds the binaries and the manifest in this repository, so the recorded commit always exists here.

To avoid the download (servers without internet access, Docker images), build a jar with the native included, from `modules/lib`:

```bash
# classifier jar: cobalt-lib-0.1.0-natives-<classifier>.jar, next to the main jar
mvn -Pnatives -Dnatives.classifier=linux-x86_64 package

# self-contained fat jar: cobalt-lib-0.1.0-uber-<classifier>.jar (dependencies + native)
mvn -Puber-jar-linux-x86_64 package
```

Classifiers: `linux-x86_64`, `linux-aarch64`, `darwin-x86_64`, `darwin-aarch64`, `windows-x86_64`, `windows-aarch64`.

---

## Quickstart

### Linked, log in with a QR code

Scan the printed QR from your phone (WhatsApp, then Linked devices), then send and receive in real time.

```java
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.QrCode;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

void main() throws Exception {
    WhatsAppClient.builder()
            .linkedApi()
            .webClient()                               // session persisted under ~/.cobalt/proto
            .createConnection()                        // a fresh connection
            .name("Cobalt Bot")                        // the linked-device name shown in WhatsApp
            .unregistered(QrCode.toTerminal())         // print the QR to scan
            .addLoggedInListener(api -> System.out.println("Connected"))
            .addNewMessageListener((api, message) -> {
                if (!message.key().fromMe()) {
                    message.key()
                            .parentJid()
                            .ifPresent(chat -> api.sendMessage(chat, LinkedMessageContainer.of("Got your message")));
                }
            })
            .connect()                                 // returns once the socket is live
            .waitForDisconnection();                   // park this thread for the session
}
```

Prefer a pairing code over a QR? Swap the terminal stage (and import `...Web.PairingCode`):

```java
.unregistered(5511999999999L, PairingCode.toTerminal())   // phone with country code, as a long
```

### Cloud API

For a Business Platform number, supply your credentials and let Cobalt run the webhook server for inbound traffic.

```java
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

void main() throws Exception {
    WhatsAppClient.builder()
            .cloudApi()
            .loadConnection("EAAB...", "123456789")    // system-user token + phone number id (required)
            .appSecret("...")                          // verifies inbound webhook signatures
            .webhook("my-verify-token", 8080)          // start the webhook server on :8080
            .build()
            .addLoggedInListener(api -> System.out.println("Connected"))
            .addNewMessageListener((api, message) -> {
                message.key()
                        .parentJid()
                        .ifPresent(chat -> api.sendMessage(chat, LinkedMessageContainer.of("Got your message")));
            })
            .connect()                                 // validates the token, starts the webhook
            .waitForDisconnection();                   // park this thread for the session
}
```

Both examples are compiled against the current code.

---

## The quickstart, explained

The QR example above is a complete client in one expression. Walking it line by line touches every concept Cobalt relies on.

### `WhatsAppClient.builder()`

`WhatsAppClient` is a sealed interface with exactly two implementations, so this is the fork in the road:

- `.linkedApi()` leads to the reverse-engineered transport.
- `.cloudApi()` leads to the official Cloud API.

Operations that exist on both transports (connect, send, react, mark read, typing, block) live on the shared type, so code written against `WhatsAppClient` works with either.

### `.webClient()`

Picks the flavour of the Linked client and where the session lives:

- `webClient()` pairs as a companion device, like WhatsApp Web. With no argument the session is **persisted to disk** under `~/.cobalt/proto/`; pass `LinkedWhatsAppStoreFactory.temporary()` to keep it in memory only, or `LinkedWhatsAppStoreFactory.persistent(path)` to choose the folder.
- `mobileClient()` registers as a standalone phone (iOS or Android) rather than a companion.
- `customClient()` lets you supply your own store implementation.

The store is the session's single source of truth: Signal keys, your account identity, contacts, chats, messages, sync state, settings. The persistent variant snapshots metadata as protobuf (`store.proto`) and keeps messages in an embedded H2 MVStore file (`messages.mv`), pure Java with no native dependency. You query it at runtime through typed sub-stores:

```java
Optional<Chat> chat = client.store().chatStore().findChatByJid(someJid);
```

### `.createConnection()`

A connection is one registered session, and this stage decides where it comes from:

- `createConnection()` starts a fresh, unregistered session. It creates a new one on **every** run, so use it only for the first login.
- `loadLatestConnection()`, `loadConnection(uuid)` and `loadConnection(phoneNumber)` reopen a persisted one and return an `Optional`.
- `loadLatestOrCreateConnection()` and `loadOrCreateConnection(uuid)` reopen it or fall back to a new one.
- `createConnection(sixParts)` imports credentials from a portable six-part key string.

Reusing a persisted connection is why a client does not show the QR twice.

### `.name("Cobalt Bot")`

The options stage configures the session before registration:

- `name(...)` is the device name shown under Linked devices.
- `proxy(...)` routes the connection through a proxy.
- `device(...)` is the device Cobalt presents itself as.
- `clientVersion(...)` pins the WhatsApp client version.
- `releaseChannel(...)` selects RELEASE or BETA; BETA unlocks Web calling.
- `fullHistory()`, `defaultHistory()` and `discardHistory()` control how much history the phone syncs.
- `errorHandler(...)` sets the recovery policy.

The error handler is Cobalt's whole recovery policy: every failure is a typed exception in a sealed `WhatsAppException` hierarchy, and your `WhatsAppLinkedClientErrorHandler` maps it to one of `DISCARD`, `DISCONNECT`, `RECONNECT`, `LOG_OUT`, or `BAN`. Nothing is hardcoded; the default handler logs and keeps the client alive.

### `.unregistered(QrCode.toTerminal())`

The terminal stage: it declares how this session proves itself to WhatsApp and produces the actual `LinkedWhatsAppClient`. A verification handler decides how the proof reaches the user:

- `unregistered(QrCode.toTerminal())` prints the QR to scan; `QrCode.toFile(...)` writes it to disk instead.
- `unregistered(phone, PairingCode.toTerminal())` prints a pairing code to type into the phone.
- `registered()` skips verification by reusing the persisted credentials (it returns an `Optional`).
- On the mobile flavour, `register(phone, Mobile.sms(...))` runs the OTP ceremony; `Mobile.call(...)` and `Mobile.whatsapp(...)` cover the other channels.

### `.addNewMessageListener(...)`

Everything WhatsApp pushes to the client surfaces as an event, and there are two ways to subscribe:

- One chainable registrar per event: `addLoggedInListener`, `addNewMessageListener`, `addCallListener`, `addDisconnectedListener`, and dozens more.
- One class implementing `LinkedWhatsAppClientListener` (or `CloudWhatsAppClientListener`), which has a no-op default for every event, registered once with `addListener(...)`.

Each listener invocation runs on its own virtual thread, so a slow listener never stalls the connection; every lambda also receives the client instance (`api`), so listeners need no outside references.

### `message.key()`

Inside the listener you meet the message model, three types that appear in every messaging call:

- `MessageInfo` is a message as it exists in a chat: content plus metadata (sender, timestamp, status).
- `MessageKey` is its identity: the chat (`parentJid()`), the author (`senderJid()`), the id, and `fromMe()`, which the example checks to avoid replying to itself. Keys are what you pass to react, edit, delete, star, and pin.
- `LinkedMessageContainer` is the content itself, a one-of where exactly one message type (text, image, poll, ...) is set. It is also the model the Cloud client translates to its REST shape.

`parentJid()` returns a `Jid`, the address of any entity: a user, a group, a newsletter, a broadcast list. Build one with `Jid.of("15551234567")`. Most methods actually accept a `JidProvider`, implemented by `Jid`, `Chat`, `Contact` and `Newsletter` alike, so a resolved object can be passed wherever an address is expected.

### `api.sendMessage(chat, LinkedMessageContainer.of("..."))`

`LinkedMessageContainer.of(...)` wraps content for sending; the `String` overload is plain text. Richer content is built first, because every model in Cobalt follows the same convention: a generated `<TypeName>Builder` constructs it, fields are read with `field()` accessors (not `getField()`), absent fields return `Optional`, and lists are unmodifiable.

```java
var location = new LocationMessageBuilder()
        .degreesLatitude(37.386051)
        .degreesLongitude(-122.083855)
        .name("Meet here")
        .build();
api.sendMessage(chat, LinkedMessageContainer.of(location));
```

`sendMessage` returns the `MessageKey` of what was sent, closing the loop: hold on to it to edit, delete, or react later.

> **_IMPORTANT:_** `sendMessage` is a plain blocking call, like every operation in Cobalt: it returns the result or throws, and there is no `CompletableFuture` anywhere in the API. Blocking is cheap because everything runs on virtual threads; this listener is already on its own, so a slow send never stalls the connection. To run operations in parallel, start your own virtual threads.

### `.connect()`

Opens the encrypted socket, runs the Noise handshake, and starts processing traffic; it returns as soon as the connection is live.

### `.waitForDisconnection()`

`connect()` does not block for the session's lifetime, so without this line the main thread would fall through and the JVM could exit. `waitForDisconnection()` parks the calling thread until a terminal disconnect: transient network drops are reconnected automatically and do not wake it. The reason is reported to `addDisconnectedListener` as one of `DISCONNECTED`, `RECONNECTING`, `LOGGED_OUT`, or `BANNED`. Deliberate teardown:

- `disconnect()` closes the connection; the session stays valid.
- `reconnect()` tears down and reconnects in one call.
- `logout()` (Linked client) closes the connection and invalidates the session.

### The Cloud variant

The Cloud quickstart follows the same shape with a different first stage. There is no QR and no Signal state; the Cloud store is small (access token, phone-number id, WABA id, app secret, webhook config) and persistent by default:

- `loadConnection(accessToken, phoneNumberId)` loads or creates the connection with the required credentials and opens the configuration stage; `loadConnection(phoneNumberId)` and `loadLatestConnection()` resume a saved one.
- `appSecret(...)` lets Cobalt sign requests and verify webhook payloads.
- `webhook(verifyToken, port)` starts the built-in webhook server when you connect; it answers Meta's verification handshake and validates the `X-Hub-Signature-256` of every delivery before dispatching events.
- `build()` produces the `CloudWhatsAppClient`.

Listeners register exactly as on the Linked client, and `sendMessage` takes the same `LinkedMessageContainer`, so everything from the message model onward carries over unchanged.

---

## Development

- Build: `mvn compile` from the root (Java 25). The module layout and coding rules are in [`CLAUDE.md`](CLAUDE.md).
- Engine review: [`docs/engine-review/STATUS.md`](docs/engine-review/STATUS.md) lists what is fixed, validated, pending, or still a hypothesis; update it with every engine fix.
- Live validation needs real phones and does not run in the cloud: see [`docs/engine-review/VALIDACAO.md`](docs/engine-review/VALIDACAO.md).

### Tooling inherited from upstream

Parts of Cobalt are regenerated by deterministic extractors that run against the real WhatsApp Web client (each one is a Node project: `npm install`, `npm run build`, `npm start`):

- [tools/web/proto-extractor](tools/web/proto-extractor): extracts every protobuf message and enum into `whatsapp.proto`, the reference for the hand-written models in [modules/wire](modules/wire).
- [tools/web/ab-props-codegen](tools/web/ab-props-codegen): regenerates the `ABProp` constants in `modules/wire/wire-linked`.
- [tools/web/graphql-extractor](tools/web/graphql-extractor): dumps the spec of every GraphQL/MEX operation, used to validate the models in `modules/wire/wire-graphql`.
- [tools/web/wam-codegen](tools/web/wam-codegen): regenerates the WAM telemetry events in `modules/wire/wire-wam`, consumed by the [modules/wam-core](modules/wam-core) processor.
- [tools/web/mcp-server](tools/web/mcp-server): the MCP server for reverse engineering and live WhatsApp Web sessions (`npm install`, `npx playwright install chromium`, `npm run build`, `npm start`).
- [.claude/commands/validate.md](.claude/commands/validate.md): the experimental `/validate` command, which compares Cobalt modules with their WhatsApp counterparts. It is expensive and has never been run end to end.

---

## Documentation

Every member of the library, public and internal alike, carries Javadoc, so this README only covers the essentials. Generate it locally with `mvn javadoc:javadoc -pl modules/lib` after an `install`.
