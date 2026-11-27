---
type: "query"
date: "2026-11-27T20:20:30.283500+00:00"
question: "como foi feita a evolucao LTS de bibliotecas, plugins Maven, nativos e ferramentas na v0.1.0"
contributor: "graphify"
outcome: "useful"
source_nodes: ["pom.xml", "ApkArchive", "WhatsAppAndroidClientInfo", "build-natives.sh"]
---

# Q: como foi feita a evolucao LTS de bibliotecas, plugins Maven, nativos e ferramentas na v0.1.0

## Answer

POM pai centralizou dependencyManagement e pluginManagement (herdados pelos 12 modulos), removeu OSSRH legado, propriedades mortas e ez-vcard redundante em cobalt-lib, e consolidou os profiles uber-jar-*. Plugins atualizados: compiler 3.16.0, surefire 3.6.1, javadoc 3.12.0, source 3.4.0, assembly 3.8.0, gpg 3.2.8, central-publishing 0.12.0, exec 3.7.0, Maven Wrapper 3.9.16. Dependencias atualizadas: JUnit 6.1.3 via junit-bom, H2 MVStore 2.5.252, Fastjson2 2.0.66, PDFBox 3.0.8, libphonenumber 9.0.44, ZXing 3.5.4, ez-vcard 0.12.2, BouncyCastle 1.87, JavaPoet 0.23.0, Selenium 4.54.0. O apk-parser abandonado foi substituido pelo leitor puro-JDK ApkArchive (ZIP + AXML + APK Signing Block v2/v3/v3.1 + fallback JAR v1). Receitas nativas atualizadas para FFmpeg n7.1.5 LTS, openh264 v2.7.0, libvpx v1.17.0, dav1d 1.5.5, libwebp v1.6.0 e runners ubuntu-24.04; tools/web/* elevados para Node >=24 LTS. Nota de build JPMS: no reactor Maven, usar verify/package/install (nao apenas compile) para que modulos downstream vejam JARs empacotados de wire-core/stanza-core.

## Outcome

- Signal: useful

## Source Nodes

- pom.xml
- ApkArchive
- WhatsAppAndroidClientInfo
- build-natives.sh