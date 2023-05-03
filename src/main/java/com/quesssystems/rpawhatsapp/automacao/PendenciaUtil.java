package com.quesssystems.rpawhatsapp.automacao;

import automacao.AutomacaoApi;
import automacao.Pendencia;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.ConversaoPendenciaException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
