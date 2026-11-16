# Revisão do motor cobalt-2: ledger de posição e validação

Fonte única de "onde estamos" na revisão/refatoração do motor. Cada item tem estado,
evidência (arquivo:linha, teste ou consulta ao grafo) e próximo passo. Atualize este
arquivo e registre a memória Graphify correspondente a cada achado.

- Repositório: `jonales/cobalt-2` (base `3a53a668`, 2026-11-12, vinda do repositório arquivado `jonales/Cobalt`). Os números de PR abaixo são do `cobalt-2`, salvo quando indicado `jonales/Cobalt#N`.
- Build: Java 25 (`openjdk-25-jdk-headless`), `mvn -DskipTests install -pl modules/lib -am` OK
- Grafo: `graphify update modules` -> `modules/graphify-out/` (83k nós, 221k arestas; `graph.json` não versionado)
- Conferência de 2026-11-16 no `master` (`2cac1f66`): os 35 testes das correções de jonales/Cobalt#3 e #4 e dos PRs #1 a #4 passam (`DeviceListMetadataWireTest`, `DeviceUSyncKeyIndexTest`, `DeviceSyncedListTimestampTest`, `SignalStoreLidAddressTest`, `ChatMessageReceiverLidTest`, `DeviceServiceTest`, `PttPipelineGranuleTest`, `AudioPipelineFrameSizeTest`, `ImagePipelineTest`, `VideoPipelineEncoderTest`, `RetryTargetChatTest`, `SuccessStreamHandlerCredentialsRefreshTest`, `RejectCreatorTest`, `RejectStanzaWireTest`).
- **Ainda abertos para a 1ª versão:** 1.2, 1.3 e 2.3 (sem mudança no código) e 3.6 (mapeamento MEX de grupo). Os itens 3.7 a 3.10 (vídeo, foto da conta, status de contato e logout) foram validados ao vivo em 2026-11-15. Roteiro em `docs/engine-review/VALIDACAO.md`.
- Memórias Graphify: `modules/graphify-out/memory/` (versionadas); consolidar com `graphify reflect --memory-dir modules/graphify-out/memory`

## Legenda

| Estado | Significado |
|---|---|
| CORRIGIDO | Mudança aplicada, compilada e coberta por teste |
| VALIDADO | Analisado; comportamento correto, nada a fazer |
| PENDENTE | Problema confirmado, ainda sem correção |
| HIPÓTESE | Suspeita, falta evidência (log/captura) |

## Referência de comparação

Os testes do usuário compararam o motor com o **whatsmeow** (Go), que não apresentou os sintomas. O whatsmeow faz um bootstrap mínimo após o login; o cobalt-2 replica o bootstrap completo do WA Web (AB-props, privacidade, probes, sync de app-state), o que por definição é mais lento até o evento de login.

## Relação com a API (`jonales/msgforge-api`)

- A API usava `it.auties00:cobalt:0.0.10` (Maven Central) e migrou para o cobalt-2 em jonales/msgforge-api#12; a imagem Docker instala o motor no commit fixado em `msgforge-api/scripts/install-cobalt.sh`. Uma correção daqui só chega à API depois que esse commit é atualizado.
- Na 0.0.10 o login leva cerca de 60 s por causa de um timeout de IQ (`erro em LOGIN: Node timed out`), registrado nas memórias da API.
- O spike `docs/spikes/cobalt-0.1` da API é o teste de aceitação do motor: código de segurança, tempo de login, status, LID, nono dígito, envio repetido, sessão restaurada.

## Sintoma 1: conexão lenta com o WhatsApp

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 1.1 | Bootstrap pós-`<success>` serial (~10 round-trips antes do `LoggedIn`) | VALIDADO | `stream/control/SuccessStreamHandler.java` `bootstrap`: probes de compliance agora em background; privacy settings + 4 MEX de disallowed lists em paralelo com join antes do `LoggedIn`. Ao vivo (jonales/Cobalt#3): login em 2 a 5 s, com o `hasWhatsapp` corrigido (`024a6f4`, o envio travava 30 s) | Nada |
| 1.2 | `abPropsService.sync()` com 3 tentativas e `sleep` aleatório de até 10 s entre elas | PENDENTE | `props/LiveABPropsService.java` `sync(Long, boolean)`, sem mudança no código. Ao vivo o 1º sync dá certo (login em 2 a 5 s), então só pesa quando ele falha | Se o 1º sync falhar o login atrasa até ~20 s; avaliar backoff menor ou sync em background (cuidado: `LID_ONE_ON_ONE_MIGRATION_ENABLED` depende dele) |
| 1.3 | Leituras bloqueantes de AB-prop no caminho de envio (`getBool(prop)` espera até 30 s pelo 1º sync) | HIPÓTESE | `UserMessageSender.shouldConvertToLid` usa `getBool(ABProp.WEB_PNLESS_STANZAS)` | Só afeta envio feito antes do 1º sync; confirmar com log de timestamps |
| 1.4 | `flushStore()` a cada envio | VALIDADO | `PersistentStore.save()` é debounced (flusher daemon) | Nada |

## Sintoma 2: "Não foi possível confirmar seu código de segurança" a cada mensagem enviada

Texto real do aviso (registrado no `msgforge-api`, `docs/spikes/cobalt-0.1/README.md`): aparece no celular de quem **recebe**, em toda mensagem, com a API rodando a **versão antiga 0.0.10, do Maven Central**, não o cobalt-2. O aviso vem da verificação ICDC: o destinatário compara o `deviceListMetadata` (hash das chaves e timestamp da lista de aparelhos de quem envia) com a lista que ele conhece. O cobalt-2 calcula e envia o ICDC (`IcdcComputer`, `IcdcEnricher`, `MessageSender.encryptForDevices`). Validado ao vivo em jonales/Cobalt#4: com os três fixes do item 2.4, o aviso não aparece mais.

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 2.1 | Phash divergente no ack não refazia a lista de dispositivos (cache reutilizado para sempre; o servidor reporta phash em todo ack) | CORRIGIDO | `device/LiveDeviceService.java` `getDeviceLists`; teste `DeviceServiceTest.mismatchedPhashForcesServerSync` | Confirmar em sessão real que o ack para de trazer `phash` após o reenvio |
| 2.2 | Lote usync com falha sobrescrevia a lista em cache com "só primário" (perdia companions) | CORRIGIDO | `device/LiveDeviceService.java` `fetchDeviceListsFromServer` (loop de fallback) | Nada |
| 2.3 | `refresh_lid="true"` no ack chama `getDeviceLists(..., null, ...)`, que é no-op quando o usuário já está em cache | PENDENTE | `message/send/UserMessageSender.java` `maybeRefreshLid` | Forçar usync (mesma técnica de 2.1) após confirmar o fluxo `syncContactListJob` no WA Web |
| 2.5 | Sessão recriada a cada execução (novo pareamento) | DESCARTADO | Builder: `createConnection()` sempre cria sessão nova; `loadLatestOrCreateConnection()` / `registered()` reusam. `WhatsAppStoreFactory.temporary()` não persiste nada. O `msgforge-api` restaura a sessão do Postgres com `registered()` (`CobaltWhatsAppGateway.start`) | Nada |
| 2.6 | Com a 0.0.10, **toda** mensagem saía como `pkmsg` (sessão Signal nunca se fixava; a API registrou isso ao vivo) | VALIDADO | Causa: o mesmo aparelho tinha duas sessões, pelo número e pelo LID. `1e2729b` (sessão pelo LID também para contatos) e `3780eb7` (aprende o par do `sender_pn`); testes `SignalStoreLidAddressTest`, `ChatMessageReceiverLidTest`. Ao vivo (jonales/Cobalt#3): `msg` depois da primeira resposta | Nada |
| 2.4 | Aviso "Não foi possível confirmar seu código de segurança" por causa do ICDC (`deviceListMetadata`) | VALIDADO | A causa não era a lista em cache: eram três erros no ICDC. `f33125a` (timestamps em segundos, não ms), `91c859b` (`key-index` lido do `<device-list>`; todo aparelho ficava com índice 0) e `51f2d36` (a lista guarda o `ts` assinado, não o horário da consulta; era este que gerava o aviso). Testes `DeviceListMetadataWireTest`, `DeviceUSyncKeyIndexTest`, `DeviceSyncedListTimestampTest`. Ao vivo (jonales/Cobalt#4, dois números): o aviso não aparece mais | O ramo `needsListReset` ainda usa o timestamp antigo (fora do escopo do #4) |

## Correções dos testes ao vivo do Msgforge (2026-11-13 a 2026-11-15)

Achados ao rodar o Msgforge com duas contas reais. Detalhes, causa e testes estão na descrição de cada PR.

| # | Área | Estado | Evidência | Próximo passo |
|---|---|---|---|---|
| 3.1 | Retry (`receipt type=retry`) procurava a mensagem pelo JID do aparelho; a conversa é guardada por usuário, então o reenvio era recusado (`MESSAGE_NOT_EXIST`) | CORRIGIDO | PR #1; `retryTargetChat` espelha `WAWebHandleRetryRequest.getTargetChat`; teste `RetryTargetChatTest` | Não há como provocar um retry sob demanda; confirmar quando um aparecer no log |
| 3.2 | `NodeTimeout` do `fb:thrift_iq` (silent nonce de conta Business) escapava das threads de renovação de credenciais no bootstrap ("Exception in thread" a cada login) | VALIDADO | PR #1; `runCredentialsRefresh`; teste `SuccessStreamHandlerCredentialsRefreshTest`; ao vivo sem a exceção nas duas contas | Nada |
| 3.3 | Recusa de chamada ia com o número de quem ligou (`caller_pn`) e com `reason`; o servidor aceitava e ignorava, e quem ligou continuava ouvindo chamar | VALIDADO | PR #2; `LifecycleController.rejectCreator` usa o criador da oferta (LID); recusa comum sai `count="0"` sem `reason`; testes `RejectCreatorTest`, `RejectStanzaWireTest`; recibo ao vivo em ~0,5 s | Nada |
| 3.4 | `parseChatMetadata` (w:g2) lia `<announce/>`/`<restrict/>` (e trocados); o servidor manda `<announcement/>`/`<locked/>` | VALIDADO | PR #3; validado ao vivo depois de reiniciar o processo | Nada |
| 3.5 | `editGroupMetadata` sem os metadados no Store (normal depois de reiniciar) não mandava nada | VALIDADO | PR #3; agora busca por `queryGroupMetadata` (que passa a gravar no Store) e lança `NoSuchElementException` se o grupo não existir | Nada |
| 3.6 | Consulta MEX `queryGroupInfo` -> `mapFetchGroupInfoToMetadata`: announce/restrict saem `true` num grupo novo e nenhum participante sai como admin | PENDENTE | PR #3 (em aberto): `announcement`/`locked` são strings tratadas como presente = true; `edges.role` não casa com `GroupPartipantRole` | Capturar o JSON real da resposta MEX e corrigir o mapeamento; até lá o Msgforge usa o w:g2 |
| 3.7 | Envio de vídeo falhava: perfil/nível H.264 passados por nome ao libopenh264 (EINVAL), `+faststart` sem caminho para reabrir a saída (ENOENT), miniatura sem cabeçalho JFIF (proporção de pixel 0/1) | CORRIGIDO | PR #4; teste `VideoPipelineEncoderTest` (fixture de 13 KB, confere duração, dimensões, miniatura JPEG e `moov` antes de `mdat`); validado ao vivo em 2026-11-15 (MP4 H.264 de 5 s pela API da Msgforge: chegou com miniatura, duração e som) | Testar ao vivo um vídeo que precise transcodificar (outro codec ou resolução maior) |
| 3.8 | Foto da própria conta (`editProfilePicture`) ia com `target` = o próprio número e uma `<picture type="preview">`; o servidor não respondia e a chamada estourava o tempo | CORRIGIDO | PR #9; `ProfilePictureStanzaTest`; validado ao vivo em 2026-11-15 pela Msgforge (troca em 2 s, notificação ao contato). O `WAWebSendProfilePictureJob` só põe `target` para grupo e manda uma `<picture>` | `removeProfilePicture` segue o mesmo formato; falta ver ao vivo |
| 3.9 | Status de contato chega como `<status from="status@broadcast">`; todo `<status>` ia para o handler de canal, que confirmava e descartava: nenhum `onNewStatus` | CORRIGIDO | PR #10; `ContactStatusRoutingTest`; validado ao vivo em 2026-11-15 (status chega, `messages.upsert` e recibo `read` com `readStatus`). Como o `WAWebCommsHandleMessagingStanza`, vira `<message>` e segue o caminho das mensagens | Nack de decifragem ainda sai com `class=message` (o Web usa `status`). O motor dispara `onNewMessage` e `onNewStatus` para o mesmo status: quem assina os dois recebe duas vezes |
| 3.10 | `logout()` deixava o socket aberto depois do `remove-companion-device` aceito: ~47 s até o servidor derrubar com 401, e o celular mostrava o aparelho conectado | CORRIGIDO | PR #11; validado ao vivo em 2026-11-15 (nenhum tráfego depois do `result`) | O celular ainda pode demorar a tirar o aparelho da lista |

Correções de jonales/Cobalt#3 e jonales/Cobalt#4. Os dois foram validados ao vivo com o Msgforge e dois números reais; os itens de sessão e ICDC estão nas linhas 1.1, 2.4 e 2.6:

- usync: `hasWhatsapp` com `context=interactive` e só o telefone no `<contact>`; leitura do key-index de cada aparelho no `<device-list>`.
- Signal/recebimento: uma sessão por aparelho, pelo LID, também para contatos; aprende o par LID/número do `sender_pn` e acha a sessão aberta pelo número.
- ICDC/aparelhos: timestamps do `deviceListMetadata` em segundos; lista sincronizada guarda o timestamp assinado (relacionados a 2.1 e 2.4).
- Mídia: imagens voltam a ser enviadas (ImageIO e `JpegCleaner`; PNG e JPEG validados); contexto do `avformat_open_input` não é fechado duas vezes (a JVM caía com SIGSEGV); granule da nota de voz em 48 kHz; áudio comum em quadros AAC de tamanho fixo (nota de voz MP3/OGG e MP3 como arquivo validados no celular e no Web).
- Outros: confirma toda stanza `status` (sem isso a conta parava de receber tudo depois de reconectar); não envia `local_automated_type=unknown` (ack 479 para contas Business); sessão web registrada reabre sem pedir QR.

## Testes conhecidos falhando (pré-existentes, fora do escopo)

- `DeviceServiceHandleDeviceNotificationTest` (3 erros): `IllegalArgumentException: event missing 'stanza' subtree` em `DeviceFixtures.buildNodeFromEvent`; problema de fixture, não de código de produção.

## Como continuar

```
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
mvn -DskipTests install -pl modules/lib -am
mvn -pl modules/lib test -Dtest='DeviceServiceTest'
graphify update modules                               # reconstrói o grafo após mudanças
graphify affected "<path>::<symbol>" --graph modules/graphify-out/graph.json
graphify save-result --memory-dir modules/graphify-out/memory --outcome useful --question "..." --answer "..." --nodes A B
```
