package com.quesssystems.rpawhatsapp.exceptions;

public class ArquivoNaoEncontradoException extends Exception {
    public ArquivoNaoEncontradoException(String path) {
        super(String.format("Arquivo %s não encontrado", path));
    }
}
