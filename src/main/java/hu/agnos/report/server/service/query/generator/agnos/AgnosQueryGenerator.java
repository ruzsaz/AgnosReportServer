/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.agnos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import hu.agnos.cube.driver.CubeHandler;
import hu.agnos.cube.driver.ResultSet;
import hu.agnos.report.entity.AdditionalCalculation;
import hu.agnos.report.entity.Report;
import hu.agnos.report.repository.ReportRepository;
import hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier.KaplanMeierMain;
import hu.agnos.report.server.util.CubeServerClient;
import hu.agnos.report.server.util.DrillVectorCompressor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author parisek
 */
@Service
public class AgnosQueryGenerator {

    private final static String MAIN_SEPARATOR = ";";

    private final static String SECONDARY_SEPARATOR = ":";

    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri;


    public AgnosQueryGenerator() {
    }

    public String getResponse(String message) throws SQLException, ClassNotFoundException, InterruptedException, Exception {
        List<AgnosQueryPostProcessor> postProcessorPool = new ArrayList();
        StringBuilder response = new StringBuilder();

        String[] messageArray = message.split(MAIN_SEPARATOR);
        String cubeName = messageArray[0].split(SECONDARY_SEPARATOR)[0];
        String reportName = messageArray[0].split(SECONDARY_SEPARATOR)[1];

        Optional<String[]> optionalHierarchyHeader = CubeServerClient.getCubeHierarchyHeader(cubeServerUri, cubeName);
        Optional<String[]> optionalMeasureHeader = CubeServerClient.getCubeMeasureHeaderOfCube(cubeServerUri, cubeName);
        Optional<Report> optionalReport = (new ReportRepository()).findById(cubeName, reportName);
        //Report report = ModelSingleton.getInstance().getReport(cubeName, reportName);        
        //Cube cube = MOLAPCubeSingleton.getCube(cubeName);       
        if (optionalReport.isPresent() && optionalHierarchyHeader.isPresent() && optionalMeasureHeader.isPresent()) {
            Report report = optionalReport.get();
            String[] hierarchyHeader = optionalHierarchyHeader.get();
            String[] measureHeader = optionalMeasureHeader.get();
            CubeHandler cubeHandler = new CubeHandler(hierarchyHeader, measureHeader);

            String baseVector = messageArray[1];
            String[] drillVectors = new String[messageArray.length - 2];

            for (int i = 2; i < messageArray.length; i++) {
                drillVectors[i - 2] = messageArray[i];
            }

            int hierarchiesSize = optionalHierarchyHeader.get().length;
            //a temp megmondja, hogy a MOLAP kockában szereplő hierarchia a riporton belül
            //hanyadik indexen szerepel
            int[] temp = new int[hierarchiesSize];
            for (int i = 0; i < hierarchiesSize; i++) {
                temp[i] = report.getHierarchyIdxByUniqueName(hierarchyHeader[i]);
            }

            //preprocess
            AgnosQueryPreProcessor preProcessor = new AgnosQueryPreProcessor();

            String newBaseVector = preProcessor.besaVectorConverter(temp, baseVector);
            int drillVectorsSize = drillVectors.length;
            String[] drillVectorsArray = new String[drillVectorsSize];
            for (int i = 0; i < drillVectorsSize; i++) {
                drillVectorsArray[i] = preProcessor.drillVectorConverter(temp, drillVectors[i]);
                postProcessorPool.add(new AgnosQueryPostProcessor(drillVectors[i], baseVector, report, cubeHandler));
            }

            String drillVectorsComrressOneString = DrillVectorCompressor.compressDrillVectorsInOneString(drillVectorsArray);

            ResultSet[] resultSets = null;

            if (report.isAdditionalCalculation()) {
                AdditionalCalculation addCal = report.getAdditionalCalculation();
                if (addCal.getFunction().equalsIgnoreCase("KaplanMeier")) {
                    KaplanMeierMain kmAC = new KaplanMeierMain(addCal.getArgs(), cubeName, hierarchyHeader, measureHeader, cubeServerUri);
                    resultSets = kmAC.process(newBaseVector, drillVectorsArray);
                }
            } else {
                //process
                //TODO
                Optional<ResultSet[]> optionalResultSet = CubeServerClient.getCubeData(cubeServerUri, cubeName, baseVector, drillVectorsComrressOneString);
                if (optionalResultSet.isPresent()) {
                    resultSets = optionalResultSet.get();
                }
            }

            //postprocess       
            response.append("[");
            for (int i = 0; i < resultSets.length; i++) {
                postProcessorPool.get(i).setResultSet(resultSets[i]);
                String json = postProcessorPool.get(i).getResult().toString();
                response.append(json).append(",");
            }
        }
        response = response.replace(response.length() - 1, response.length(), "").append("]");
        return response.toString();
    }

}
