# AboutFragment

## 🎯 Objetivo / Contexto

Esta tela é responsável por exibir informações sobre o aplicativo, como versão, desenvolvedor e links para políticas de privacidade e termos de uso.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [AboutFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/about/AboutFragment.kt)
- **Layout XML:** [fragment_about.xml](BilingualReader/src/main/res/layout/fragment_about.xml)
- **ViewModel / Presenter:** N/A (Lógica simples no Fragment)
- **Principais Views (IDs):**
  - `about_app_version_number`: Exibe o número da versão do aplicativo.
  - `about_btn_rate_us`: Botão para avaliar o app na Play Store.
  - `about_btn_shared`: Botão para compartilhar o app.
  - `about_btn_suggestion`: Botão para enviar sugestão via e-mail.
  - `about_btn_email`: Botão para enviar e-mail de contato.
  - `about_btn_github`: Botão para abrir o repositório no GitHub.
  - `about_app_library`: Exibe a lista de bibliotecas utilizadas e seus links.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Versão):** Obtém a versão do app dinamicamente através do `packageManager`.
- **Regra 2 (Avaliação):** Redireciona para a Play Store (via `market://` ou link `http`) ao clicar em "Avaliar".
- **Regra 3 (Compartilhamento):** Utiliza `Intent.ACTION_SEND` para compartilhar informações básicas sobre o app.
- **Regra 4 (Contato/Sugestão):** Utiliza `Intent.ACTION_SENDTO` com `mailto:` para sugestões e e-mail de contato.
- **Regra 5 (GitHub):** Abre o link do GitHub configurado nas strings.
- **Regra 6 (Créditos):** Carrega uma lista de bibliotecas do array de recursos `about_app_library_content` e habilita links clicáveis no `TextView`.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Menu Principal (Drawer/NavigationView).
- **Próximas Telas (Para onde vai):** Aplicativos externos (Navegador, Google Play, Cliente de E-mail).

## 🌍 Strings / Dicionário (Referência)

- `about_app_name`
- `about_app_version`
- `about_app_author`
- `about_app_author_name`
- `about_app_shared_and_rate_us`
- `about_app_shared_and_rate_us_description`
- `about_app_rate_us`
- `about_app_shared`
- `about_app_contact`
- `about_app_contact_description`
- `about_app_suggestion`
- `about_app_mail`
- `about_app_github`
- `about_app_library`
- `about_app_mail_address`
- `about_app_github_link`
- `action_app_not_installed`

