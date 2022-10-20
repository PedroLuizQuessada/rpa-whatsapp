package com.quesssystems.rpawhatsapp.service;

import com.quesssystems.rpawhatsapp.automacao.PendenciaWhatsapp;
import com.quesssystems.rpawhatsapp.exceptions.CadastrarContatoException;
import com.quesssystems.rpawhatsapp.exceptions.ContaNaoLogadaException;
import enums.UnidadesMedidaTempoEnum;
import exceptions.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import util.AutomacaoApiUtil;
import util.SeleniumUtil;
import util.TimerUtil;

import javax.swing.*;
import java.util.List;

@Service
public class GoogleContatosService {

    @Value("${rpa.google-contatos.link}")
    private String googleContatosLink;

    public void formataNumeros(List<PendenciaWhatsapp> pendenciasWhatsapp) {
        for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
            StringBuilder numero = new StringBuilder(pendenciaWhatsapp.getNumero().replace(".", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", ""));
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
        }
    }

    public void acessarGoogleContatos(WebDriver webDriver, String linkRegistrarFalha, Integer idAutomacao) throws UrlInvalidaException, RecuperarDadosException, AutomacaoNaoIdentificadaException {
        SeleniumUtil.navegar(webDriver, googleContatosLink);

        while (true) {
            try {
                RpaService.verificarContaLogada(webDriver, "Google Contatos", "//span[contains(text(), 'Contatos')]");
                break;
            }
            catch (ContaNaoLogadaException e) {
                AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(e.getMessage())), idAutomacao);
                JOptionPane.showMessageDialog(null, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        AutomacaoApiUtil.executarRequisicao(String.format(linkRegistrarFalha, idAutomacao, AutomacaoApiUtil.converterMensagemParaRequisicao(" ")), idAutomacao);
    }

    public void cadastrarContato(WebDriver webDriver, String numero) throws UrlInvalidaException, ElementoNaoEncontradoException, CadastrarContatoException, TimerUtilException {
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//button[@title='Adicionar novo contato']")).click();
        TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 3);
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//div[contains(text(), 'Criar um contato')]")).click();
        TimerUtil.aguardar(UnidadesMedidaTempoEnum.SEGUNDOS, 3);
        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//input[@aria-label='Nome']")).sendKeys(numero);
        SeleniumUtil.aguardarElementoVisivel(webDriver, 10, By.xpath("//input[@type='tel']")).sendKeys(numero);
        SeleniumUtil.aguardarElementoClicavel(webDriver, 10, By.xpath("//button[@aria-label='Salvar']")).click();

        try {
            SeleniumUtil.aguardarElementoVisivel(webDriver, 60, By.xpath("//div[contains(text(), 'Novo contato criado')]"));
        }
        catch (ElementoNaoEncontradoException e) {
            throw new CadastrarContatoException(numero);
        }
        SeleniumUtil.navegar(webDriver, googleContatosLink);
    }
}
