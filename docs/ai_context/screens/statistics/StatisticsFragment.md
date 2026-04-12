# StatisticsFragment

## 🎯 Objetivo / Contexto

[Uma breve descrição de 1 a 3 frases sobre o que o usuário faz nesta tela].

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [StatisticsFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/statistics/StatisticsFragment.kt)
- **Layout XML:** [fragment_statistics.xml](BilingualReader/src/main/res/layout/fragment_statistics.xml) (Inclui [fragment_statistics_manga.xml](BilingualReader/src/main/res/layout/fragment_statistics_manga.xml) e [fragment_statistics_book.xml](BilingualReader/src/main/res/layout/fragment_statistics_book.xml))
- **ViewModel / Presenter:** N/A (Usa `StatisticsRepository` diretamente)
- **Principais Views (IDs):**
  - `statistics_progress`: BlurView exibido durante o carregamento dos dados.
  - `statistics_manga_chart` / `statistics_book_chart`: Gráficos de linha que exibem o progresso de leitura mensal.
  - `statistics_manga_total_read_pages` / `statistics_book_total_read_pages`: Total de páginas lidas.
  - `statistics_manga_total_read_times` / `statistics_book_total_read_times`: Tempo total de leitura formatado.
  - `statistics_manga_chart_year` / `statistics_book_chart_year`: Filtro de ano para os gráficos.
  - `statistics_manga_chart_library` / `statistics_book_chart_library`: Filtro de biblioteca para os gráficos.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Agregados Gerais):** Exibe contagens de itens "Lendo", "Para Ler", na "Biblioteca" e "Lidos" para Mangas e Livros.
- **Regra 2 (Tempo de Leitura):** Calcula e exibe o tempo total gasto em itens concluídos vs. itens em andamento, além do tempo total geral.
- **Regra 3 (Média de Leitura):** Calcula a média de tempo por página baseada no tempo total e páginas totais lidas.
- **Regra 4 (Gráficos Dinâmicos):** Utiliza a biblioteca MPAndroidChart para renderizar o volume de leitura por mês. Os dados mudam dinamicamente ao selecionar um ano ou biblioteca específica nos seletores (AutoCompleteTextView).
- **Regra 5 (Formatação de Tempo):** Converte segundos brutos em uma string amigável contendo Dias, Horas, Minutos e Segundos.
- **Regra 6 (Feedback de Carregamento):** Implementa um efeito de desfoque (BlurView) com indicador de progresso circular enquanto as consultas ao banco de dados são processadas.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Menu Principal (Drawer/NavigationView).
- **Próximas Telas (Para onde vai):** N/A.

## 🌍 Strings / Dicionário (Referência)

- `statistics_read_by_month`
- `statistics_sector_manga`
- `statistics_sector_book`
- `statistics_chart_library_all`
- `statistics_average`
- `statistics_format_days`
- `statistics_format_hours`
- `statistics_format_minutes`
- `statistics_format_seconds`

