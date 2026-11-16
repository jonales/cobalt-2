---
type: "query"
date: "2026-11-16T01:57:41.668287+00:00"
question: "de onde vem o nome e o icone em aparelhos conectados"
contributor: "graphify"
outcome: "useful"
source_nodes: ["WhatsAppSocketClient", "DeviceProps"]
---

# Q: de onde vem o nome e o icone em aparelhos conectados

## Answer

Do DeviceProps enviado no pareamento: os (o nome) e platformType (o icone, desenhado pelo proprio celular; nao ha campo de imagem). Desde o PR #13 o os e o nome configurado no builder (name), ainda o valor configurado no pareamento; sem nome, Windows ou Mac OS.

## Outcome

- Signal: useful

## Source Nodes

- WhatsAppSocketClient
- DeviceProps