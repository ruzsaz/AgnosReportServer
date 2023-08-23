package hu.agnos.report.server.service;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.server.service.query.generator.agnos.AgnosQueryGenerator;
import hu.mi.agnos.report.entity.Report;
import hu.mi.agnos.report.exception.WrongCubeName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataService {


    @Autowired
    ReportService reportService;

    //TODO: vajaon ez jó-e így, konkurens futásnál lehet-e baj?
    //TODO: Az egészet static-á kéne alakítani
    @Autowired
    AgnosQueryGenerator agnosQueryGenerator;


    public String getData(String queries) throws WrongCubeName, Exception, ClassNotFoundException {
        Report report = reportService.getReportEntity(queries);
        String responseString = null;

        String databaseType = report.getDatabaseType().toUpperCase();

        switch (databaseType) {
            case "AGNOS_MOLAP" -> {
                responseString = agnosQueryGenerator.getResponse(queries);
            }
        }

        return responseString;
    }

}
