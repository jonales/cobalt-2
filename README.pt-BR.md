<!-- Definições de links e badges (mantidas num só lugar) -->
[upstream]: https://github.com/Auties00/Cobalt
[license]: LICENSE
[cloud-api]: https://developers.facebook.com/docs/whatsapp/cloud-api
[java25]: https://openjdk.org/projects/jdk/25/

[badge-license]: https://img.shields.io/badge/LICEN%C3%87A-MIT-2f81f7?style=for-the-badge&labelColor=0b0e14
[badge-java]: https://img.shields.io/badge/JAVA-25-f89820?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=0b0e14
[badge-upstream]: https://img.shields.io/badge/FORK%20DE-Auties00%2FCobalt-2f81f7?style=for-the-badge&logo=github&labelColor=0b0e14

<div align="center">

<a href="README.md"><img src="https://flagcdn.com/24x18/us.png" width="24" height="18" alt="English"> English</a>
&nbsp;|&nbsp;
<a href="README.pt-BR.md"><img src="https://flagcdn.com/24x18/br.png" width="24" height="18" alt="Português"> <b>Português</b></a>

<br/>

# C O B A L T - 2

### O motor de WhatsApp em Java do Msgforge: WhatsApp Web, Desktop, Mobile e Cloud API na JVM

<br/>

[![Java][badge-java]][java25]
[![Licença][badge-license]][license]
[![Upstream][badge-upstream]][upstream]

</div>

## Sobre este repositório

O **cobalt-2** é uma continuação independente do [Cobalt][upstream], a biblioteca de WhatsApp escrita por Alessandro Autiero (Auties00) e publicada sob a licença MIT. Ele saiu do fork `jonales/Cobalt` e é mantido como o motor de WhatsApp do **Msgforge** (`jonales/msgforge-api`).

O que muda em relação ao original:

- **Correções achadas em testes ao vivo.** A maior parte do trabalho aqui vem de rodar o Msgforge com celulares de verdade: reenvio de mensagem (retry), recusa de chamada, metadados de grupo, envio de vídeo e de nota de voz, lista de aparelhos e o aviso de código de segurança (ICDC), sessões Signal pelo LID, entre outras. O estado de cada item fica em [`docs/engine-review/STATUS.md`](docs/engine-review/STATUS.md), e o que ainda precisa de celular está em [`docs/engine-review/VALIDACAO.md`](docs/engine-review/VALIDACAO.md).
- **Sem versão no Maven Central.** O fork mantém as coordenadas do original (`com.github.auties00:cobalt-lib:0.1.0`), mas não é publicado em lugar nenhum; você compila a partir do código (veja [Instalação](#instalação)).
- **Issues separadas.** Problemas deste fork vão para este repositório, não para o original.

O crédito pelo desenho e pela maior parte do código é do projeto original. Este fork não fala em nome dele.

## Aviso

O Cobalt é um projeto independente e não oficial. Não tem ligação, autorização, manutenção, patrocínio nem endosso do WhatsApp LLC, da Meta Platforms, Inc. ou de empresas ligadas a elas. Os nomes de produtos e empresas são marcas dos seus donos.

O transporte Linked, feito por engenharia reversa, fala o protocolo privado do WhatsApp. O WhatsApp não aceita clientes de terceiros, então esse transporte pode violar os Termos de Serviço, causar restrições na conta ou parar de funcionar a qualquer momento. Usar a própria conta de boa-fé tem pouco risco, mas mandar mensagens em massa não solicitadas ou automáticas, principalmente de números novos, é exatamente o que o antispam do WhatsApp pune. Use o Cobalt com responsabilidade e por sua conta e risco. Para mensagens comerciais, prefira a Cloud API oficial.

---

## O que é o Cobalt

O Cobalt faz de uma aplicação JVM um cliente de WhatsApp. Ele tem dois jeitos completamente diferentes de chegar ao WhatsApp, atrás de uma única API de cliente selada:

- **O cliente Linked** é uma reimplementação limpa dos apps reais WhatsApp Web, Desktop e Mobile. Sem navegador, sem Selenium, sem processo intermediário.
- **O cliente Cloud** usa a [WhatsApp Cloud API][cloud-api] oficial, hospedada pela Meta, por HTTPS comum, com um servidor de webhook embutido para o que chega.

> **_IMPORTANTE:_** o Cobalt ainda não chegou à `1.0`: espere mudanças incompatíveis até a API ser congelada.

---

## Instalação

O Cobalt usa **Java 25** e é compilado com Maven. Como este fork não é publicado, instale no seu repositório Maven local:

```bash
git clone https://github.com/jonales/cobalt-2.git
cd cobalt-2
mvn -DskipTests install -pl modules/lib -am
```

Depois declare a dependência normalmente.

**Maven**

```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>cobalt-lib</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle (Kotlin DSL)** (precisa de `mavenLocal()` em `repositories`)

```kotlin
implementation("com.github.auties00:cobalt-lib:0.1.0")
```

Para compilar uma revisão fixa (como o Msgforge faz em `scripts/install-cobalt.sh`), faça checkout do commit desejado antes do `mvn install`.

### Bibliotecas nativas

Chamadas e mídia usam componentes nativos (FFmpeg, codecs, criptografia). Os binários de todas as plataformas estão versionados neste repositório em `modules/lib/natives/bin/<classifier>/`, mas o jar padrão do `cobalt-lib` não os inclui: no primeiro uso, o carregador baixa o binário da máquina do GitHub do projeto original, no commit registrado em `META-INF/native-checksums.json`, confere o SHA-256 e guarda em `~/.cobalt/cache/natives/`. Use `-Dcobalt.natives.cache=<caminho>` para mudar o cache.

Para evitar o download (servidores sem internet, imagens Docker), gere um jar com o nativo incluído, a partir de `modules/lib`:

```bash
# jar por classifier: cobalt-lib-0.1.0-natives-<classifier>.jar, ao lado do jar principal
mvn -Pnatives -Dnatives.classifier=linux-x86_64 package

# fat jar autossuficiente: cobalt-lib-0.1.0-uber-<classifier>.jar (dependências + nativo)
mvn -Puber-jar-linux-x86_64 package
```

Classifiers: `linux-x86_64`, `linux-aarch64`, `darwin-x86_64`, `darwin-aarch64`, `windows-x86_64`, `windows-aarch64`.

---

## Início rápido

### Linked, login com QR code

Escaneie o QR impresso com o celular (WhatsApp, depois Aparelhos conectados) e envie e receba em tempo real.

```java
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.QrCode;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

void main() throws Exception {
    WhatsAppClient.builder()
            .linkedApi()
            .webClient()                               // sessão salva em ~/.cobalt/proto
            .createConnection()                        // uma conexão nova
            .name("Cobalt Bot")                        // nome do aparelho conectado no WhatsApp
            .unregistered(QrCode.toTerminal())         // imprime o QR para escanear
            .addLoggedInListener(api -> System.out.println("Conectado"))
            .addNewMessageListener((api, message) -> {
                if (!message.key().fromMe()) {
                    message.key()
                            .parentJid()
                            .ifPresent(chat -> api.sendMessage(chat, LinkedMessageContainer.of("Recebi sua mensagem")));
                }
            })
            .connect()                                 // volta assim que o socket está no ar
            .waitForDisconnection();                   // segura esta thread durante a sessão
}
```

Prefere código de pareamento em vez de QR? Troque a etapa final (e importe `...Web.PairingCode`):

```java
.unregistered(5511999999999L, PairingCode.toTerminal())   // telefone com DDI, como long
```

### Cloud API

Para um número da Business Platform, informe as credenciais e deixe o Cobalt rodar o servidor de webhook para o que chega.

```java
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

void main() throws Exception {
    WhatsAppClient.builder()
            .cloudApi()
            .loadConnection("EAAB...", "123456789")    // token de system user + id do número (obrigatórios)
            .appSecret("...")                          // confere a assinatura dos webhooks
            .webhook("my-verify-token", 8080)          // sobe o servidor de webhook na porta 8080
            .build()
            .addLoggedInListener(api -> System.out.println("Conectado"))
            .addNewMessageListener((api, message) -> {
                message.key()
                        .parentJid()
                        .ifPresent(chat -> api.sendMessage(chat, LinkedMessageContainer.of("Recebi sua mensagem")));
            })
            .connect()                                 // valida o token e sobe o webhook
            .waitForDisconnection();                   // segura esta thread durante a sessão
}
```

Os dois exemplos compilam com o código atual.

---

## O início rápido, explicado

O exemplo do QR é um cliente completo numa expressão só. Passar por ele linha a linha mostra todos os conceitos do Cobalt.

### `WhatsAppClient.builder()`

`WhatsAppClient` é uma interface selada com exatamente duas implementações, então aqui o caminho se divide:

- `.linkedApi()` leva ao transporte de engenharia reversa.
- `.cloudApi()` leva à Cloud API oficial.

As operações que existem nos dois transportes (conectar, enviar, reagir, marcar como lida, digitando, bloquear) ficam no tipo comum, então código escrito contra `WhatsAppClient` funciona com qualquer um.

### `.webClient()`

Escolhe o sabor do cliente Linked e onde a sessão fica:

- `webClient()` pareia como aparelho conectado, como o WhatsApp Web. Sem argumento, a sessão é **salva em disco** em `~/.cobalt/proto/`; passe `LinkedWhatsAppStoreFactory.temporary()` para mantê-la só em memória, ou `LinkedWhatsAppStoreFactory.persistent(caminho)` para escolher a pasta.
- `mobileClient()` registra como um celular independente (iOS ou Android) em vez de aparelho conectado.
- `customClient()` permite usar sua própria implementação de store.

O store é a fonte única da sessão: chaves Signal, identidade da conta, contatos, conversas, mensagens, estado de sincronização e configurações. A versão persistente grava os metadados em protobuf (`store.proto`) e as mensagens num arquivo H2 MVStore embutido (`messages.mv`), Java puro, sem dependência nativa. Você consulta o store em tempo de execução pelos sub-stores tipados:

```java
Optional<Chat> chat = client.store().chatStore().findChatByJid(someJid);
```

### `.createConnection()`

Uma conexão é uma sessão registrada, e esta etapa decide de onde ela vem:

- `createConnection()` cria uma sessão nova, não registrada. Ela cria uma nova a **cada** execução, então use só no primeiro login.
- `loadLatestConnection()`, `loadConnection(uuid)` e `loadConnection(telefone)` reabrem uma sessão salva e devolvem um `Optional`.
- `loadLatestOrCreateConnection()` e `loadOrCreateConnection(uuid)` reabrem a sessão ou criam uma nova se não houver.
- `createConnection(sixParts)` importa credenciais de uma chave portátil de seis partes.

É por reusar a conexão salva que o cliente não mostra o QR duas vezes.

### `.name("Cobalt Bot")`

A etapa de opções configura a sessão antes do registro:

- `name(...)` é o nome do aparelho em Aparelhos conectados.
- `proxy(...)` passa a conexão por um proxy.
- `device(...)` é o aparelho que o Cobalt diz ser.
- `clientVersion(...)` fixa a versão do cliente WhatsApp.
- `releaseChannel(...)` escolhe RELEASE ou BETA; BETA libera chamadas no Web.
- `fullHistory()`, `defaultHistory()` e `discardHistory()` controlam quanto histórico o celular sincroniza.
- `errorHandler(...)` define a política de recuperação.

O tratador de erros é toda a política de recuperação do Cobalt: cada falha é uma exceção tipada da hierarquia selada `WhatsAppException`, e o seu `WhatsAppLinkedClientErrorHandler` a converte em `DISCARD`, `DISCONNECT`, `RECONNECT`, `LOG_OUT` ou `BAN`. Nada é fixo no código; o tratador padrão registra o erro e mantém o cliente vivo.

### `.unregistered(QrCode.toTerminal())`

A etapa final: declara como a sessão se prova para o WhatsApp e produz o `LinkedWhatsAppClient`. Um tratador de verificação decide como a prova chega ao usuário:

- `unregistered(QrCode.toTerminal())` imprime o QR; `QrCode.toFile(...)` grava em arquivo.
- `unregistered(telefone, PairingCode.toTerminal())` imprime um código de pareamento para digitar no celular.
- `registered()` pula a verificação e reusa as credenciais salvas (devolve um `Optional`).
- No sabor mobile, `register(telefone, Mobile.sms(...))` faz a verificação por código; `Mobile.call(...)` e `Mobile.whatsapp(...)` cobrem os outros canais.

### `.addNewMessageListener(...)`

Tudo o que o WhatsApp manda para o cliente vira um evento, e há dois jeitos de assinar:

- Um registrador encadeável por evento: `addLoggedInListener`, `addNewMessageListener`, `addCallListener`, `addDisconnectedListener` e dezenas de outros.
- Uma classe que implementa `LinkedWhatsAppClientListener` (ou `CloudWhatsAppClientListener`), com um padrão vazio para cada evento, registrada uma vez com `addListener(...)`.

Cada chamada de listener roda na sua própria thread virtual, então um listener lento nunca trava a conexão; cada lambda também recebe a instância do cliente (`api`), então não precisa de referências de fora.

### `message.key()`

Dentro do listener aparece o modelo de mensagem, três tipos presentes em toda operação de mensagem:

- `MessageInfo` é a mensagem como existe na conversa: conteúdo mais metadados (remetente, horário, status).
- `MessageKey` é a identidade dela: a conversa (`parentJid()`), o autor (`senderJid()`), o id e `fromMe()`, que o exemplo confere para não responder a si mesmo. É a chave que você passa para reagir, editar, apagar, favoritar e fixar.
- `LinkedMessageContainer` é o conteúdo em si, um one-of em que exatamente um tipo de mensagem (texto, imagem, enquete, ...) está preenchido. É também o modelo que o cliente Cloud converte para o formato REST.

`parentJid()` devolve um `Jid`, o endereço de qualquer entidade: usuário, grupo, canal, lista de transmissão. Crie um com `Jid.of("15551234567")`. A maioria dos métodos aceita um `JidProvider`, implementado por `Jid`, `Chat`, `Contact` e `Newsletter`, então um objeto já resolvido serve onde um endereço é esperado.

### `api.sendMessage(chat, LinkedMessageContainer.of("..."))`

`LinkedMessageContainer.of(...)` embrulha o conteúdo para envio; a versão com `String` é texto puro. Conteúdo mais rico é montado antes, porque todo modelo do Cobalt segue a mesma convenção: um `<NomeDoTipo>Builder` gerado o constrói, os campos são lidos com acessores `campo()` (não `getCampo()`), campos ausentes devolvem `Optional` e listas são imutáveis.

```java
var location = new LocationMessageBuilder()
        .degreesLatitude(37.386051)
        .degreesLongitude(-122.083855)
        .name("Encontro aqui")
        .build();
api.sendMessage(chat, LinkedMessageContainer.of(location));
```

`sendMessage` devolve a `MessageKey` do que foi enviado, fechando o ciclo: guarde-a para editar, apagar ou reagir depois.

> **_IMPORTANTE:_** `sendMessage` é uma chamada bloqueante comum, como toda operação do Cobalt: devolve o resultado ou lança exceção, e não existe `CompletableFuture` em lugar nenhum da API. Bloquear é barato porque tudo roda em threads virtuais; este listener já está na sua própria, então um envio lento não trava a conexão. Para rodar operações em paralelo, crie suas próprias threads virtuais.

### `.connect()`

Abre o socket criptografado, faz o handshake Noise e começa a processar o tráfego; volta assim que a conexão está no ar.

### `.waitForDisconnection()`

`connect()` não bloqueia durante a sessão, então sem esta linha a thread principal terminaria e a JVM poderia sair. `waitForDisconnection()` segura a thread até uma desconexão definitiva: quedas passageiras de rede são reconectadas sozinhas e não a acordam. O motivo chega ao `addDisconnectedListener` como `DISCONNECTED`, `RECONNECTING`, `LOGGED_OUT` ou `BANNED`. Para encerrar de propósito:

- `disconnect()` fecha a conexão; a sessão continua válida.
- `reconnect()` derruba e reconecta numa chamada só.
- `logout()` (cliente Linked) fecha a conexão e invalida a sessão.

### A variante Cloud

O início rápido da Cloud tem o mesmo formato com uma primeira etapa diferente. Não há QR nem estado Signal; o store da Cloud é pequeno (token de acesso, id do número, id da WABA, app secret, configuração do webhook) e persistente por padrão:

- `loadConnection(accessToken, phoneNumberId)` carrega ou cria a conexão com as credenciais obrigatórias e abre a etapa de configuração; `loadConnection(phoneNumberId)` e `loadLatestConnection()` retomam uma salva.
- `appSecret(...)` permite ao Cobalt assinar requisições e conferir os webhooks.
- `webhook(verifyToken, porta)` sobe o servidor de webhook ao conectar; ele responde à verificação da Meta e confere o `X-Hub-Signature-256` de cada entrega antes de disparar os eventos.
- `build()` produz o `CloudWhatsAppClient`.

Os listeners são registrados igual ao cliente Linked, e `sendMessage` recebe o mesmo `LinkedMessageContainer`, então tudo do modelo de mensagem em diante vale sem mudança.

---

## Desenvolvimento

- Build: `mvn compile` na raiz (Java 25). A organização dos módulos e as regras de código estão no [`CLAUDE.md`](CLAUDE.md).
- Revisão do motor: [`docs/engine-review/STATUS.md`](docs/engine-review/STATUS.md) lista o que está corrigido, validado, pendente ou ainda é hipótese; atualize-o junto com cada correção do motor.
- A validação ao vivo precisa de celulares de verdade e não roda na nuvem: veja [`docs/engine-review/VALIDACAO.md`](docs/engine-review/VALIDACAO.md).

### Ferramentas herdadas do original

Partes do Cobalt são regeneradas por extratores determinísticos que rodam contra o WhatsApp Web real (cada um é um projeto Node: `npm install`, `npm run build`, `npm start`):

- [tools/web/proto-extractor](tools/web/proto-extractor): extrai todas as mensagens e enums protobuf para `whatsapp.proto`, a referência dos modelos escritos à mão em [modules/wire](modules/wire).
- [tools/web/ab-props-codegen](tools/web/ab-props-codegen): regenera as constantes `ABProp` em `modules/wire/wire-linked`.
- [tools/web/graphql-extractor](tools/web/graphql-extractor): exporta a especificação de cada operação GraphQL/MEX, usada para validar os modelos de `modules/wire/wire-graphql`.
- [tools/web/wam-codegen](tools/web/wam-codegen): regenera os eventos de telemetria WAM em `modules/wire/wire-wam`, consumidos pelo processador de [modules/wam-core](modules/wam-core).
- [tools/web/mcp-server](tools/web/mcp-server): o servidor MCP de engenharia reversa e de sessões ao vivo do WhatsApp Web (`npm install`, `npx playwright install chromium`, `npm run build`, `npm start`).
- [.claude/commands/validate.md](.claude/commands/validate.md): o comando experimental `/validate`, que compara os módulos do Cobalt com os equivalentes do WhatsApp. É caro e nunca rodou de ponta a ponta.

---

## Documentação

Todo membro da biblioteca, público ou interno, tem Javadoc, então este README cobre só o essencial. Gere localmente com `mvn javadoc:javadoc -pl modules/lib` depois de um `install`.
