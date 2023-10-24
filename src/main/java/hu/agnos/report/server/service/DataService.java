package hu.agnos.report.server.service;

import java.util.ArrayList;
import java.util.List;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.query.generator.agnos.AgnosQueryGenerator;
import hu.agnos.report.server.service.query.generator.agnos.ResponseConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    @Autowired
    ReportService reportService;

    @Autowired
    AgnosQueryGenerator agnosQueryGenerator;

//    public String getData(String queries) throws Exception {
//        //Report report = reportService.getReportEntity(queries);
//        //String responseString = null;
//        return agnosQueryGenerator.getResponse(queries);
//    }

    public String getData(Report report, ReportQuery query) {
        ResponseConverter responseConverter = new ResponseConverter(report, query);
        List<ResultSet[]> resultSetsList = new ArrayList<>();
        for (Cube cube: report.getCubes()) {
            // TODO: párhuzamosítani
            resultSetsList.add(agnosQueryGenerator.getResponse(cube, query));

        }
        return responseConverter.getAnswer(resultSetsList).asJson();
    }

    private String joinCubeResults(List<ResultSet[]> resultSetsArray) {

        return "";
    }

}
