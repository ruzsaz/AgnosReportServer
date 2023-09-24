package hu.agnos.report.server.service;

import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.query.generator.agnos.AgnosQueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataService {

    @Autowired
    ReportService reportService;

    @Autowired
    AgnosQueryGenerator agnosQueryGenerator;

    public String getData(String queries) throws Exception {
        Report report = reportService.getReportEntity(queries);
        String responseString = null;

        String databaseType = report.getDatabaseType().toUpperCase();

        if (databaseType.equals("AGNOS_MOLAP")) {
            responseString = agnosQueryGenerator.getResponse(queries);
        }

        return responseString;
    }

}
