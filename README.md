# rpa-whatsapp

### Pré-requisitos da máquina
1) Java 8
2) Acesso a internet durante a execução da automação
3) Número de Whatsapp vinculado a uma conta Google

<br>

### Instalação
1) Pasta RPAWhatsapp do projeto
2) ChromeDriver na versão do Chrome da máquina (necessário ser versão 110 ou anterior)
3) Arquivo iniciar.bat com o caminho do arquivo application.properties
4) Pasta com perfil que será usado no Google Chrome

<br>

### Configurações
###### As configurações devem ser realizadas no arquivo application.properties localizado na pasta RPAWhatsapp
### Práticas
1) rpa.intervalo-minutos: número de minutos que serão aguardados entre uma execução e outra da automação
2) rpa.texto-primeiro: "true" para enviar primeiro as mensagens de texto e depois as imagens ou "false" para o contrário
### Técnicas
1) rpa.token: token para acesso aos endpoints do Sistemato
2) api.recuperar-dados.link: link ao endpoint do Sistemato para recuperar dados da automação
3) api.registrar-log.link: link ao endpoint do Sistemato para registrar logs da automação
4) api.processar-pendencia.link: link ao endpoint do Sistemato para marcar cada uma das pendências da automação como processadas
5) api.id-automacao: ID da automação no Sistemato
6) rpa.webdriver.path: caminho até o ChromeDriver
7) rpa.browser-exe.path: caminho até a pasta do executável do Google Chrome 
8) rpa.porta: porta a ser usada pelo navegador 
9) rpa.profile.path: caminho até a pasta criada para armazenar o perfil usado no ChromeDriver 
10) rpa.arquivos.path: caminho até a pasta onde deverão ser incluídos os arquivos .png e .jpeg a serem enviados a cada um dos contatos 
11) rpa.whatsapp.link: link do WhatsappWeb 
12) rpa.google-contatos.link: link do Google Contatos

<br>

### Preparação
1) Configurar via Sistemato os textos que serão enviados como mensagens a cada um dos contatos. Caso não houverem textos a serem enviados, pular este passo.
2) Subir as imagens (.png ou .jpeg) a serem enviadas no diretório "RPAWhatsapp/arquivos" na máquina onde rodará automação. Caso não houverem imagens a serem enviadas, pular este passo.

<br>

### Execução
1) Ao tentar acessar o WhatsappWeb, a automação irá verificar se a máquina já se encontra logada, em caso negativo irá aparecer uma mensagem na tela. O usuário deverá logar manualmente e, após logado, clicar no botão "OK"
2) Ao tentar acessar o GoogleContatos, a automação irá verificar se a máquina já se encontra logada, em caso negativo irá aparecer uma mensagem na tela. O usuário deverá logar manualmente e, após logado, clicar no botão "OK"

<br>

### Sistemato
1) A automação seguirá as configurações aplicadas pelo Sistemato
2) A automação puxará as pendências subidas no Sistemato através de planilhas Excel
3) O Sistemato será atualizado com possíveis falhas da automação ao longo de sua execução
4) A automação registrará no Sistemato cada uma de suas execuções