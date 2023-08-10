/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.agnos;

import hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier.KaplanMeierMain;
import hu.agnos.cube.driver.CubeHandler;
import hu.agnos.cube.driver.ResultSet;
import hu.agnos.report.server.util.CubeServerClient;
import hu.agnos.report.server.util.DrillVectorCompressor;
import hu.mi.agnos.report.entity.Report;
import hu.mi.agnos.report.repository.ReportRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author parisek
 */
@Service
public class AgnosQueryGenerator {

    private final static String MAIN_SEPARATOR = ";";

    private final static String SECONDARY_SEPARATOR = ":";

    @Autowired
    private CubeServerClient cubeServerClient;

    public AgnosQueryGenerator() {
    }

    public String getResponse(String message) throws SQLException, ClassNotFoundException, InterruptedException, Exception {
        List<AgnosQueryPostProcessor> postProcessorPool = new ArrayList();
        StringBuilder response = new StringBuilder();

        String[] messageArray = message.split(MAIN_SEPARATOR);
        String cubeName = messageArray[0].split(SECONDARY_SEPARATOR)[0];
        String reportName = messageArray[0].split(SECONDARY_SEPARATOR)[1];

        Optional<String[]> optionalHierarchyHeader = cubeServerClient.getCubeHierarchyHeader(cubeName);
        Optional<String[]> optionalMeasureHeader = cubeServerClient.getCubeMeasureHeaderOfCube(cubeName);
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
                if (report.getAdditionalCalculation().getFunction().equals("KaplanMeier")) {
                    KaplanMeierMain kmAC = new KaplanMeierMain(report.getAdditionalCalculation().getArgs(), cubeName, hierarchyHeader, measureHeader, cubeServerClient);
                    resultSets = kmAC.process(newBaseVector, drillVectorsArray);
                }
            } else {
                //process
                //TODO
                Optional<ResultSet[]> optionalResultSet = cubeServerClient.getCubeData(cubeName, baseVector, drillVectorsComrressOneString);
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
