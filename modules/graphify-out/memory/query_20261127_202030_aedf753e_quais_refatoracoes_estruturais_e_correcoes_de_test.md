---
type: "query"
date: "2026-11-27T20:20:30.329269+00:00"
question: "quais refatoracoes estruturais e correcoes de testes foram feitas na v0.1.0"
contributor: "graphify"
outcome: "useful"
source_nodes: ["LiveLinkedWhatsAppClient", "SyntheticTelemetryHub", "AppStateMutationRegistry", "CobaltConnectionEvent", "DeviceFixtures", "WamImplGeneratorTest"]
---

# Q: quais refatoracoes estruturais e correcoes de testes foram feitas na v0.1.0

## Answer

Extraidos SyntheticTelemetryHub (16 emissores Synthetic*Telemetry) e AppStateMutationRegistry (41 fabricas *MutationFactory, removendo businessBroadcastCampaignMutationFactory nao utilizada) de LiveLinkedWhatsAppClient, reduzindo 57 campos e ~300 linhas de wiring sem alterar a API publica. Implementado o pilar de metricas JFR nativo (JDK 25) em cobalt-telemetry-core (CobaltConnectionEvent, CobaltStanzaEvent, CobaltMessageEvent, CobaltMediaTransferEvent + CobaltJfrMetricsTest), conectado em LiveLinkedWhatsAppClient.onSocketOpened(). Corrigidos os 4 loaders de fixtures JSONL (DeviceFixtures, MessageFixtures, SyncFixtures, MigrationFixtures) que buscavam apenas 'stanza' em vez de 'node' (resolvendo os 3 erros pre-existentes de DeviceServiceHandleDeviceNotificationTest), e WamImplGeneratorTest atualizado para repassar jdk.module.path ao javac sob Surefire 3.6.1.

## Outcome

- Signal: useful

## Source Nodes

- LiveLinkedWhatsAppClient
- SyntheticTelemetryHub
- AppStateMutationRegistry
- CobaltConnectionEvent
- DeviceFixtures
- WamImplGeneratorTest