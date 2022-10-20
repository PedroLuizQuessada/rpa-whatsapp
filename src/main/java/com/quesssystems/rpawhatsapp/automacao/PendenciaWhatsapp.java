package com.quesssystems.rpawhatsapp.automacao;

import automacao.Pendencia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PendenciaWhatsapp extends Pendencia {
    private static List<String> textos = new ArrayList<>();
    private static List<File> arquivos = new ArrayList<>();
    private String numero;

    public static List<String> getTextos() {
        return textos;
    }

    public static void addTexto(String texto) {
        PendenciaWhatsapp.textos.add(texto);
    }

    public static List<File> getArquivos() {
        return arquivos;
    }

    public static void addArquivo(File arquivo) {
        PendenciaWhatsapp.arquivos.add(arquivo);
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }
}
