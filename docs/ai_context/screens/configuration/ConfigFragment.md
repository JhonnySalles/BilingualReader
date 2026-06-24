# ConfigFragment

## 🎯 Objetivo / Contexto

Centraliza todas as configurações do aplicativo, incluindo temas, idiomas de preferência, caminhos de biblioteca, backup de banco de dados e filtros de processamento de texto.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [ConfigFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/configuration/ConfigFragment.kt)
- **Layout XML:** [fragment_config.xml](BilingualReader/src/main/res/layout/fragment_config.xml) (inclui [fragment_config_system.xml](BilingualReader/src/main/res/layout/fragment_config_system.xml), [fragment_config_manga.xml](BilingualReader/src/main/res/layout/fragment_config_manga.xml), [fragment_config_book.xml](BilingualReader/src/main/res/layout/fragment_config_book.xml))
- **ViewModel / Presenter:** [ConfigViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/configuration/ConfigViewModel.kt)
- **Principais Views (IDs):**
  - `config_system_list_themes`: Lista horizontal de temas visuais disponíveis.
  - `config_system_backup`: Botão para exportar o banco de dados.
  - `config_system_restore`: Botão para importar um backup existente.
  - `config_manga_library_path`: Campo de seleção do diretório principal de mangas.
  - `config_book_library_path`: Campo de seleção do diretório principal de livros.
  - `config_book_tts_speed`: Slider para ajustar a velocidade do Text-to-Speech.
  - `config_book_list_fonts_japanese`: Seleção de fonte específica para textos em japonês.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Mudança Dinâmica de Tema:** Ao selecionar um tema na lista, o aplicativo é recreado para aplicar os novos estilos visuais.
- **Gestão de Permissões:** O fragmento lida com permissões de armazenamento (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`) para selecionar diretórios de biblioteca e realizar backups.
- **Sincronização com Nuvem:** Interface opcional para sincronizar marcadores de leitura via Google Drive.
- **Persistência Imediata:** A maioria das alterações nas configurações é salva imediatamente no `SharedPreferences` através do `ViewModel`.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `MainActivity` (Gaveta de navegação / Menu lateral).
- **Próximas Telas (Para onde vai):** `ConfigLibrariesFragment`, `TouchScreenFragment`.

## 🌍 Strings / Dicionário (Referência)

- `config_sector_system`
- `config_sector_manga`
- `config_sector_book`
- `config_database_backup`
- `config_database_restore`
- `config_theme_mode`
