package hu.agnos.report.server.resultDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record DimElement(String id, String knownId, String name) {

    static ObjectMapper mapper = new ObjectMapper();

    public static DimElement from(String s) {
        try {
            return new DimElement(mapper.readTree(s).get("id").asText(), mapper.readTree(s).get("knownId").asText(), mapper.readTree(s).get("name").asText());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
