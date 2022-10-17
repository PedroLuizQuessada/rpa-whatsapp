package com.quesssystems.rpawhatsapp.exceptions;

public class CadastrarContatoException extends Exception {
    public CadastrarContatoException(String numero) {
        super(String.format("Falha ao cadastrar contato %s", numero));
    }
}
