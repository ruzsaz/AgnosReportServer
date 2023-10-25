package hu.agnos.report.server.resultDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.agnos.molap.dimension.DimValue;

// TODO: lehet, hogy erre nincs is szükség, hisz ugyanaz mint a DimValue?
public record DimElement(String id, String knownId, String name) {

    static ObjectMapper mapper = new ObjectMapper();

    public static DimElement from(DimValue s) {
        return new DimElement(s.id(), s.knownId(), s.name());
    }

}
