package com.quesssystems.rpawhatsapp.exceptions;

public class MensagemVaziaException extends Exception {
    public MensagemVaziaException() {
        super("Não foi encontrado uma mensagem para ser enviada");
    }
}
