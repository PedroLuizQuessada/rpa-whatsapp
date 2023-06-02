package com.quesssystems.rpawhatsapp.exceptions;

public class ContatoNaoEncontradoException extends Exception {
    public ContatoNaoEncontradoException(String numero) {
        super(String.format("Número %s não cadastrado", numero));
    }
}
