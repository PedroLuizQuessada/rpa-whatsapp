package com.quesssystems.rpawhatsapp.service;

import automacao.AutomacaoApi;
import automacao.Requisicao;
import com.quesssystems.rpawhatsapp.automacao.PendenciaWhatsapp;
import com.quesssystems.rpawhatsapp.automacao.PendenciaUtil;
import com.quesssystems.rpawhatsapp.exceptions.*;
import enums.NavegadoresEnum;
import enums.UnidadesMedidaTempoEnum;
import exceptions.*;
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

    public RpaService(PendenciaUtil pendenciaUtil, WhatsappService whatsappService) {
        this.pendenciaUtil = pendenciaUtil;
        this.whatsappService = whatsappService;
    }

    public void iniciarAutomacao() {
        logger.info("Iniciando automação...");

        try {
            while (true) {
                logger.info("Recuperando dados da automação...");
                AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRegistrarLog, token, idAutomacao, "Recuperando dados da automação", null));
                AutomacaoApi automacaoApi = AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRecuperarDados, token, idAutomacao, null, null));
                if (automacaoApi.isExecutar(Calendar.getInstance())) {
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
                        pendenciasWhatsapp = formataNumeros(pendenciasWhatsapp);

                        if (!pendenciasWhatsapp.isEmpty()) {
                            logger.info("Acessando site...");
                            WebDriver webDriver = WebdriverUtil.getWebDriver(navegador.toString(), webDriverPath, browserExePath, porta, profilePath);
                            whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarLog, token, idAutomacao);

                            logger.info("Processando pendências...");
                            for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                                whatsappService.processarPendencia(webDriver, pendenciaWhatsapp);
                                TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 2);
                                logger.info("Registrando processamento da pendência...");
                                AutomacaoApiUtil.executarRequisicao(new Requisicao(linkProcessarPendencia, token, idAutomacao, null, pendenciaWhatsapp.getId()));
                            }

                            logger.info("Fechando navegador...");
                            WebdriverUtil.fecharNavegador(webDriver);
                            logger.info("Registrando execução...");
                            AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRegistrarLog, token, idAutomacao, "Automação finalizada", null));
                        }
                    }
                } else {
                    logger.info("Automação fora do período de execução");
                }

                logger.info(String.format("Aguardando intervalo de %d minutos", intervaloMinutos));
                TimerUtil.aguardar(UnidadesMedidaTempoEnum.MINUTOS, intervaloMinutos);
            }
        }
        catch (RecuperarDadosException | TimerUtilException | MensagemVaziaException |
               NavegadorNaoIdentificadoException | DriverException | UrlInvalidaException | ElementoNaoEncontradoException |
               ContatoNaoEncontradoException | CaracterException |
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

    private List<PendenciaWhatsapp> formataNumeros(List<PendenciaWhatsapp> pendenciasWhatsapp) {
        List<PendenciaWhatsapp> pendenciaWhatsappsFormatadas = new ArrayList<>();

        for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
            StringBuilder numero = new StringBuilder(pendenciaWhatsapp.getNumero().replace(".", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", ""));

            if (numero.length() == 0) {
                continue;
            }

            if (numero.toString().contains("E")) {
                numero = new StringBuilder(numero.substring(0, numero.indexOf("E")));
            }

            int numZeros = 11 - numero.toString().length();
            if (numZeros > 0) {
                for (int i = 0; i < numZeros; i++) {
                    numero.append("0");
                }
            }
            numero = new StringBuilder(numero.substring(0, 2) + " " + numero.substring(2));
            pendenciaWhatsapp.setNumero(numero.toString());
            pendenciaWhatsappsFormatadas.add(pendenciaWhatsapp);
        }

        return pendenciaWhatsappsFormatadas;
    }
}
