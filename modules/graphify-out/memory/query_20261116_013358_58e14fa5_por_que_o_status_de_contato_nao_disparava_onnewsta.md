---
type: "query"
date: "2026-11-16T01:33:58.371202+00:00"
question: "por que o status de contato nao disparava onNewStatus"
contributor: "graphify"
outcome: "useful"
source_nodes: ["NewsletterStatusStreamHandler", "MessageStreamHandler", "LiveNodeStreamService"]
---

# Q: por que o status de contato nao disparava onNewStatus

## Answer

O servidor manda o status de contato como <status from=status@broadcast>. Todo <status> ia para o NewsletterStatusStreamHandler, que so trata canal e descartava o resto. Corrigido no PR #10: vira <message> e segue o MessageStreamHandler.handleContactStatus, como o WAWebCommsHandleMessagingStanza.

## Outcome

- Signal: useful

## Source Nodes

- NewsletterStatusStreamHandler
- MessageStreamHandler
- LiveNodeStreamService