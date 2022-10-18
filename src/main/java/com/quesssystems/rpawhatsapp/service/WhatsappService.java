package com.quesssystems.rpawhatsapp.service;

import com.quesssystems.rpawhatsapp.automacao.PendenciaWhatsapp;
import com.quesssystems.rpawhatsapp.exceptions.CadastrarContatoException;
import com.quesssystems.rpawhatsapp.exceptions.ContaNaoLogadaException;
import com.quesssystems.rpawhatsapp.exceptions.ContatoNaoCadastroException;
import exceptions.ElementoNaoEncontradoException;
import exceptions.RecuperarDadosException;
import exceptions.UrlInvalidaException;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import util.AutomacaoApiUtil;
import util.SeleniumUtil;

import javax.swing.*;
import java.io.File;

@Service
public class WhatsappService {

    @Value("${rpa.whatsapp.link}")
    private String whatsappLink;

    @Value("${rpa.google-contatos.link}")
    private String googleContatosLink;

    @Value("${rpa.texto-primeiro}")
    private Boolean textoPrimeiro;

    public void acessarWhatsappWeb(WebDriver webDriver, String linkRegistrarFalha, Integer idAutomacao) throws UrlInvalidaException, RecuperarDadosException {
        SeleniumUtil.navegar(webDriver, whatsappLink);

        while (true) {
            try {
                verificarContaLogada(webDriver, "Whatsapp Web", "//span[@data-testid='menu']");
                break;
            }
            catch (ContaNaoLogadaException e) {
                AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(e.getMessage())));
                JOptionPane.showMessageDialog(null, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(" ")));
    }

    public void acessarGoogleContatos(WebDriver webDriver, String linkRegistrarFalha, Integer idAutomacao) throws UrlInvalidaException, RecuperarDadosException {
        SeleniumUtil.navegar(webDriver, googleContatosLink);

        while (true) {
            try {
                verificarContaLogada(webDriver, "Google Contatos", "//span[contains(text(), 'Contatos')]");
                break;
            }
            catch (ContaNaoLogadaException e) {
                AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(e.getMessage())));
                JOptionPane.showMessageDialog(null, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(" ")));
    }

    public void verificarContaLogada(WebDriver webDriver, String site, String xpath) throws ContaNaoLogadaException {
        try {
            SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath(xpath));
        }
        catch (ElementoNaoEncontradoException e) {
            throw new ContaNaoLogadaException(site);
        }
    }

    public void cadastrarContato(WebDriver webDriver, String numero) throws UrlInvalidaException, ElementoNaoEncontradoException, CadastrarContatoException {
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//button[@title='Adicionar novo contato']")).click();
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//div[contains(text(), 'Criar um contato')]")).click();
        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//input[@aria-label='Nome']")).sendKeys(numero);
        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//input[@type='tel']")).sendKeys(numero);
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//button[@aria-label='Salvar']")).click();

        try {
            SeleniumUtil.aguardarElementosVisiveis(webDriver, 30, By.xpath("//span[contains(text(), 'Detalhes do contato')]"));
        }
        catch (ElementoNaoEncontradoException e) {
            throw new CadastrarContatoException(numero);
        }
    }

    public void processarPendencia(WebDriver webDriver, PendenciaWhatsapp pendenciaWhatsapp) throws ElementoNaoEncontradoException, UrlInvalidaException, ContatoNaoCadastroException {
        abrirConversa(webDriver, pendenciaWhatsapp.getNumero());
        enviarMensagens(webDriver, PendenciaWhatsapp.getTexto(), PendenciaWhatsapp.getArquivo());
    }

    private void abrirConversa(WebDriver webDriver, String numero) throws ElementoNaoEncontradoException, ContatoNaoCadastroException {
        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//div[@title='Caixa de texto de pesquisa']")).sendKeys(numero + Keys.ENTER);
        try {
            SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath(String.format("//span[@title='%s']", numero))).click();
        }
        catch (ElementoNaoEncontradoException e) {
            throw new ContatoNaoCadastroException(numero);
        }

        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//div[@title='Caixa de texto de pesquisa']")).clear();
    }

    private void enviarMensagens(WebDriver webDriver, String texto, File arquivo) throws ElementoNaoEncontradoException {
        WebElement input = SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//div[@title='Mensagem']"));

        if (textoPrimeiro) {
            if (texto != null) {
                input.sendKeys(texto);
            }
            if (arquivo != null) {
                input.sendKeys(arquivo.getAbsolutePath());
            }
        }
        else {
            if (arquivo != null) {
                input.sendKeys(arquivo.getAbsolutePath());
            }
            if (texto != null) {
                input.sendKeys(texto);
            }
        }
    }
}
