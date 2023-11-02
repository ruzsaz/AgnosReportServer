package hu.agnos.report.server.service;

import static hu.agnos.report.server.util.CubeQueryCreator.createCubeQuery;

import java.util.ArrayList;
import java.util.List;

import hu.agnos.cube.meta.drillDto.CubeQuery;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.cube.meta.dto.CubeMetaDTO;
import hu.agnos.cube.meta.dto.ResultSet;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.query.generator.agnos.ResponseConverter;
import hu.agnos.report.server.util.CubeServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri;

    @Autowired
    ReportService reportService;

    @Autowired
    CubeList cubeList;

    public String getData(Report report, ReportQuery query) {
        ResponseConverter responseConverter = new ResponseConverter(report, query);
        List<ResultSet[]> resultSetsList = new ArrayList<>();
        for (Cube cube: report.getCubes()) {
            // TODO: párhuzamosítani
            CubeMetaDTO cubeMeta = cubeList.cubeMap().get(cube.getName());
            CubeQuery queryForCube = createCubeQuery(report, cube.getName(), cubeMeta, query);

            resultSetsList.add(CubeServerClient.getCubeData(cubeServerUri, queryForCube));

        }
        return responseConverter.getAnswer(resultSetsList).asJson();
    }

    private String joinCubeResults(List<ResultSet[]> resultSetsArray) {

        return "";
    }

}
