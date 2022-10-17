package com.quesssystems.rpawhatsapp.exceptions;

public class ContatoNaoCadastroException extends Exception {
    public ContatoNaoCadastroException(String numero) {
        super(String.format("Número %s não cadastrado", numero));
    }
}
