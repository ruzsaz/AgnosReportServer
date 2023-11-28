package hu.agnos.report.server.service.answerProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import hu.agnos.cube.meta.resultDto.NodeDTO;
import hu.agnos.cube.meta.resultDto.ResultElement;
import hu.agnos.cube.meta.resultDto.ResultSet;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.util.SetFunctions;

/**
 * The only public method determines the required coordinate values in an answer to the frontend for a single drill.
 */
public final class CoordinateExtractor {

    private CoordinateExtractor() {
    }

    /**
     * Determines the cartesian product of occurring dimension values in a drill, at least the part that should be
     * present in the answer.
     *
     * @param report The report the answer is made for
     * @param drillName List of the dimension names in the drill
     * @param matchingResultSets cubeName -> resultSet map containing the answer from different cubes for the
     *         drill
     * @return List of all possible dimension value combinations
     */
    public static List<List<NodeDTO>> getFullDimensionProductSet(Report report, List<String> drillName, Map<String, ResultSet> matchingResultSets) {
        List<Set<NodeDTO>> dimValuesInDrill = new ArrayList<>(drillName.size());
        for (String drillDimensionName : drillName) {
            Set<String> transparentCubeNames = report.getDimensionByName(drillDimensionName).getTransparentInCubes();
            dimValuesInDrill.add(CoordinateExtractor.ExtractDimensionValues(report, drillDimensionName, transparentCubeNames, matchingResultSets));
        }
        return SetFunctions.cartesianProduct(dimValuesInDrill);
    }

    /**
     * Extracts all occurring dimension values from a list of ResultSets (a ResultSet is an answer from the cubeServer
     * to a single cube+drill). There can be "transparent" cubes according to a dimension, whose dimension values are
     * omitted if not present in another cube.
     *
     * @param report The report the answer is made for
     * @param dimName Name string of the dimension to extract
     * @param transparentCubeNames Cube names whose dimension values should be omitted, if not present in
     *         another cube
     * @param resultSets cubeName -> resultSet map containing the answer from different cubes for the drill to
     *         look for dimension values
     * @return Set of the occurring dimension values
     */
    private static Set<NodeDTO> ExtractDimensionValues(Report report, String dimName, Set<String> transparentCubeNames, Map<String, ResultSet> resultSets) {
        Set<NodeDTO> dimensionValues = new HashSet<>(10);
        Stream<Map.Entry<String, ResultSet>> sorted = CoordinateExtractor.sortByCubeOrder(report, resultSets);
        sorted.forEach(rse -> {
            Set<NodeDTO> elementsToAdd = CoordinateExtractor.extractDimensionValuesFromSingleCube(dimName, transparentCubeNames, rse);
            if (elementsToAdd != null) {
                dimensionValues.addAll(elementsToAdd);
            }
        });
        return dimensionValues;
    }

    /**
     * Sorts the (cubeName) -> (resultSet) map by the cubes' order in the report.
     *
     * @param report The report the answer is made for
     * @param resultSets Map of resultSets to order
     * @return Stream of the map entries, in order of the cubes' occurrence in the report.
     */
    private static Stream<Map.Entry<String, ResultSet>> sortByCubeOrder(Report report, Map<String, ResultSet> resultSets) {
        Comparator<String> cubeOrderComparator = (o1, o2) -> {
            List<String> cubeNamesInOrder = report.getCubes().stream().map(Cube::getName).toList();
            return cubeNamesInOrder.indexOf(o1) - cubeNamesInOrder.indexOf(o2);
        };
        return resultSets.entrySet().stream().sorted(Map.Entry.comparingByKey(cubeOrderComparator));
    }

    /**
     * Gets the coordinate values of a single dimension from an answer from a single Cube.
     *
     * @param dimName Name of the dimension whose coordinate values are needed
     * @param transparentCubeNames The set of cube names whose coordinate values should be omitted from the
     *         result
     * @param resultSetsEntry The (cube name) -> (resultSet) map entry to process
     * @return The set of coordinate values
     */
    private static Set<NodeDTO> extractDimensionValuesFromSingleCube(String dimName, Set<String> transparentCubeNames, Map.Entry<String, ResultSet> resultSetsEntry) {
        ResultSet resultSet = resultSetsEntry.getValue();
        String resultSetCubeName = resultSet.cubeName();
        if (!transparentCubeNames.contains(resultSetCubeName)) {
            int indexInDrillVector = Arrays.asList(resultSet.dimensionHeader()).indexOf(dimName);
            if (indexInDrillVector >= 0 && resultSet.actualDrill()[indexInDrillVector].isShowResultAsDimValue()) {
                Set<NodeDTO> dimensionValues = new HashSet<>(10);
                for (ResultElement resultElement : resultSet.response()) {
                    dimensionValues.add(resultElement.header()[indexInDrillVector]);
                }
                return dimensionValues;
            }
        }
        return null;
    }

}
