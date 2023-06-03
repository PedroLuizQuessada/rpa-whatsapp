# rpa-whatsapp

### Pré-requisitos da máquina
1) Java 8
2) Acesso à internet durante a execução da automação

<br>

### Instalação
1) Pasta RPAWhatsapp do projeto
2) Pasta com perfil que será usado no Google Chrome
3) Pasta com Chromium
4) Arquivo iniciar.bat com o caminho do arquivo application.properties

<br>

### Configurações
###### As configurações devem ser realizadas no arquivo application.properties localizado na pasta RPAWhatsapp
1) api.nome-ambiente: nome do ambiente ao qual a automação pertence
2) api.id-automacao: ID da automação no Sistemato
3) api.token: token para acesso aos endpoints do Sistemato
4) rpa.intervalo-minutos: quantidade de minutos que a automação esperará entre uma execução e outra
5) rpa.texto-primeiro: "true" para enviar primeiro as mensagens de texto e depois as imagens ou "false" para o contrário
6) rpa.parar-quando-nao-encontrar-contato: "true" para a automação parar ao não encontrar um contato no WhatsApp ou "false" para ela continuar executando os casos restantes
7) rpa.arquivos.path: caminho até a pasta onde deverão ser incluídos os arquivos .png e .jpeg a serem enviados a cada um dos contatos
8) rpa.webdriver.path: caminho até o ChromeDriver
9) rpa.profile.path: caminho até a pasta criada para armazenar o perfil usado no ChromeDriver
10) rpa.browser-exe.path: caminho até a pasta do executável do Google Chrome 
11) rpa.porta: porta a ser usada pelo navegador 

<br>

### Preparação
1) Configurar via Sistemato os textos que serão enviados como mensagens a cada um dos contatos. Caso não houverem textos a serem enviados, pular este passo.
2) Subir as imagens (.png ou .jpeg) a serem enviadas no diretório "RPAWhatsapp/arquivos" na máquina onde rodará automação. Caso não houverem imagens a serem enviadas, pular este passo.

<br>

### Execução
1) A automação seguirá as configurações aplicadas no Sistemato
2) A automação puxará as pendências subidas no Sistemato por planilhas Excel
3) Ao tentar acessar o WhatsappWeb, a automação irá verificar se a máquina já se encontra logada, em caso negativo irá aparecer uma mensagem na tela. O usuário deverá logar manualmente e, após logado, clicar no botão "OK"
4) A automação registrará no Sistemato possíveis falhas que ocorram ao longo da execução
5) A automação registrará no Sistemato cada uma das suas execuções