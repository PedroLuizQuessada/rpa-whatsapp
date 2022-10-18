package com.quesssystems.rpawhatsapp.service;

import automacao.AutomacaoApi;
import automacao.Planilha;
import com.quesssystems.rpawhatsapp.automacao.PendenciaWhatsapp;
import com.quesssystems.rpawhatsapp.automacao.PendenciaUtil;
import com.quesssystems.rpawhatsapp.exceptions.CadastrarContatoException;
import com.quesssystems.rpawhatsapp.exceptions.ContatoNaoCadastroException;
import com.quesssystems.rpawhatsapp.exceptions.MensagemVaziaException;
import enums.StatusEnum;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class RpaService {
    private final Logger logger = LoggerFactory.getLogger(RpaService.class);

    @Value("${api.recuperar-dados.link}")
    private String linkRecuperarDados;

    @Value("${api.registrar-falha.link}")
    private String linkRegistrarFalha;

    @Value("${api.registrar-execucao.link}")
    private String linkRegistrarExecucao;

    @Value("${api.id-automacao}")
    private Integer idAutomacao;

    @Value("${google-drive.path.pendentes}")
    private String googleDrivePathPendentes;

    @Value("${google-drive.path.mensagens}")
    private String googleDrivePathMensagens;

    @Value("${google-drive.path.processados}")
    private String googleDrivePathProcessados;

    @Value("${rpa.intervalo-minutos}")
    private Integer intervaloMinutos;

    @Value("${rpa.navegador}")
    private String navegador;

    @Value("${rpa.webdriver.path}")
    private String webDriverPath;

    private final PendenciaUtil pendenciaUtil;

    private final WhatsappService whatsappService;

    private WebDriver webDriver;

    public RpaService(PendenciaUtil pendenciaUtil, WhatsappService whatsappService) {
        this.pendenciaUtil = pendenciaUtil;
        this.whatsappService = whatsappService;
    }

    public void iniciarAutomacao() {
        logger.info("Iniciando automação...");

        try {
            AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(" ")));
            logger.info("Recuperando dados da automação...");

            AutomacaoApi automacaoApi = AutomacaoApiUtil.executarRequisicao(String.format(linkRecuperarDados, idAutomacao));
            if (automacaoApi.getStatus().equals(StatusEnum.NAOENCONTRADO)) {
                throw new AutomacaoNaoIdentificadaException(idAutomacao);
            }

            if (automacaoApi.isExecutar(Calendar.getInstance())) {
                logger.info("Recuperando pendências...");
                List<Planilha> planilhas = GoogleDriveUtil.recuperarPendencias(googleDrivePathPendentes);
                List<PendenciaWhatsapp> pendenciasWhatsapp = new ArrayList<>();

                if (planilhas.isEmpty()) {
                    logger.info("Sem planilhas pendentes");
                }
                else {
                    logger.info("Recuperando mensagens a serem enviadas...");
                    List<Planilha> planilhasMensagens = GoogleDriveUtil.recuperarPendencias(googleDrivePathMensagens);
                    if (!planilhasMensagens.isEmpty()) {
                        PendenciaWhatsapp.setTexto(planilhasMensagens.get(0).getDados().get(0).get(0));
                    }
                    List<File> imagensMensagens = GoogleDriveUtil.recuperarImagem(googleDrivePathMensagens);
                    if (!imagensMensagens.isEmpty()) {
                        PendenciaWhatsapp.setArquivo(imagensMensagens.get(0));
                    }

                    if (PendenciaWhatsapp.getTexto() == null && PendenciaWhatsapp.getArquivo() == null) {
                        throw new MensagemVaziaException();
                    }

                    logger.info("Convertendo planilhas em pendências...");
                    for (Planilha planilha : planilhas) {
                        pendenciasWhatsapp.addAll(pendenciaUtil.planilhaToPendencias(planilha));
                    }

                    logger.info("Acessando sites...");
                    if (!pendenciasWhatsapp.isEmpty()) {
                        webDriver = WebdriverUtil.getWebDriver(navegador, webDriverPath);
                        whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarFalha, idAutomacao);
                        whatsappService.acessarGoogleContatos(webDriver, linkRegistrarFalha, idAutomacao);
                    }

                    logger.info("Cadastrando contatos...");
                    for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                        whatsappService.cadastrarContato(webDriver, pendenciaWhatsapp.getNumero());
                    }

                    whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarFalha, idAutomacao);

                    logger.info("Processando pendências...");
                    for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                        whatsappService.processarPendencia(webDriver, pendenciaWhatsapp);
                    }

                    logger.info("Movendo pendências...");
                    String nomePlanilhaProcessada = googleDrivePathProcessados + "\\" + ConversorUtil.getDateToString(Calendar.getInstance(), ConversorUtil.getDateToString(Calendar.getInstance(), "dd_MM_yyyy_HH_mm_sss")) + ".xlsx";
                    GoogleDriveUtil.moverPendencias(planilhas, new File(googleDrivePathPendentes), new File(nomePlanilhaProcessada));
                }
            }
            else {
                logger.info("Automação fora do período de execução");
            }

            logger.info("Registrando execução...");
            AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarExecucao, idAutomacao));
            logger.info(String.format("Aguardando intervalo de %d minutos", intervaloMinutos));
            TimerUtil.aguardar(UnidadesMedidaTempoEnum.MINUTOS, intervaloMinutos);
        }
        catch (RecuperarDadosException | ArquivoException | TimerUtilException | MensagemVaziaException |
               NavegadorNaoIdentificadoException | DriverException | UrlInvalidaException | ElementoNaoEncontradoException |
               CadastrarContatoException | ContatoNaoCadastroException | MoverPendenciaException | AutomacaoNaoIdentificadaException e) {
            try {
                AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(e.getMessage())));
            }
            catch (RecuperarDadosException e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
            JOptionPane.showMessageDialog(null, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
