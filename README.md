# BilingualMangaReader
> Leitor de mangas e comics offline, bem como ebooks em geral, no qual possui compatibilidade com Tesseract e Google Vision para utilização de OCR, vínculo de dois arquivos em idiomas diferentes com facilidade na troca entre as páginas de ambos e compatibilidade com textos extraido e processado através do programa [MangaExtractor](https://github.com/JhonnySalles/MangaExtractor)

<h4 align="center"> 
	🚧  BilingualReader 🚀 Em construção...  🚧
</h4>

[![Build Status][travis-image]][travis-url]

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

    List<Class> capitulos          # Lista de classes de capitulos
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
    │	      ├── ingles         # Significado da palavra em inglês
    │	      ├── leitura        # Leitura em katakana
    │       └── revisado       # Flag sinalizadora que o vocabulário foi revisado ou não. 
    └── vocabulario            # Vocabulário do capitulo
        ├── palavra            
        ├── portugues          # Significado da palavra em português
	      ├── ingles             # Significado da palavra em inglês
	      ├── leitura            # Leitura em katakana
        └── revisado           # Flag sinalizadora que o vocabulário foi revisado ou não.
  
         
> Estrutura de classe com informações da página que possui a legenda pré processada, podendo ser obtida de uma raw ou traduzida de alguma fã sub. Com ele será possível apresentar a tradução ou significados dos kanjis presente na página.


## Histórico de Release

* 1.0.0
    * Commit inicial com as alterações do projeto base.
* 1.0.1
    * Em progresso.

### Features

- [X] Leitor de Mangas/Comics
- [X] Abertura de arquivos cbr/rar
- [X] Abertura de arquivos cbz/zip
- [X] Abertura de arquivos cbt/tar
- [X] Abertura de arquivos cb7/7z
- [X] Carregar legendas pré processadas
- [X] Localização da legenda em outro idioma
- [X] Popup flutuante da legenda para facilitar a leitura
- [X] Impressão na imagem das coordenadas de texto extraido para melhor localizar a legenda
- [X] Reconhecimento de furigana do texto em japonês
- [X] Reconhecimento do nivel jlpt do kanji
- [X] Significado do kanji
- [X] Troca das legendas entre idiomas selecionados (devido a diferenças de paginas entre versões pode não localizar de forma satisfatória)
- [X] Abertura de um segundo arquivo para leitura de multi idioma (com a troca de imagens entre o original e outro arquivo em outro idioma)
- [X] Rastreamento de erros e logs.
- [X] Chamada ao aplicativo Kaku para utilização de OCR
- [X] Reconhecimento de palavras utilizando o Tesseract OCR/Google Vision
- [X] Dicionarios Japonês
- [ ] Leitor de novels
- [ ] Abertura de arquivos pdf
- [ ] Abertura de arquivos ebook no formato awz/awz3
- [ ] Abertura de arquivos ebook no formato epub/epub3
- [ ] Abertura de arquivos ebook no formato mobi
- [ ] Abertura de arquivos ebook no formato fb2
- [ ] Abertura de arquivos ebook no formato doc/docx
- [ ] Abertura de arquivos ebook no formato html
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

> Abra então no android studio a pasta do projeto e aguarde o gradle processar as dependências 

> Após a instalação das dependências compile e execute o projeto, no qual será então aberto no emulador.


## Exemplos

> Algumas imagens do aplicativo

![Biblioteca](https://i.imgur.com/roLmu9C.png)

![Leitor](https://i.imgur.com/r4hhAzj.jpg)

![Leitor](https://i.imgur.com/Awwcjyc.jpg)


> Recursos

![Vinculo de paginas em dois arquivos](https://i.imgur.com/uCvYPV6.png)

![Paginas não vinculadas](https://i.imgur.com/E1kyoQ4.png)

![Paginas duplas](https://i.imgur.com/adp9MwE.png)

![Popup flutuante](https://i.imgur.com/dIuQO9N.jpg)

![Localização do texto reconhecido com ocr](https://i.imgur.com/fMavbfI.jpg)

![Informações da legenda](https://i.imgur.com/t7LR4PV.jpg)

![Informações do kanji](https://i.imgur.com/j0Wzpsv.jpg)

![Informações do kanji](https://i.imgur.com/js2wlIb.jpg)

![Vocabulário](https://i.imgur.com/xG1jfYr.jpg)


> Estrutura do arquivo recomendada

![Estrutura de pasta](https://i.imgur.com/EZdlHGV.png)

> Recomendo embutir a legenda no arquivo e separar as pastas por capitulo para facilitar a localização da legenda correta quando não for possível o aplicativo encontrar a pagina correspondente.