package hu.agnos.report.server.util;

import java.util.List;
import java.util.Optional;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.cube.meta.http.CubeClient;

/**
 *
 * @author parisek
 */

public class CubeServerClient {

    public static Optional<ResultSet[]> getCubeData(String cubeServerUri, String cubeName, String baseVector, String drillVectorsComrressOneString) {
        return null;
    }

    public static Optional<ResultSet[]> getCubeData(String cubeServerUri, String cubeName, ReportQuery query) {
        return (new CubeClient(cubeServerUri)).getData(cubeName, query);
    }

    public static Optional<List<String>> getCubeHierarchyHeader(String cubeServerUri, String cubeName) {
        return (new CubeClient(cubeServerUri)).getHierarchyHeaderOfCube(cubeName);
    }

    public static Optional<String[]> getCubeMeasureHeaderOfCube(String cubeServerUri, String cubeName) {
        return (new CubeClient(cubeServerUri)).getMeasureHeaderOfCube(cubeName);
    }

    public static Optional<CubeList> getCubeList(String cubeServerUri) {
        return (new CubeClient(cubeServerUri)).getCubesNameAndDate();
    }
}
