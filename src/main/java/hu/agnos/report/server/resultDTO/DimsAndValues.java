package hu.agnos.report.server.resultDTO;

import java.util.List;

public record DimsAndValues(List<DimElement> dims, List<ValueElement> vals) {}
