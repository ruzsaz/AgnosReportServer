/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hu.agnos.report.server.service;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.cube.meta.dto.CubeNameAndDate;
import hu.agnos.report.server.service.query.generator.agnos.AgnosQueryGenerator;
import hu.mi.agnos.report.entity.Report;
import hu.mi.agnos.report.exception.WrongCubeName;
import hu.mi.agnos.report.util.JsonMarshaller;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author parisek
 */
@Service
public class CubeService {

    @Autowired
    List<Report> reportList;

    @Autowired
    Optional<CubeList> cubeList;
    
    //TODO: vajaon ez jó-e így, konkurens futásnál lehet-e baj?
    @Autowired
    AgnosQueryGenerator agnosQueryGenerator;

    public String getData(String queries) throws WrongCubeName, Exception, ClassNotFoundException {
        String cubeAndReportName = queries.split(";")[0];
        String cubeUniqueName = cubeAndReportName.split(":")[0];
        String reportUniqueName = cubeAndReportName.split(":")[1];
        String responseString = null;

        String databaseType = getReportEntity(cubeUniqueName, reportUniqueName).getDatabaseType().toUpperCase();

        switch (databaseType) {
            case "AGNOS_MOLAP" -> {
                responseString = agnosQueryGenerator.getResponse(queries);
            }
        }

        return responseString;
    }

    public String getReport(String cubeUniqueName, String reportUniqueName) {
        return JsonMarshaller.getJSONFull(getReportEntity(cubeUniqueName, reportUniqueName));
    }

    public String getReportsHeader() {

        String s = "{\"reports\":[";
        String origin = new String(s);

        //for (Cube cube : instance.values()) {            
        for (Report report : this.reportList) {

            String reportString = JsonMarshaller.getJSONHeader(report) + ",";

            String cubeName = report.getCubeName();
            String databaseType = report.getDatabaseType();

            String createdDateString = "";
            switch (databaseType) {
                case "AGNOS_MOLAP":
                    createdDateString = getCreatedDate(cubeName);
                    break;
            }

            reportString = reportString.replaceAll("zolikaokosdataUpdated", createdDateString);
            //if (Authorizator.hasPermission(username, report.getRoleToAccess())) {
                s += reportString;
            //}                
        }
        //}

        if (!origin.equals(s)) {
            s = s.substring(0, (s.length() - 1));
        }

//        System.out.println(s);
        return s + "]}";
    }

    private Report getReportEntity(String cubeUniqueName, String reportUniqueName) {
        Report result = null;
        for (Report r : reportList) {
            if (r.getName().toUpperCase().equals(reportUniqueName.toUpperCase())
                    && r.getCubeName().toUpperCase().equals(cubeUniqueName.toUpperCase())) {
                result = r;
                break;
            }
        }
        return result;
    }

    private String getCreatedDate(String cubeName) {
        String result = "";
        if (this.cubeList.isPresent()) {
            for (CubeNameAndDate cubeNameAndDate : this.cubeList.get().getCubesNameAndDate()) {
                if (cubeNameAndDate.getName().equals(cubeName)) {
                    result = cubeNameAndDate.getCreatedDate();
                    break;
                }
            }
        }
        return result;
    }
}
