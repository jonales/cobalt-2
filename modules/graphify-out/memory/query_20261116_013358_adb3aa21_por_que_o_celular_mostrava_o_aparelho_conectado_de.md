---
type: "query"
date: "2026-11-16T01:33:58.450719+00:00"
question: "por que o celular mostrava o aparelho conectado depois do logout"
contributor: "graphify"
outcome: "useful"
source_nodes: ["LiveLinkedWhatsAppClient"]
---

# Q: por que o celular mostrava o aparelho conectado depois do logout

## Answer

O remove-companion-device era aceito, mas o socket ficava aberto ~47 s ate o servidor derrubar com 401. logout() agora chama disconnect(LOGGED_OUT) logo depois, como o WAWebSocketLogoutUtils. PR #11.

## Outcome

- Signal: useful

## Source Nodes

- LiveLinkedWhatsAppClient