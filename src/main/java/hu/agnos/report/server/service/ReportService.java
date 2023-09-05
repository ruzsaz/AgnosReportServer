/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hu.agnos.report.server.service;

import java.util.List;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.cube.meta.dto.CubeNameAndDate;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.query.generator.agnos.AgnosQueryGenerator;
import hu.agnos.report.util.JsonMarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author parisek
 */
@Service
public class ReportService {

    @Autowired
    List<Report> reportList;

    @Autowired
    CubeList cubeList;
    
    //TODO: vajaon ez jó-e így, konkurens futásnál lehet-e baj?
    //TODO: Az egészet static-á kéne alakítani
    @Autowired
    AgnosQueryGenerator agnosQueryGenerator;


    public String getReport(String cubeUniqueName, String reportUniqueName) {
        return JsonMarshaller.getJSONFull(getReportEntity(cubeUniqueName, reportUniqueName));
    }

    public String getReportsHeader(SecurityContext context) {

        String s = "{\"reports\":[";
        String origin = new String(s);

        //for (Cube cube : instance.values()) {            
        for (Report report : AccessRoleService.availableForContext(context, this.reportList)) {

            String reportString = JsonMarshaller.getJSONHeader(report) + ",";

            String cubeName = report.getCubeName();
            String databaseType = report.getDatabaseType();

            String createdDateString = "";
            switch (databaseType) {
                case "AGNOS_MOLAP":
                    createdDateString = getCreatedDate(cubeName);
                    break;
            }

            reportString = reportString.replaceAll("ValueOfUpdatedIsOnlyRevealedAtRuntime", createdDateString);
            //if (Authorizator.hasPermission(username, report.getRoleToAccess())) {
                s += reportString;
            //}                
        }
        //}

        if (!origin.equals(s)) {
            s = s.substring(0, (s.length() - 1));
        }

        return s + "]}";
    }

    public Report getReportEntity(String queries) {
        String cubeAndReportName = queries.split(";")[0];
        String cubeUniqueName = cubeAndReportName.split(":")[0];
        String reportUniqueName = cubeAndReportName.split(":")[1];
        return getReportEntity(cubeUniqueName, reportUniqueName);
    }

    private Report getReportEntity(String cubeUniqueName, String reportUniqueName) {
        Report result = null;
        for (Report r : reportList) {
            if (r.getName().equalsIgnoreCase(reportUniqueName)
                    && r.getCubeName().equalsIgnoreCase(cubeUniqueName)) {
                result = r;
                break;
            }
        }
        return result;
    }

    private String getCreatedDate(String cubeName) {
        String result = "";
        if (cubeList != null) {
            for (CubeNameAndDate cubeNameAndDate : cubeList.getCubesNameAndDate()) {
                if (cubeNameAndDate.getName().equals(cubeName)) {
                    result = cubeNameAndDate.getCreatedDate();
                    break;
                }
            }
        }
        return result;
    }
}
