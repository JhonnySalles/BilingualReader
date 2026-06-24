# SelectMangaFragment

## 🎯 Objetivo / Contexto

Esta tela permite que o usuário selecione um mangá específico de sua biblioteca. É geralmente utilizada como um passo intermediário para vincular fontes, buscar capítulos ou em outras funcionalidades que requerem a identificação de um mangá existente.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [SelectMangaFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/menu/SelectMangaFragment.kt)
- **Layout XML:** [fragment_select_manga.xml](BilingualReader/src/main/res/layout/fragment_select_manga.xml)
- **ViewModel / Presenter:** [SelectMangaViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/menu/SelectMangaViewModel.kt)
- **Principais Views (IDs):**
  - `select_manga_recycler`: Lista para exibição dos mangás disponíveis para seleção.
  - `toolbar_select_manga_title`: Título da toolbar que também serve de âncora para o menu de contexto de troca de biblioteca.
  - `select_manga_scroll_up` / `select_manga_scroll_down`: Botões de navegação rápida pela lista.
  - `menu_select_manga_search`: Botão de busca no menu para filtrar os mangás por nome.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Filtragem por Biblioteca):** Exibe mangás de uma biblioteca específica. O usuário pode trocar a biblioteca ativa clicando no título da tela, o que abre um menu de contexto com as bibliotecas disponíveis.
- **Regra 2 (Consistência de Visualização):** Respeita o modo de exibição (Lista ou Grade) configurado globalmente para a biblioteca de mangás, carregando essa preferência do `SharedPreferences`.
- **Regra 3 (Pré-filtragem):** Pode receber um nome ou ID de mangá inicial via `Arguments` para já abrir a tela com uma busca pré-processada.
- **Regra 4 (Retorno de Seleção):** Ao clicar em um mangá, o fragmento encapsula o objeto `Manga` selecionado em um `Bundle` e o envia de volta para a tela chamadora através do método `onBack()`.
- **Regra 5 (Scroll Inteligente):** Implementa botões de scroll que aparecem/desaparecem baseados na direção e intensidade da rolagem do usuário.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Telas de Menu que requerem seleção de mangá (ex: Vincular Capítulos).
- **Próximas Telas (Para onde vai):** Retorna para a tela anterior enviando o mangá selecionado via `Bundle`.

## 🌍 Strings / Dicionário (Referência)

- `menu_select_manga_search`
- `action_scroll_up`
- `action_scroll_down`

