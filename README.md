# BilingualMangaReader
> Leitor de mangas e comics offline, bem como ebooks em geral, no qual possui compatibilidade com Tesseract e Google Vision para utilização de OCR, vínculo de dois arquivos em idiomas diferentes com facilidade na troca entre as páginas de ambos e compatibilidade com textos extraido e processado através do programa [MangaExtractor](https://github.com/JhonnySalles/MangaExtractor)

<h4 align="center"> 
	🚧  BilingualReader 🚀 Em construção...  🚧
</h4>

<p align="center">
 <a href="#Sobre">Sobre</a> •
 <a href="#Bibliotecas-utilizadas">Bibliotecas utilizadas</a> • 
 <a href="#Json-processado">Json processado</a> • 
 <a href="#Estrutura-da-classe-do-arquivo-de-legenda">Estrutura do arquivo de legenda</a> • 
 <a href="#Histórico-de-Release">Histórico de Release</a> • 
 <a href="#Features">Features</a> • 
 <a href="#Contribuindo">Contribuindo</a> • 
 <a href="#Instalação">Instalação</a> • 
 <a href="#Exemplos">Exemplos</a>
</p>


## Sobre

Programa foi criado em Kotlin, onde foi utilizado algumas bibliotecas que estarão listadas mais abaixo para carregamento das imagens e leitura de arquivos jsons.

O aplicativo foi projetado para reconhecer arquivos cbr/rar, cbz/zip, cbt/tar e cb7/7z em uma pasta de biblioteca, onde irá listar todas os arquivos encontrados na pasta informada.

Existe a opção de abertura nos mais populares formatos de ebooks da atualidade como epub/epub3, awz/awz3 e mobi.

Também irá salvar algumas preferências e o progresso e tem suporte a furigana e nivel jlpt do kanji em cores.

Possui compatibilidade com o Tesseract e Google Vision para reconhecimento de caracteres em imagens, no qual comparado a versão de computador e em teste realizado é bem limitado, o que apenas faz jus o reconhecimento para kanjis e não palavras inteiras.

Vinculo entre dois arquivos diferentes para realizar a troca de página entre os arquivos de forma fácil com apenas um clique de botão, no qual pode auxiliar na leitura, aprendizado e entendimento de um novo idioma.


### Bibliotecas utilizadas

<ul>
  <li><a href="https://github.com/square/picasso">Picasso</a> - Uma poderosa biblioteca de download e cache de imagens para Android.</li>
  <li><a href="https://github.com/google/gson">Gson</a> - Uma biblioteca para conversão de json em classe. </li>
  <li><a href="https://github.com/WorksApplications/Sudachi">Sudachi</a> - Uma excelente api para reconhecimento de vocabulário dentro de uma frase em japonês, como também sua leitura, forma de dicionário e afins. </li>
  <li> Room - Uma biblioteca nativa com vários recusos para gerenciar banco de dados SQLite. </li>
  <li> PageView - Implementado a estrutura de apresentação de imagens em carrocel. </li>
  <li><a href="https://github.com/junrar/junrar">Junrar</a> - Biblioteca para leitura e extração de arquivos rar e cbr. </li>
  <li><a href="https://github.com/0xbad1d3a5/Kaku]">Kaku</a> - Leitor OCR para android. <i>"Apenas chamada por dentro aplicativo, necessário estar instalado."</i> </li>
  <li><a href="https://www.atilika.org/">Kuromoji</a> - Analizador morfológico japonês. </li>
  <li><a href="https://github.com/WorksApplications/Sudachi">Sudachi</a> - Sudachi é um analisador morfológico japonês.</li>
  <li><a href="https://github.com/lofe90/FuriganaTextView">FuriganaTextView</a> - TextView personalizado para Android que renderiza texto em japonês com furigana. </li>
  <li><a href="https://github.com/adaptech-cz/Tesseract4Android">Tesseract4Android</a> - Poderosa biblioteca que faz a comunicação com o Tesseract OCR.</li>
  <li><a href="https://github.com/tony19/logback-android">LogBack</a> - Biblioteca que traz o poderoso logback para o android.</li>
  <li><a href="https://github.com/sarajmunjal/two-way-backport">TwoWayView</a> - Biblioteca para apresentação de lista, grids e afins com formato horizontal e vertical.</li>
  <li>Retrofit 2 - Popular biblioteca para HTTPs no Android.</li>
</ul>


## Json processado

Caso tenha alguma dúvida sobre como processar o json favor entrar em contato. Em breve estarei disponibilizando as legendas que já processadas.

[Legendas do manga de exemplo.](https://drive.google.com/drive/folders/1RGVVoyrLfT6qZO9mYChkmWPc1Gm0s3YJ?usp=sharing)

### Estrutura da classe do arquivo de legenda

O aplicativo é também compatível com legendas extraidas e pré processadas com o programa [MangaExtractor](https://github.com/JhonnySalles/MangaExtractor), onde após exportar para json as legendas com o formato abaixo, é possível carrega-los tanto embutido no arquivo de manga (rar/zip/tar), como também importado um arquivo de json solto localizado em alguma pasta no celular.

    List<Class> capitulos      # Lista de classes de capitulos
    ├── id
    ├── manga                  # Nome do manga
    ├── volume              
    ├── capitulo
    ├── linguagem              # Atualmente é suportado em linguagem Inglês, Japonês e Português.
    ├── scan
    ├── isExtra
    ├── isRaw
    ├── isProcessado
    ├── List<Class> paginas    # Array de classes páginas
    │   ├── nome               # Nome da imagem que está sendo processado
    │   ├── numero             # Um contador sequencial das imagens que estão no diretório
    │   ├── hashPagina
    │   ├── isProcessado
    │   ├── List<Class> Textos # Array de classe dos textos da página
    │   │   ├── sequencia
    │   │   ├── posX1          # Coordenadas da fala na imagem
    │   |   ├── posY1              
    │   |   ├── posX2              
    │   |   └── posY2 
    |   ├── hashPagina         # Hash md5 da imagem que foi processada, para que possa localizar a legenda desta página
    │   └── vocabulario        # Vocabulário da página
    │       ├── palavra       
    │       ├── portugues      # Significado da palavra em português
    │	    |  ├── ingles         # Significado da palavra em inglês
    │	    |  ├── leitura        # Leitura em katakana
    │       └── revisado       # Flag sinalizadora que o vocabulário foi revisado ou não. 
    └── vocabulario            # Vocabulário do capitulo
        ├── palavra            
        ├── portugues          # Significado da palavra em português
	|   ├── ingles            # Significado da palavra em inglês
	|   └── leitura           # Leitura em katakana
        └── revisado           # Flag sinalizadora que o vocabulário foi revisado ou não.
  
         
> Estrutura de classe com informações da página que possui a legenda pré processada, podendo ser obtida de uma raw ou traduzida de alguma fã sub. Com ele será possível apresentar a tradução ou significados dos kanjis presente na página.


## Histórico de Release

* 1.0.0
    * Commit inicial com as alterações do projeto base.
    * Implementado a abertura de livros no formato PDF, EPUB, MOBI e FB2.
* 1.0.1
    * Em progresso.

### Features

- [X] Leitor de Mangas/Comics
- [X] Abertura de arquivos cbr/rar
- [X] Abertura de arquivos cbz/zip
- [X] Abertura de arquivos cbt/tar
- [X] Abertura de arquivos cb7/7z
- [X] Leitor de Livros
- [X] Abertura de arquivos pdf
- [X] Abertura de arquivos epub/epub3 [ebook]
- [X] Abertura de arquivos mobi [ebook]
- [X] Abertura de arquivos fb2 [ebook]
- [X] Abertura de arquivos djvu [ebook]
- [X] Arquivo de legendas pré processadas [Mangas/Comics]
- [X] Localização da legenda em outro idioma [Mangas/Comics]
- [X] Auto-Scroll quando pressionar avançar [Mangas/Comics]
- [X] Zoom com o clique longo [Mangas/Comics]
- [X] Impressão na imagem as coordenadas de legenda para melhor entendimento
- [X] Dicionarios Japonês
- [X] Reconhecimento de furigana do texto em japonês
- [X] Apresentação do nivel jlpt do kanji
- [X] Significado do kanji
- [X] Implementação de temas
- [X] Alternar entre legendas selecionados (devido a diferenças de paginas entre as versões em diferente idioma a localização não ocorria de forma satisfatória)
- [X] Abertura de um segundo arquivo para leitura de multi idioma (alternar entre as imagens da página com um siples botão) [Mangas/Comics]
- [X] Permitir a modificação do vinculo entre as páginas dos dois arquivos [Mangas/Comics]
- [X] Automatizar o vinculo entre as páginas, levando em consideração páginas duplas [Mangas/Comics]
- [X] Rastreamento de erros e logs
- [X] Chamada ao aplicativo Kaku para utilização de OCR
- [X] Reconhecimento de palavras utilizando o <b>Tesseract OCR/Google Vision</b>
- [X] Dicionarios Japonês
- [X] Leitura de metadata de Mangas/Comics no formato <b>ComicRack</b>
- [X] Sugestões de filtros na biblioteca (Autor, Extensão, Série, Volume, editora)
- [X] Adicionar marcação do capítulo na barra de progresso (capítulos separado por pastas no arquivo)
- [X] Lista de páginas com separação por capítulos
- [X] Compartilhar imagem [Mangas/Comics]
- [X] Animações nos ícones
- [X] Compartilhamento de marcações entre o dispositivos através do Google Drive
- [X] Compartilhamento de marcações entre o dispositivos através do Firebase
- [X] Funcionalidade de texto para fala utilizando a api do Edge da Microsoft
- [X] Estatisticas de leituras
- [X] Marcadores de texto na leitura de livro.
- [X] Popup para vocabulario e kanji em livros em japonês.
- [ ] Exportar vocabularios ao Anki


## Contribuindo

1. Fork (<https://github.com/JhonnySalles/BilingualReader/fork>)
2. Crie sua branch de recurso (`git checkout -b feature/fooBar`)
3. Faça o commit com suas alterações (`git commit -am 'Add some fooBar'`)
4. Realize o push de sua branch (`git push origin feature/fooBar`)
5. Crie um novo Pull Request

<!-- Markdown link & img dfn's -->

[travis-image]: https://img.shields.io/travis/dbader/node-datadog-metrics/master.svg?style=flat-square
[travis-url]: https://travis-ci.org/dbader/node-datadog-metrics
[wiki]: https://github.com/yourname/BilingualReader/wiki

## Instalação

> Para executar o projeto é necessário ter o android studio instalado junto com uma versão do emulador.

> Abra então no android studio a pasta do projeto e aguarde o gradle processar as dependências. 

> Após a instalação das dependências compile e execute o projeto, no qual será então aberto no emulador.


## Exemplos

> Algumas imagens do aplicativo

<p align="center" float="left">
   <img alt="Menu principal" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Menu.png" width="300" class="center">
   <img alt="Biblioteca" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_01.png" width="300" class="center">
   <img alt="Biblioteca" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_02.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Leitor" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_01.png" width="300" class="center">
   <img alt="Capítulos" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Paginas_01.png" width="300" class="center">
</p>

<p align="center">
   <img alt="Leitor" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_03.png" width="500" class="center">
</p>

<p align="center" float="left">
   <img alt="Apresentação do livro" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_01.png" width="300" class="center">
   <img alt="Marcações no texto" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_detash_01.png" width="300" class="center">
   <img alt="Filtros para marcações" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_detash_04.png" width="300" class="center">
</p>


> Biblioteca

<p align="center" float="left">
   <img alt="Bibliotecaa" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_01.png" width="300" class="center">
   <img alt="Ordenação" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_05.png" width="300" class="center">
   <img alt="Filtro" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_06.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Sugestões de pesquisa" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_03.png" width="300" class="center">
   <img alt="Filtrado Sugestões" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Biblioteca_04.png" width="300" class="center">
</p>


> MetaData

<p align="center" float="left">
   <img alt="MetaData ComicRack" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Detalhe_01.png" width="300" class="center">
   <img alt="Informações do arquivo" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Detalhe_02.png" width="300" class="center">
</p>


> Vinculo entre arquivos

<p align="center" float="left">
   <img alt="Vinculo de paginas entre dois arquivos" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_01.png" width="300" class="center">
   <img alt="Movimentação das próximas imagens" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_02.png" width="300" class="center">
   <img alt="Vinculo em página simples" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_03.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Vinculo de pagina dupla" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_04.png" width="300" class="center">
   <img alt="Pagina dupla" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_05.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Zoom da pagina" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_06.png" width="300" class="center">
   <img alt="Zoom da pagina" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_07.png" width="300" class="center">
</p>

<p align="center">
   <img alt="Página original" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_08.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Página vinculada" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_09.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Página original com zoom" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_10.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Zoom mantido na troca" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Vinculo_11.png" width="500" class="center">
</p>


> Leitura

<p align="center">
   <img alt="Leitor" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_03.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Zoom mantendo pressionado" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_04.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Funções de touch" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_05.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Auto Scroll ao clicar na próxima página" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_06.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Auto Scroll na leitura" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_07.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Auto Scroll na leitura" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_08.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Auto Scroll próxima página" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_09.png" width="500" class="center">
</p>

<p align="center" float="left">
   <img alt="Cores e iluminação" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Leitura_02.png" width="300" class="center">
   <img alt="Compartilhar a imagem" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Compartilhar_01.png" width="300" class="center">
   <img alt="Compartilhar a imagem" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Compartilhar_02.png" width="300" class="center">
</p>


> Legenda

<p align="center" float="left">
   <img alt="Legendas" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_01.png" width="300" class="center">
   <img alt="Legendas" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_02.png" width="300" class="center">
   <img alt="Vocabulário" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_03.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Legenda" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_04.png" width="300" class="center">
   <img alt="Legenda" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_05.png" width="300" class="center">
   <img alt="Kanji" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_06.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Kanji" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_07.png" width="300" class="center">
   <img alt="Kanji" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Legenda_08.png" width="300" class="center">
</p>

<p align="center">
   <img alt="Popup da legenda" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Popup_01.png" width="500" class="center">
</p>

<p align="center">
   <img alt="Identificação da legenda pela coordenada no clique longo" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Popup_02.png" width="500" class="center">
</p>

<p align="center" float="left">
   <img alt="Impressão da localização da legenda" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Popup_03.png" width="300" class="center">
   <img alt="Impressão da localização da legenda" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Popup_04.png" width="300" class="center">
</p>
<p align="center" float="left">
   <img alt="Impressão da localização da legenda em outro idioma" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Popup_05.png" width="300" class="center">
   <img alt="Copia do vocabulário" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Popup_06.png" width="300" class="center">
</p>


> Recursos

<p align="center" float="left">
   <img alt="Sincronização de marcações" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Notificacao_01.png" width="300" class="center">
   <img alt="Sincronização de marcações" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Notificacao_02.png" width="300" class="center">
   <img alt="Importação de vocabulário" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Notificacao_03.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Temas" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Tema_01.png" width="300" class="center">
   <img alt="Temas" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Tema_02.png" width="300" class="center">
   <img alt="Temas" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Tema_03.png" width="300" class="center">
</p>


> Livro

<p align="center" float="left">
   <img alt="Apresentação do livro" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_01.png" width="300" class="center">
   <img alt="Layout da leitura do livro" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_02.png" width="300" class="center">
   <img alt="Busca de texto" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_find_01.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Livro em japonês" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_03.png" width="300" class="center">
   <img alt="Processamento de vocabulário e kanjis" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_japanese_01.png" width="300" class="center">
   <img alt="Popup de significados" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_japanese_02.png" width="300" class="center">
</p>


> Marcações

<p align="center" float="left">
   <img alt="Marcações no texto" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_detash_01.png" width="300" class="center">
   <img alt="Anotações das marcações" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_detash_02.png" width="300" class="center">
</p>

<p align="center" float="left">
   <img alt="Pesquisa de marcações" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_detash_03.png" width="300" class="center">
   <img alt="Filtros para marcações" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Livro_detash_04.png" width="300" class="center">
</p>


> Estrutura do arquivo recomendada

<p align="center">
   <img alt="Estrutura de pastas do arquivo" src="https://github.com/JhonnySalles/BilingualReader/blob/master/image/Estrutura_do_Arquivo.png" width="500" class="center">
</p>

> Recomendo embutir a legenda no arquivo e separar as pastas por capitulo para facilitar a localização da legenda correta quando não for possível o aplicativo encontrar a pagina correspondente.
