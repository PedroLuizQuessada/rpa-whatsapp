package com.quesssystems.rpawhatsapp.automacao;

import automacao.AutomacaoApi;
import automacao.Pendencia;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.ConversaoPendenciaException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class PendenciaUtil extends automacao.PendenciaUtil {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public <T extends Pendencia> List<T> converterPendencia(AutomacaoApi automacaoApi, Integer idAutomacao) throws ConversaoPendenciaException {
        try {
            List<PendenciaWhatsapp> pendenciaWhatsapps = mapper.convertValue(automacaoApi.getPendencias(), new TypeReference<List<PendenciaWhatsapp>>(){});
            for (PendenciaWhatsapp pendenciaWhatsapp : pendenciaWhatsapps) {
                Map<String, Object> map = new ObjectMapper().readValue(pendenciaWhatsapp.getJson(), HashMap.class);
                pendenciaWhatsapp.setNumero(String.valueOf(map.get("numero")));
            }
            return (List<T>) pendenciaWhatsapps;
        }
        catch (JsonProcessingException e) {
            throw new ConversaoPendenciaException(idAutomacao);
        }
    }

    public void recuperaMensagens(AutomacaoApi automacaoApi, String arquivosPath) {
        if (automacaoApi.getTexto() != null && automacaoApi.getTexto().length() > 0) {
            for (String texto : automacaoApi.getTexto().split(";")) {
                PendenciaWhatsapp.addTexto(texto);
            }
        }
        for (File arquivo : Objects.requireNonNull(new File(arquivosPath).listFiles())) {
            if (arquivo.getName().substring(arquivo.getName().lastIndexOf('.')).equals(".png") ||
                    arquivo.getName().substring(arquivo.getName().lastIndexOf('.')).equals(".jpeg"))
                PendenciaWhatsapp.addArquivo(arquivo);
        }
    }

    public List<PendenciaWhatsapp> formataNumeros(List<PendenciaWhatsapp> pendenciasWhatsapp) {
        List<PendenciaWhatsapp> pendenciaWhatsappsFormatadas = new ArrayList<>();

        for (PendenciaWhatsapp pendenciaWhatsapp : pendenciasWhatsapp) {
            StringBuilder numero = new StringBuilder(pendenciaWhatsapp.getNumero().replace(".", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", ""));

            if (numero.length() == 0) {
                continue;
            }

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
            pendenciaWhatsappsFormatadas.add(pendenciaWhatsapp);
        }

        return pendenciaWhatsappsFormatadas;
    }
}
