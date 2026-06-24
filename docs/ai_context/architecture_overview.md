# Visão Geral da Arquitetura (Architecture Overview)

## 🎯 Propósito do Projeto
BilingualReader é um aplicativo de leitura bilíngue para livros (EPUB, PDF, etc.) e mangás/comics (CBR, CBZ, RAR, ZIP, TAR). O app permite o vínculo de páginas, OCR para extração de texto, uso de dicionários e gestão de vocabulário.

## 🏗️ Padrão Arquitetural
* **Arquitetura Base:** MVVM (Model-View-ViewModel) e Clean Architecture.
* **Linguagem Principal:** Kotlin.
* **Componentes de View:**
  * UI é modularizada em Activities e Fragments.
  * O pacote `view/ui` contém as telas principais.
  * O pacote `view/adapter` contém os adapters para as listas.
  * O pacote `view/components` possui views e componentes customizados, importantes para comportamentos de leitura avançados.

## 🗂️ Estrutura de Pacotes Principal
* `model`: Entidades, enums e exceções.
* `service`: Serviços, parseamento de arquivos, OCR (Tesseract/Google Vision), repositórios locais (Room/Database), integração com Tracker (MyAnimeList), atualizações e ShareMark.
* `view`: Componentes visuais, Activities, Fragments, ViewModels e Adapters.
* `util`: Constantes, helpers, conversores e manipulação de secrets.

## 📚 Banco de Dados (Room)
* O pacote `service/repository` gerencia a comunicação de dados.
* Inclui DAO e entidades mapeadas para salvar anotações, histórico, vocabulário e o progresso da leitura bilíngue.

## 🔄 Principais Fluxos
* **Leitura de Mangá:** Processamento via `service/parses/manga` (Zip, Rar, Epub, Tar), exibição via componentes do leitor em `view/ui/reader/manga` e views em `view/components/manga`.
* **Leitura de Livro:** Parseadores em `service/parses/book` e telas de leitura em `view/ui/reader/book`.
* **Gestão de Vocabulário:** Armazenamento do vocabulário que pode ser sincronizado e testado (via Flashcards/Anki) integrado nos repositórios.

_Este documento serve como referência rápida para o assistente de IA compreender o projeto como um todo e o contexto do BilingualReader._