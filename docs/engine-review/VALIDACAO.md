# Validação ao vivo do motor

O que validar com celulares de verdade, e como, depois das correções do motor (a partir de jonales/Cobalt#2, o repositório arquivado de onde o cobalt-2 veio).
Não roda na nuvem: o proxy bloqueia o WhatsApp. Resultados voltam para `STATUS.md`.

## Caminho recomendado: pela API (msgforge-api)

A API usa o cobalt-2 (desde jonales/msgforge-api#12) e tem o roteiro completo, com comandos:
**`msgforge-api/docs/VALIDACAO-COBALT-FORK.md`**. Ele cobre login, aviso de código de segurança,
sessão Signal (`pkmsg` x `msg`), status, mídia e sessão restaurada, e a imagem Docker já instala
o cobalt-2 no commit fixado em `msgforge-api/scripts/install-cobalt.sh`.

## Caminho curto: só o motor (spike)

Para isolar o motor, sem API nem Docker (precisa de JDK 25 e Maven):

```bash
git clone https://github.com/jonales/cobalt-2.git && cd cobalt-2
git checkout master
mvn -DskipTests -Dmaven.javadoc.skip=true install -pl modules/lib -am

cd ../msgforge-api/docs/spikes/cobalt-0.1
mvn -q compile exec:java            # escaneie o QR; comandos: existe | enviar | sair
```

Use a tabela "O que observar" do `README.md` do spike.

## O que medir e onde cada resultado entra no STATUS.md

Já validado ao vivo (jonales/Cobalt#3 e #4): tempo de login, aviso de código de segurança e `pkmsg` x `msg`. Repita só como teste de regressão.

| Medida | Como ver | Item do STATUS.md |
|---|---|---|
| Tempo do scan até "logado" | relógio; no log, `post-handshake bootstrap complete` | 1.1, 1.2 |
| Aviso "Não foi possível confirmar seu código de segurança" no destinatário | abrir a conversa no celular B | 2.1, 2.4 |
| Envio depois de B responder sai `enc type=msg` (não `pkmsg`) | log `DEBUG` da API (`...COBALT_WIRE=DEBUG`) | 2.6 |
| Ack do envio sem `phash` depois do primeiro reenvio | mesmo log (`ack{...}` sem `phash`) | 2.1 |
| Status entregue/lida chegam | Manager ou `findMessages` da API | (regressão) |

## Pendências dos PRs #1 a #4 do cobalt-2

| Teste | Como ver | Item do STATUS.md |
|---|---|---|
| ~~Enviar vídeo de A para B~~ (validado em 2026-11-15: MP4 H.264 chegou com miniatura, duração e som; falta um formato que precise transcodificar) | o vídeo toca no celular B, com miniatura, e começa antes de baixar inteiro | 3.7 |
| Consultar um grupo novo pela MEX (`queryGroupInfo`) | announce/restrict devem sair `false` e o criador como admin; guarde o JSON da resposta | 3.6 |
| Retry: se aparecer `receipt type=retry` no log | a mensagem é reenviada (sem `MESSAGE_NOT_EXIST`) | 3.1 |

## Se der errado

Guarde o log do trecho (só nomes e atributos dos nós, nunca conteúdo) e anote o número do teste.
Com isso, a próxima sessão do Claude compara com o caminho de envio do motor
(`MessageSender.encryptForDevices`, `MessageEncryption.encryptForDevice`, `IcdcComputer`) e com o
`LiveDeviceService.getDeviceLists`. Registre o achado em `STATUS.md` e numa memória Graphify.

## Rodada de 2026-11-15 (Msgforge, duas contas)

Validados: vídeo (3.7), foto da própria conta (3.8), status de contato com "visto" automático
(3.9) e logout fechando a conexão (3.10). Falta: remover a foto da conta, um vídeo que precise
transcodificar, a consulta MEX de grupo (3.6) e o retry observado ao vivo (3.1).

