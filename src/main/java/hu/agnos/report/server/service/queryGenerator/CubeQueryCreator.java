package hu.agnos.report.server.service.queryGenerator;

import java.util.ArrayList;
import java.util.List;

import hu.agnos.cube.meta.queryDto.BaseVectorCoordinate;
import hu.agnos.cube.meta.queryDto.BaseVectorCoordinateForCube;
import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.queryDto.DrillScenario;
import hu.agnos.cube.meta.queryDto.DrillVector;
import hu.agnos.cube.meta.queryDto.DrillVectorForCube;
import hu.agnos.cube.meta.queryDto.ReportQuery;
import hu.agnos.cube.meta.resultDto.CubeMetaDTO;
import hu.agnos.cube.meta.resultDto.DimensionDTO;
import hu.agnos.report.entity.Dimension;
import hu.agnos.report.entity.Report;

import static hu.agnos.report.server.util.SetFunctions.limitList;

/**
 * Transforms a query from the frontend to a personalized query for a Cube. The personalized query contains base- and
 * drill vectors that the Cube can answer, translated to the Cube's own dimensions.
 */
public class CubeQueryCreator {

    private final Report report; // The Report under process
    private final String cubeName; // Name of the target cube
    private final CubeMetaDTO cubeMeta; // Meta of the target cube

    /**
     * Creates a transformer, to transform a query from the frontend to a personalized query for a Cube. The
     * personalized query contains base- and drill vectors that the Cube can answer, translated to the Cube's own
     * dimensions.
     *
     * @param report The report whose queries the transformer expected to process
     * @param cubeName The cube's name whom the query will be sent
     * @param cubeMeta The cube's meta whom the query will be sent
     */
    public CubeQueryCreator(Report report, String cubeName, CubeMetaDTO cubeMeta) {
        this.report = report;
        this.cubeName = cubeName;
        this.cubeMeta = cubeMeta;
    }

    /**
     * Creates a personalized query for a given cube. The query contains only the dimensions and drills present in the
     * cube, to the level available in the cube.
     *
     * @param query The report's query to personalize for the cube
     * @return The personalized query
     */
    public List<CubeQuery> createCubeQuery(ReportQuery query) {
        List<BaseVectorCoordinateForCube> baseVectorForCube = createBaseVectorForCube(query.baseVector());
        List<DrillVectorForCube> drillVectorsForCube = createDrillVectorsForCube(baseVectorForCube, query.drillVectors());
        int numberOfDrills = query.drillVectors().size();
        List<CubeQuery> cubeQueries = new ArrayList<>(numberOfDrills);
        for (int i = 0; i < numberOfDrills; i++) {
            cubeQueries.add(new CubeQuery(cubeName, query.drillVectors().get(i), baseVectorForCube, drillVectorsForCube.get(i)));
        }
        return cubeQueries;
    }

    /**
     * Creates a personalized base vector for a cube from a base vector of the report. The created base vector contains
     * only the dimensions present in the cube, and only to the depth available in the cube.
     *
     * @param baseVector Original base vector in the report
     * @return The personalized base vector, like [ "2016", "06" ]
     */
    private List<BaseVectorCoordinateForCube> createBaseVectorForCube(List<BaseVectorCoordinate> baseVector) {
        List<DimensionDTO> dimensionHeader = cubeMeta.dimensionHeader();
        List<BaseVectorCoordinateForCube> result = new ArrayList<>(dimensionHeader.size());
        for (DimensionDTO dimension : dimensionHeader) {
            int maxDepth = dimension.maxDepth();
            String levelValueString = "";
            for (BaseVectorCoordinate coordinate : baseVector) {
                if (dimension.name().equals(coordinate.name())) {
                    levelValueString = String.join(",", CubeQueryCreator.limitDepth(coordinate.levelValues(), maxDepth));
                    break;
                }
            }
            result.add(new BaseVectorCoordinateForCube(dimension.name(), levelValueString));
        }
        return result;
    }

    /**
     * Creates a list of drillVectors personalized to the cube from the drill vectors of the report. A result drill
     * vector is limited only the available dimensions in the cube, and a drill in a dimension is requested only if
     * there is a level below the base level in the dimension.
     *
     * @param baseVectorForCube Base vector in the cube to start the drill from
     * @param drillVectors List of original drill vectors in the report
     * @return A list of the personalized drill vectors, like [ [false, false, true], [false, false, false] ]
     */
    private List<DrillVectorForCube> createDrillVectorsForCube(List<BaseVectorCoordinateForCube> baseVectorForCube, List<DrillVector> drillVectors) {
        List<DrillVectorForCube> result = new ArrayList<>(drillVectors.size());
        for (DrillVector drillVector : drillVectors) {
            result.add(CreateSingleDrillVectorForCube(baseVectorForCube, drillVector));
        }
        return result;
    }

    /**
     * Limits a drill-down path in a dimension to a given depth, like [ "2016", "06" ] + depth of 2 -> [ "2016" ] (top
     * level = depth of 1, "2016" = depth of 2)
     *
     * @param list List of the elements in the path
     * @param limit Depth to limit
     * @return Drill-down path limited to the requested depth
     */
    private static List<String> limitDepth(List<String> list, int limit) {
        return limitList(list, limit);
    }

    /**
     * Creates a drillVector personalized to the cube from a drill vector of the report. The result is limited only the
     * available dimensions in the cube, and a drill in a dimension is requested only if there is a level below the base
     * level in the dimension.
     *
     * @param baseVectorForCube Base vector in the cube to start the drill from
     * @param drillVector Original drill vector in the report, like ["SEX", "TIME"]
     * @return The personalized drill vector, like [false, false, true]
     */
    private DrillVectorForCube CreateSingleDrillVectorForCube(List<BaseVectorCoordinateForCube> baseVectorForCube, DrillVector drillVector) {
        List<DimensionDTO> dimensionHeader = cubeMeta.dimensionHeader();
        int dimNumber = dimensionHeader.size();
        DrillScenario[] result = new DrillScenario[dimNumber];
        for (int i = 0; i < dimNumber; i++) {
            DimensionDTO dim = dimensionHeader.get(i);
            Dimension dimensionInReport = report.getDimensionByName(dim.name());
            int allowedDepth = (dimensionInReport == null) ? -1 : dimensionInReport.getAllowedDepth();
            result[i] = CubeQueryCreator.determineDrillInDimensionMode(allowedDepth, drillVector.dimsToDrill(), dim, baseVectorForCube.get(i));
        }
        return new DrillVectorForCube(result);
    }

    /**
     * Determines the drill mode of a cube at a specific dimension, given the base level.
     *
     * @param depthAllowedInReport The maximal depth allowed by the report in the given dimension (0: root
     *         level, etc...)
     * @param dimsToDrill List of the requested drill dimension names, like [ "TIME", "SEX" ]
     * @param dimension The dimension of the cube the drill should be evaluated
     * @param baseVectorCoordinate Base vector value in the dimension, like "2016,06"
     * @return The allowed DrillScenario, like DRILL, or NOT_REQUESTED, etc...
     */
    private static DrillScenario determineDrillInDimensionMode(int depthAllowedInReport, List<String> dimsToDrill, DimensionDTO dimension, BaseVectorCoordinateForCube baseVectorCoordinate) {
        if (dimsToDrill.contains(dimension.name())) { // If drill is requested in the dimension
            int baseVectorDepth = CubeQueryCreator.depthCodedInPath(baseVectorCoordinate.levelValuesString());
            if (depthAllowedInReport > baseVectorDepth) {
                if (dimension.maxDepth() > baseVectorDepth) {
                    return DrillScenario.DRILL;
                }
                return DrillScenario.REQUESTED_BUT_LEVEL_MISSING;
            }
            return DrillScenario.REQUESTED_BUT_ALLOWED_LEVEL_REACHED;
        }
        return DrillScenario.NOT_REQUESTED;
    }

    /**
     * Returns the depth of a vector-coordinate, coded as a drill-path.
     *
     * @param path The path, like "2016,04"
     * @return Depth of the element (base:0, "2016":1, "2016,04":2
     */
    private static int depthCodedInPath(String path) {
        if (path.isEmpty()) {
            return 0;
        }
        return (int) path.chars().filter(i -> i == ',').count() + 1;
    }

}
