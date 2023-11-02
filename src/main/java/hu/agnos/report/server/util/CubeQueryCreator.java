package hu.agnos.report.server.util;

import static hu.agnos.report.server.util.SetFunctions.limitList;

import java.util.ArrayList;
import java.util.List;

import hu.agnos.cube.meta.drillDto.BaseVectorCoordinate;
import hu.agnos.cube.meta.drillDto.BaseVectorCoordinateForCube;
import hu.agnos.cube.meta.drillDto.CubeQuery;
import hu.agnos.cube.meta.drillDto.DrillScenario;
import hu.agnos.cube.meta.drillDto.DrillVector;
import hu.agnos.cube.meta.drillDto.DrillVectorForCube;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.cube.meta.dto.CubeMetaDTO;
import hu.agnos.cube.meta.dto.DimensionDTO;
import hu.agnos.report.entity.Report;

public class CubeQueryCreator {

    /**
     * Creates a personalized query for a given cube.
     * The query contains only the dimensions and drills present in the cube,
     * to the level available in the cube.
     *
     * @param report The Report under process
     * @param cubeName Name of the target cube
     * @param cubeMeta Meta of the target cube
     * @param query The report's query to personalize for the cube
     * @return The personalized query
     */
    public static CubeQuery createCubeQuery(Report report, String cubeName, CubeMetaDTO cubeMeta, ReportQuery query) {
        List<BaseVectorCoordinateForCube> baseVectorForCube = createBaseVectorForCube(cubeMeta.dimensionHeader(), query.baseVector());
        List<DrillVectorForCube> drillVectorsForCube = createDrillVectorsForCube(report, cubeMeta.dimensionHeader(), baseVectorForCube, query.drillVectors());
        return new CubeQuery(cubeName, query.drillVectors(), baseVectorForCube, drillVectorsForCube);
    }

    /**
     * Creates a personalized base vector for a cube from a base vector of the report.
     * The created base vector contains only the dimensions present in the cube,
     * and only to the depth available in the cube.
     *
     * @param dimensionHeader The dimensions in the cube
     * @param baseVector Original base vector in the report
     * @return The personalized base vector, like [ "2016", "06" ]
     */
    private static List<BaseVectorCoordinateForCube> createBaseVectorForCube(List<DimensionDTO> dimensionHeader, List<BaseVectorCoordinate> baseVector) {
        List<BaseVectorCoordinateForCube> result = new ArrayList<>();
        for (DimensionDTO dimension : dimensionHeader) {
            int maxDepth = dimension.maxDepth();
            for (BaseVectorCoordinate coordinate : baseVector) {
                if (dimension.name().equals(coordinate.name())) {
                    result.add(new BaseVectorCoordinateForCube(coordinate.name(), String.join(",", limitDepth(coordinate.levelValues(), maxDepth))));
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Limits a drill-down path in a dimension to a given depth,
     * like [ "2016", "06" ] + depth of 2 -> [ "2016" ]
     * (top level = depth of 1, "2016" = depth of 2)
     *
     * @param list List of the elements in the path
     * @param limit Depth to limit
     * @return Drill-down path limited to the requested depth
     */
    private static List<String> limitDepth(List<String> list, int limit) {
        return limitList(list, limit);
    }

    /**
     * Creates a list of drillVectors personalized to the cube from the drill vectors of the report.
     * A result drill vector is limited only the available dimensions in the cube,
     * and a drill in a dimension is requested only if there is a level below the base level
     * in the dimension.
     *
     * @param report The Report under process
     * @param dimensionHeader Dimensions in the cube
     * @param baseVectorForCube Base vector in the cube to start the drill from
     * @param drillVectors List of original drill vectors in the report
     * @return A list of the personalized drill vectors, like [ [false, false, true], [false, false, false] ]
     */
    private static List<DrillVectorForCube> createDrillVectorsForCube(Report report, List<DimensionDTO> dimensionHeader, List<BaseVectorCoordinateForCube> baseVectorForCube, List<DrillVector> drillVectors) {
        List<DrillVectorForCube> result = new ArrayList<>();
        for (DrillVector drillVector : drillVectors) {
            result.add(CreateSingleDrillVectorForCube(report, dimensionHeader, baseVectorForCube, drillVector));
        }
        return result;
    }

    /**
     * Creates a drillVector personalized to the cube from a drill vector of the report.
     * The result is limited only the available dimensions in the cube, and a drill in a
     * dimension is requested only if there is a level below the base level in the dimension.
     *
     * @param report The Report under process
     * @param dimensionHeader Dimensions in the cube
     * @param baseVectorForCube Base vector in the cube to start the drill from
     * @param drillVector Original drill vector in the report, like ["SEX", "TIME"]
     * @return The personalized drill vector, like [false, false, true]
     */
    private static DrillVectorForCube CreateSingleDrillVectorForCube(Report report, List<DimensionDTO> dimensionHeader, List<BaseVectorCoordinateForCube> baseVectorForCube, DrillVector drillVector) {
        int dimNumber = dimensionHeader.size();
        DrillScenario[] result = new DrillScenario[dimNumber];
        for (int i = 0; i < dimNumber; i++) {
            DimensionDTO dim = dimensionHeader.get(i);
            result[i] = determineDrillInDimensionMode(report.getDimensionByName(dim.name()).getAllowedDepth(), drillVector.dimsToDrill(), dim, baseVectorForCube.get(i));
        }
//        System.out.println("CubeDrill: " + Arrays.stream(result).map(Enum::name).collect(Collectors.joining(",")));
        return new DrillVectorForCube(result);
    }

    /**
     * Determines the drill mode of a cube at a specific dimension, given the base level.
     *
     * @param depthAllowedInReport The maximal depth allowed by the report in the given dimension (0: root level, etc...)
     * @param dimsToDrill List of the requested drill dimension names, like [ "TIME", "SEX" ]
     * @param dimension The dimension of the cube the drill should be evaluated
     * @param baseVectorInDimension Base vector value in the dimension, like "2016,06"
     * @return The allowed DrillScenario, like DRILL, or NOT_REQUESTED, etc...
     */
    private static DrillScenario determineDrillInDimensionMode(int depthAllowedInReport, List<String> dimsToDrill, DimensionDTO dimension, BaseVectorCoordinateForCube baseVectorInDimension) {
        if (dimsToDrill.contains(dimension.name())) { // If drill is requested in the dimension
            int baseVectorDepth = depthCodedInPath(baseVectorInDimension.levelValuesString());
//            System.out.println("MÉLY: " + dimension.name() + ", base:" + baseVectorDepth + "(" + baseVectorInDimension.levelValuesString() + ")" + ", report:" + depthAllowedInReport + ", cube:" + dimension.maxDepth());
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
        return (int) path.chars().filter(ch -> ch == ',').count() + 1;
    }

}
