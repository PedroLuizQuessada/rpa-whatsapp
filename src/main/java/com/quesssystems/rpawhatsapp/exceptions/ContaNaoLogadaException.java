package com.quesssystems.rpawhatsapp.exceptions;

public class ContaNaoLogadaException extends Exception {
    public ContaNaoLogadaException(String site) {
        super(String.format("Conta do %s não está logada", site));
    }
}
