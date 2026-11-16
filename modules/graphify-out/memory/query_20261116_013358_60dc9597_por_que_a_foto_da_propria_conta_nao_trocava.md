---
type: "query"
date: "2026-11-16T01:33:58.411260+00:00"
question: "por que a foto da propria conta nao trocava"
contributor: "graphify"
outcome: "useful"
source_nodes: ["LiveLinkedWhatsAppClient", "IqSendProfilePictureRequest"]
---

# Q: por que a foto da propria conta nao trocava

## Answer

editProfilePicture mandava target = o proprio numero e uma picture type=preview; o servidor nao respondia. O WAWebSendProfilePictureJob so poe target para grupo e manda uma picture type=image. Corrigido no PR #9.

## Outcome

- Signal: useful

## Source Nodes

- LiveLinkedWhatsAppClient
- IqSendProfilePictureRequest