package com.quesssystems.rpawhatsapp.service;

import automacao.AutomacaoApi;
import automacao.Planilha;
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

    @Value("${rpa.webdriver.path}")
    private String webDriverPath;

    @Value("${rpa.browser-exe.path}")
    private String browserExePath;

    @Value("${rpa.porta}")
    private Integer porta;

    @Value("${rpa.profile.path}")
    private String profilePath;

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
            AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(" ")), idAutomacao);

            logger.info("Recuperando dados da automação...");
            AutomacaoApi automacaoApi = AutomacaoApiUtil.executarRequisicao(String.format(linkRecuperarDados, idAutomacao), idAutomacao);
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
                    if (!planilhasMensagens.isEmpty() && !planilhasMensagens.get(0).getDados().isEmpty()) {
                        for (String texto : planilhasMensagens.get(0).getDados().get(0)) {
                            if (texto.length() > 0) {
                                PendenciaWhatsapp.addTexto(texto);
                            }
                        }
                    }
                    List<File> imagensMensagens = GoogleDriveUtil.recuperarImagem(googleDrivePathMensagens);
                    for (File imagemMensagem : imagensMensagens) {
                        PendenciaWhatsapp.addArquivo(imagemMensagem);
                    }

                    if (PendenciaWhatsapp.getTextos().isEmpty() && PendenciaWhatsapp.getArquivos().isEmpty()) {
                        throw new MensagemVaziaException();
                    }

                    logger.info("Convertendo planilhas em pendências...");
                    for (Planilha planilha : planilhas) {
                        pendenciasWhatsapp.addAll(pendenciaUtil.planilhaToPendencias(planilha));
                    }
                    googleContatosService.formataNumeros(pendenciasWhatsapp);

                    if (!pendenciasWhatsapp.isEmpty()) {
                        logger.info("Acessando sites...");
                        WebDriver webDriver = WebdriverUtil.getWebDriver(navegador.toString(), webDriverPath, browserExePath, porta, profilePath);
                        whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarFalha, idAutomacao);
                        googleContatosService.acessarGoogleContatos(webDriver, linkRegistrarFalha, idAutomacao);

                        logger.info("Cadastrando contatos...");
                        for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                            googleContatosService.cadastrarContato(webDriver, pendenciaWhatsapp.getNumero());
                        }

                        whatsappService.acessarWhatsappWeb(webDriver, linkRegistrarFalha, idAutomacao);

                        logger.info("Processando pendências...");
                        for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
                            whatsappService.processarPendencia(webDriver, pendenciaWhatsapp);
                            TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 2);
                        }

                        logger.info("Fechando navegador...");
                        WebdriverUtil.fecharNavegador(webDriver);
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
            AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarExecucao, idAutomacao), idAutomacao);
            logger.info(String.format("Aguardando intervalo de %d minutos", intervaloMinutos));
            TimerUtil.aguardar(UnidadesMedidaTempoEnum.MINUTOS, intervaloMinutos);
        }
        catch (RecuperarDadosException | ArquivoException | TimerUtilException | MensagemVaziaException |
               NavegadorNaoIdentificadoException | DriverException | UrlInvalidaException | ElementoNaoEncontradoException |
               CadastrarContatoException | ContatoNaoCadastroException | MoverPendenciaException | CaracterException |
               RobotException | ArquivoNaoEncontradoException | AutomacaoNaoIdentificadaException | FecharNavegadorException e) {
            if (!e.getClass().equals(AutomacaoNaoIdentificadaException.class)) {
                try {
                    AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(e.getMessage())), idAutomacao);
                }
                catch (RecuperarDadosException | AutomacaoNaoIdentificadaException e1) {
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
