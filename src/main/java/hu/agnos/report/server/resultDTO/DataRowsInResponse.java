package hu.agnos.report.server.resultDTO;

import java.util.List;

public record DataRowsInResponse(List<DimsAndValues> rows) {}