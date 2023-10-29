package hu.agnos.report.server.util;

import static hu.agnos.report.server.util.SetFunctions.limitList;

import java.util.ArrayList;
import java.util.List;

import hu.agnos.cube.meta.drillDto.BaseVectorCoordinate;
import hu.agnos.cube.meta.drillDto.BaseVectorCoordinateForCube;
import hu.agnos.cube.meta.drillDto.CubeQuery;
import hu.agnos.cube.meta.drillDto.DrillVector;
import hu.agnos.cube.meta.drillDto.DrillVectorForCube;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.cube.meta.dto.CubeMetaDTO;
import hu.agnos.cube.meta.dto.DimensionDTO;

/**
 * @author parisek
 */

public class CubeQueryCreator {

    public static CubeQuery createCubeQuery(String cubeName, CubeMetaDTO cubeMeta, ReportQuery query) {
        List<BaseVectorCoordinateForCube> baseVectorForCube = createBaseVectorForCube(cubeMeta.dimensionHeader(), query.baseVector());
        List<DrillVectorForCube> drillVectorsForCube = createDrillVectorsForCube(cubeMeta.dimensionHeader(), baseVectorForCube, query.drillVectors());
        return new CubeQuery(cubeName, query.drillVectors(), baseVectorForCube, drillVectorsForCube);
    }

    /**
     * Creates a String that represents a baseVector, like "4:1,17::"
     * where order of the hierarchies corresponds to the order of hierarchies
     * in the cube, and the numbers are the element ids in the hierarchy level
     * where the base of the data is.
     *
     * @param dimensionHeader The dimensions in the order inside the cube
     * @param baseVector       Base vector in object form, with the name of the hierarchy,
     *                         and the base levels in an ordered array
     * @return The string representation of the base vector, like "4:1,17::"
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

    private static List<String> limitDepth(List<String> list, int limit) {
        return limitList(list, limit - 1);
    }

    /**
     * Creates a String that represents some drillVectors, like "0:0:1,1:1:0,0:0:0"
     * where order of the 0-1s corresponds to the order of hierarchies
     * in the cube, and the separate drillVectors are separated by commas.
     *
     * @param baseVectorForCube Names of the hierarchies in the order inside the cube
     * @param drillVectors     Array of drillVectors, which is an array of hierachy names
     *                         meaning the hierarchy is part of the drill.
     * @return The string representation of the drills, like 0:0:1,1:1:0,0:0:0"
     */
    private static List<DrillVectorForCube> createDrillVectorsForCube(List<DimensionDTO> dimensionHeader, List<BaseVectorCoordinateForCube> baseVectorForCube, List<DrillVector> drillVectors) {
        List<DrillVectorForCube> result = new ArrayList<>();
        for (DrillVector drillVector : drillVectors) {
            result.add(CreateSingleDrillVectorForCube(dimensionHeader, baseVectorForCube, drillVector));
        }
        return result;
    }

    private static DrillVectorForCube CreateSingleDrillVectorForCube(List<DimensionDTO> dimensionHeader, List<BaseVectorCoordinateForCube> baseVectorForCube, DrillVector drillVector) {
        int dimNumber = dimensionHeader.size();
        boolean[] result = new boolean[dimNumber];
        for (int i = 0; i < dimNumber; i++) {
            DimensionDTO dim = dimensionHeader.get(i);
            int maxDepth = dim.maxDepth();
            int baseVectorDepth = depthCodedInPath(baseVectorForCube.get(i).levelValuesString());
            result[i] = drillVector.dimsToDrill().contains(dim.name()) && maxDepth > baseVectorDepth;
        }
        return new DrillVectorForCube(result);
    }

    private static int depthCodedInPath(String path) {
        if (path.isEmpty()) {
            return 1;
        }
        return (int) path.chars().filter(ch -> ch == ',').count() + 2;
    }

}
