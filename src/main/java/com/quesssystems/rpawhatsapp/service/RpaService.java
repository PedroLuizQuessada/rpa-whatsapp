package com.quesssystems.rpawhatsapp.service;

import automacao.AutomacaoApi;
import automacao.Requisicao;
import com.quesssystems.rpawhatsapp.automacao.PendenciaWhatsapp;
import com.quesssystems.rpawhatsapp.automacao.PendenciaUtil;
import com.quesssystems.rpawhatsapp.exceptions.*;
import enums.NavegadoresEnum;
import enums.UnidadesMedidaTempoEnum;
import exceptions.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import util.*;

import javax.swing.*;
import java.io.File;
import java.util.*;

@Service
public class RpaService {
    private final Logger logger = LoggerFactory.getLogger(RpaService.class);

    @Value("${api.recuperar-dados.link}")
    private String linkRecuperarDados;

    @Value("${api.registrar-log.link}")
    private String linkRegistrarLog;

    @Value("${api.processar-pendencia.link}")
    private String linkProcessarPendencia;

    @Value("${api.id-automacao}")
    private Integer idAutomacao;

    @Value("${rpa.token}")
    private String token;

    @Value("${rpa.intervalo-minutos}")
    private Integer intervaloMinutos;

    @Value("${rpa.webdriver.path}")
    private String webDriverPath;

    @Value("${rpa.browser-exe.path}")
    private String browserExePath;

    @Value("${rpa.porta}")
    private Integer porta;

    @Value("${rpa.profile.path}")
    private String profilePath;

    @Value("${rpa.arquivos.path}")
    private String arquivosPath;

    private final NavegadoresEnum navegador = NavegadoresEnum.CHROME;

    private final PendenciaUtil pendenciaUtil;

    private final WhatsappService whatsappService;

    private final GoogleContatosService googleContatosService;

    public RpaService(PendenciaUtil pendenciaUtil, WhatsappService whatsappService, GoogleContatosService googleContatosService) {
        this.pendenciaUtil = pendenciaUtil;
        this.whatsappService = whatsappService;
        this.googleContatosService = googleContatosService;
    }

    public void iniciarAutomacao() {
        logger.info("Iniciando automação...");

        try {
            while (true) {
                logger.info("Recuperando dados da automação...");
                AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRegistrarLog, token, idAutomacao, "Recuperando dados da automação", null));
                AutomacaoApi automacaoApi = AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRecuperarDados, token, idAutomacao, null, null));
                if (automacaoApi.isExecutar(Calendar.getInstance())) {
                    logger.info("Recuperando pendências...");

                    if (automacaoApi.getPendencias() == null || automacaoApi.getPendencias().isEmpty()) {
                        logger.info("Sem pendências");
                    } else {
                        logger.info("Recuperando mensagens a serem enviadas...");
                        if (automacaoApi.getTexto() != null && automacaoApi.getTexto().length() > 0) {
                            for (String texto : automacaoApi.getTexto().split(";")) {
                                PendenciaWhatsapp.addTexto(texto);
                            }
                        }
                        for (File arquivo : Objects.requireNonNull(new File(arquivosPath).listFiles())) {
                            if (arquivo.getName().substring(arquivo.getName().lastIndexOf('.')).equals(".png") ||
                                    arquivo.getName().substring(arquivo.getName().lastIndexOf('.')).equals(".jpeg"))
                                PendenciaWhatsapp.addArquivo(arquivo);
                        }

                        if (PendenciaWhatsapp.getTextos().isEmpty() && PendenciaWhatsapp.getArquivos().isEmpty()) {
                            throw new MensagemVaziaException();
                        }

                        logger.info("Convertendo planilhas em pendências...");
                        List<PendenciaWhatsapp> pendenciasWhatsapp = new ArrayList<>(pendenciaUtil.converterPendencia(automacaoApi, idAutomacao));
                        pendenciasWhatsapp = googleContatosService.formataNumeros(pendenciasWhatsapp);

                        if (!pendenciasWhatsapp.isEmpty()) {
                            logger.info("Acessando sites...");
                            WebDriver webDriver = WebdriverUtil.getWebDriver(navegador.toString(), webDriverPath, browserExePath, porta, profilePath);
                            whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarLog, token, idAutomacao);
                            googleContatosService.acessarGoogleContatos(webDriver, linkRegistrarLog, token, idAutomacao);

                            logger.info("Cadastrando contatos...");
                            for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                                googleContatosService.cadastrarContato(webDriver, pendenciaWhatsapp.getNumero());
                            }

                            logger.info("Processando pendências...");
                            logger.info("Realizando sincronização de contatos...");
                            boolean primeiraPendenciaProcessada = false;
                            for (int i = 0; i < 200; i++) {
                                whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarLog, token, idAutomacao);

                                try {
                                    whatsappService.processarPendencia(webDriver, pendenciasWhatsapp.get(0));
                                    TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 2);
                                    primeiraPendenciaProcessada = true;
                                    logger.info("Registrando processamento da pendência...");
                                    AutomacaoApiUtil.executarRequisicao(new Requisicao(linkProcessarPendencia, token, idAutomacao, null, pendenciasWhatsapp.get(0).getId()));
                                    break;
                                }
                                catch (ContatoNaoCadastroException e) {
                                    TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 30);
                                }
                            }

                            if (!primeiraPendenciaProcessada) {
                                throw new ContatoNaoCadastroException(pendenciasWhatsapp.get(0).getNumero());
                            }

                            logger.info("Sincronização de contatos realizada, processando resto das pendências...");
                            for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                                if (pendenciasWhatsapp.indexOf(pendenciaWhatsapp) == 0) {
                                    continue;
                                }
                                whatsappService.processarPendencia(webDriver, pendenciaWhatsapp);
                                TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 2);
                                logger.info("Registrando processamento da pendência...");
                                AutomacaoApiUtil.executarRequisicao(new Requisicao(linkProcessarPendencia, token, idAutomacao, null, pendenciaWhatsapp.getId()));
                            }

                            logger.info("Fechando navegador...");
                            WebdriverUtil.fecharNavegador(webDriver);
                        }
                    }
                } else {
                    logger.info("Automação fora do período de execução");
                }

                logger.info("Registrando execução...");
                AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRegistrarLog, token, idAutomacao, "Automação finalizada", null));
                logger.info(String.format("Aguardando intervalo de %d minutos", intervaloMinutos));
                TimerUtil.aguardar(UnidadesMedidaTempoEnum.MINUTOS, intervaloMinutos);
            }
        }
        catch (RecuperarDadosException | TimerUtilException | MensagemVaziaException |
               NavegadorNaoIdentificadoException | DriverException | UrlInvalidaException | ElementoNaoEncontradoException |
               CadastrarContatoException | ContatoNaoCadastroException | CaracterException |
               RobotException | ArquivoNaoEncontradoException | AutomacaoNaoIdentificadaException | FecharNavegadorException |
               TokenInvalidoException | MensagemInvalidaException | RequisicaoException | ConversaoPendenciaException e) {
            if (!e.getClass().equals(AutomacaoNaoIdentificadaException.class) && !e.getClass().equals(TokenInvalidoException.class)) {
                try {
                    AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRegistrarLog, token, idAutomacao, String.format("Falha: %s", e.getMessage()), null));
                }
                catch (RecuperarDadosException | AutomacaoNaoIdentificadaException | RequisicaoException | TokenInvalidoException | MensagemInvalidaException e1) {
                    JOptionPane.showMessageDialog(null, e1.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
            JOptionPane.showMessageDialog(null, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void verificarContaLogada(WebDriver webDriver, String site, String xpath) throws ContaNaoLogadaException {
        try {
            SeleniumUtil.aguardarElementoVisivel(webDriver, 300, By.xpath(xpath));
        }
        catch (ElementoNaoEncontradoException e) {
            throw new ContaNaoLogadaException(site);
        }
    }
}
