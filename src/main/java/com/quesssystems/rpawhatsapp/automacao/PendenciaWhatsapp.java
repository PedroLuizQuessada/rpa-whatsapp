package com.quesssystems.rpawhatsapp.automacao;

import automacao.Pendencia;

import java.io.File;

public class PendenciaWhatsapp extends Pendencia {
    private static String texto;
    private static File arquivo;
    private String numero;

    public static String getTexto() {
        return texto;
    }

    public static void setTexto(String texto) {
        PendenciaWhatsapp.texto = texto;
    }

    public static File getArquivo() {
        return arquivo;
    }

    public static void setArquivo(File arquivo) {
        PendenciaWhatsapp.arquivo = arquivo;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }
}
