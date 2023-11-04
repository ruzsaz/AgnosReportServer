package hu.agnos.report.server.resultDTO;

import java.util.List;

import hu.agnos.cube.meta.resultDto.NodeDTO;

public record DimsAndValues(List<NodeDTO> dims, List<ValueElement> vals) {}
