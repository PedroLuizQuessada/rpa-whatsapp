package com.quesssystems.rpawhatsapp.automacao;

import automacao.Pendencia;
import automacao.Planilha;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PendenciaUtil extends automacao.PendenciaUtil {
    @Override
    public <T extends Pendencia> List<T> planilhaToPendencias(Planilha planilha) {
        List<PendenciaWhatsapp> pendencias = new ArrayList<>();
        for (List<String> linha : planilha.getDados()) {
            if (linha.isEmpty()) {
                continue;
            }

            PendenciaWhatsapp pendencia = new PendenciaWhatsapp();
            pendencia.setPlanilha(planilha);
            pendencia.setNumero(linha.get(0));

            pendencias.add(pendencia);
        }

        return (List<T>) pendencias;
    }
}
