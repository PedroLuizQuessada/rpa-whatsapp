package com.quesssystems.rpawhatsapp.service;

import automacao.Requisicao;
import com.quesssystems.rpawhatsapp.automacao.PendenciaWhatsapp;
import com.quesssystems.rpawhatsapp.exceptions.ArquivoNaoEncontradoException;
import com.quesssystems.rpawhatsapp.exceptions.ContatoNaoEncontradoException;
import enums.UnidadesMedidaTempoEnum;
import exceptions.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import util.AutomacaoApiUtil;
import util.RobotUtil;
import util.SeleniumUtil;
import util.TimerUtil;

import javax.swing.*;
import java.io.File;
import java.util.List;

@Service
public class WhatsappService {
    private static final String LINK_WHATSAPP = "https://web.whatsapp.com/";
    private static final String LINK_CHAT_DIRETO = "https://chatdireto.com/";

    @Value("${rpa.texto-primeiro}")
    private Boolean textoPrimeiro;

    public void acessarWhatsappWeb(WebDriver webDriver, String linkRegistrarLog, String token, Integer idAutomacao) throws UrlInvalidaException, RecuperarDadosException, AutomacaoNaoIdentificadaException, MensagemInvalidaException, TokenInvalidoException, RequisicaoException {
        SeleniumUtil.navegar(webDriver, LINK_WHATSAPP);

        while (true) {
            try {
                SeleniumUtil.aguardarElementoVisivel(webDriver, 300, By.xpath("//span[@data-testid='menu']"));
                break;
            }
            catch (ElementoNaoEncontradoException e) {
                String mensagem = "Conta do WhatsApp não está logada";
                AutomacaoApiUtil.executarRequisicao(new Requisicao(linkRegistrarLog, token, idAutomacao, mensagem, null));
                JOptionPane.showMessageDialog(null, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void processarPendencia(WebDriver webDriver, PendenciaWhatsapp pendenciaWhatsapp) throws ElementoNaoEncontradoException, UrlInvalidaException, ContatoNaoEncontradoException, CaracterException, RobotException, TimerUtilException, ArquivoNaoEncontradoException {
        abrirConversa(webDriver, pendenciaWhatsapp.getNumero());
        enviarMensagens(webDriver, PendenciaWhatsapp.getTextos(), PendenciaWhatsapp.getArquivos());
    }

    private void abrirConversa(WebDriver webDriver, String numero) throws ElementoNaoEncontradoException, ContatoNaoEncontradoException, TimerUtilException, UrlInvalidaException {
        SeleniumUtil.navegar(webDriver, LINK_CHAT_DIRETO);
        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//input[@name='telefone']")).sendKeys(numero);
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//button[contains(text(), 'Abrir WhatsApp!')]")).click();

        try {
            SeleniumUtil.aguardarElementoVisivel(webDriver, 60, By.xpath("//button[@aria-label='Mensagem de voz']"));
            TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 2);
        }
        catch (ElementoNaoEncontradoException e) {
            throw new ContatoNaoEncontradoException(numero);
        }
    }

    private void enviarMensagens(WebDriver webDriver, List<String> textos, List<File> arquivos) throws ElementoNaoEncontradoException, RobotException, CaracterException, TimerUtilException, ArquivoNaoEncontradoException {
        WebElement input = SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//div[@title='Mensagem']"));

        if (textoPrimeiro) {
            if (textos != null) {
                for (String texto : textos) {
                    input.sendKeys(texto + Keys.ENTER);
                }
            }
            if (arquivos != null) {
                for (File arquivo : arquivos) {
                    enviarArquivo(webDriver, arquivo);
                }
            }
        }
        else {
            if (arquivos != null) {
                for (File arquivo : arquivos) {
                    enviarArquivo(webDriver, arquivo);
                }
            }
            if (textos != null) {
                for (String texto : textos) {
                    input.sendKeys(texto + Keys.ENTER);
                }
            }
        }
    }

    private void enviarArquivo(WebDriver webDriver, File arquivo) throws ElementoNaoEncontradoException, CaracterException, RobotException, TimerUtilException, ArquivoNaoEncontradoException {
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//span[@data-testid='clip']")).click();
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//span[@data-testid='attach-image']")).click();
        RobotUtil.escreverTexto(arquivo.getAbsolutePath());
        RobotUtil.pressionarEnter();
        TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 1);
        try {
            SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//span[@data-testid='send']")).click();
        }
        catch (ElementoNaoEncontradoException e) {
            throw new ArquivoNaoEncontradoException(arquivo.getAbsolutePath());
        }
    }
}
