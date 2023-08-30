/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hu.agnos.report.server.util;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.cube.meta.http.CubeClient;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author parisek
 */

public class CubeServerClient {

    public static Optional<ResultSet[]> getCubeData(String cubeServerUri, String cubeName, String baseVector, String drillVectorsComrressOneString) {
        return (new CubeClient()).getData(cubeServerUri, cubeName, baseVector, drillVectorsComrressOneString);
    }

    public static Optional<String[]> getCubeHierarchyHeader(String cubeServerUri, String cubeName) {
        return (new CubeClient()).getHierarchyHeaderOfCube(cubeServerUri, cubeName);
    }

    public static Optional<String[]> getCubeMeasureHeaderOfCube(String cubeServerUri, String cubeName) {
        return (new CubeClient()).getMeasureHeaderOfCube(cubeServerUri, cubeName);
    }

    public static Optional<CubeList> getCubeList(String cubeServerUri) {
        return (new CubeClient()).getCubesNameAndDate(cubeServerUri);
    }
}
