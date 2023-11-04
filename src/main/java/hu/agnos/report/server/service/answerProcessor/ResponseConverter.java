package hu.agnos.report.server.service.answerProcessor;

import hu.agnos.cube.meta.queryDto.DrillScenario;
import hu.agnos.cube.meta.queryDto.DrillVector;
import hu.agnos.cube.meta.queryDto.ReportQuery;
import hu.agnos.cube.meta.resultDto.NodeDTO;
import hu.agnos.cube.meta.resultDto.ResultElement;
import hu.agnos.cube.meta.resultDto.ResultSet;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Dimension;
import hu.agnos.report.entity.Indicator;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.resultDTO.*;
import hu.agnos.report.server.util.SetFunctions;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Tool to construct a response from the frontend from the individual responses from the different Cubes.
 */
@AllArgsConstructor
public class ResponseConverter {

    private Report report;
    private ReportQuery query;
    private Executor executor;

    /**
     * Selects the ResultSet matching to a drill from an array of ResultSets, came from an answer from a single Cube.
     *
     * @param drill The drill to find the answers for
     * @param resultSets Array of ResultSets to look for the answers in
     * @return The matching resultSet, as a cubeName -> ResultSet map, with exactly 1 element
     */
    private static Map<String, ResultSet> findTheMatchingResultSet(DrillVector drill, ResultSet[] resultSets) {
        Map<String, ResultSet> result = new HashMap<>(1);
        for (ResultSet resultSet : resultSets) {
            DrillVector originalDrill = resultSet.originalDrill();
            if (SetFunctions.isHaveSameElements(originalDrill.dimsToDrill(), drill.dimsToDrill())) {
                result.put(resultSet.cubeName(), resultSet);
                break;
            }
        }
        return result;
    }

    private static double[] getMatchingResultValues(NodeDTO[] matchPattern, ResultSet resultSet) {
        DrillScenario[] actualDrill = resultSet.actualDrill();
        for (ResultElement element : resultSet.response()) {
            if (isMatches(actualDrill, matchPattern, element)) {
                return element.measureValues();
            }
        }
        return null;
    }

    /**
     * Determines if a pattern matches to the element's dimension values. Null matches to anything, notNull matches with
     * String.equals().
     *
     * @param actualDrill The materialized drill strategy in each coordinate
     * @param matchPattern Like ([null], [{"id":"0","knownId":"01","name":"Baranya"}], [null])
     * @param element Single result element from a resultSet
     * @return True of matches, false if not
     */
    private static boolean isMatches(DrillScenario[] actualDrill, NodeDTO[] matchPattern, ResultElement element) {
        int patternLength = matchPattern.length;
        for (int i = 0; i < patternLength; i++) {
            if (actualDrill[i].isShowResultAsDimValue() && matchPattern[i] != null && !Objects.equals(matchPattern[i], element.header()[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines a dim-matching-pattern to look for values in a resultSet.
     *
     * @param drillDimNames Names of the drill dimensions (e.g. 'TERULETI','NEM')
     * @param dimValues Corresponding dim values (e.g. '{"id":"2","knownId":"03","name":"Békés"}',
     *         '{"id":"0","knownId":"1","name":"férfi"}')
     * @param dimensionHeader Array of dimension names in the cube
     * @return The dim-matching-pattern (e.g. null, '{"id":"2","knownId":"03","name":"Békés"}', null)
     */
    private static NodeDTO[] getMatchPattern(List<String> drillDimNames, List<NodeDTO> dimValues, String[] dimensionHeader) {
        NodeDTO[] pattern = new NodeDTO[dimensionHeader.length];
        int drillNamesSize = drillDimNames.size();
        for (int i = 0; i < drillNamesSize; i++) {
            int indexInDrillVector = Arrays.asList(dimensionHeader).indexOf(drillDimNames.get(i));
            if (indexInDrillVector >= 0) {
                pattern[indexInDrillVector] = dimValues.get(i);
            }
        }
        return pattern;
    }

    /**
     * Determines the full cartesian product of occurring dimension values in a drill.
     *
     * @param drillName List of the dimension names in the drill
     * @param matchingResultSets List of result sets (from different cubes)
     * @return The all possible dimension value combinations
     */
    private static List<List<NodeDTO>> getFullDimensionProductSet(List<String> drillName, Map<String, ResultSet> matchingResultSets) {
        List<Set<NodeDTO>> dimValuesInDrill = new ArrayList<>(drillName.size());
        for (String drillDimensionName : drillName) {
            dimValuesInDrill.add(ExtractDimensionValues(drillDimensionName, matchingResultSets));
        }
        return SetFunctions.cartesianProduct(dimValuesInDrill);
    }

    /**
     * Extracts all occurring dimension values from a list of ResultSets (a ResultSet is an answer from the cubeServer
     * to a single cube+drill).
     *
     * @param dimName Name string of the dimension to extract
     * @param resultSets List of resultSet to look in for dimension values
     * @return Set of the occurring dimension values
     */
    private static Set<NodeDTO> ExtractDimensionValues(String dimName, Map<String, ResultSet> resultSets) {
        Set<NodeDTO> dimensionValues = new HashSet<>(10);
        for (ResultSet resultSet : resultSets.values()) {
            int indexInDrillVector = Arrays.asList(resultSet.dimensionHeader()).indexOf(dimName);
            if (indexInDrillVector >= 0 && resultSet.actualDrill()[indexInDrillVector].isShowResultAsDimValue()) {
                for (ResultElement resultElement : resultSet.response()) {
                    dimensionValues.add(resultElement.header()[indexInDrillVector]);
                }
            }
        }
        return dimensionValues;
    }

    public AnswerForAllDrills getAnswer(List<ResultSet[]> resultSetsList) {
        List<AnswerForSingleDrill> answerList = new ArrayList<>(query.drillVectors().size());
        for (DrillVector drillVector : query.drillVectors()) {
            Map<String, ResultSet> matchingResultSets = getResulSetsForDrill(drillVector, resultSetsList);
            answerList.add(getAnswerForDrillAsync(drillVector.dimsToDrill(), matchingResultSets));
        }
        return new AnswerForAllDrills(answerList);
    }

    /**
     * Selects the matching results from a list of resultSet for a drill request. (A report query contains some
     * drill-requests, and all cubes answers for them. The cube's results are the elements in the list of resultSets,
     * each of them contains an answer for every drill requests)
     *
     * @param drill The drill to find the answers for
     * @param resultSetsList List of resultSets to look for the answers in
     * @return The matching resultSet, as a cubeName -> ResultSet map
     */
    private Map<String, ResultSet> getResulSetsForDrill(DrillVector drill, List<ResultSet[]> resultSetsList) {
        Map<String, ResultSet> matchingResultSets = new HashMap<>(report.getCubes().size());
        for (ResultSet[] resultSets : resultSetsList) {
            matchingResultSets.putAll(findTheMatchingResultSet(drill, resultSets));
        }
        return matchingResultSets;
    }

    private AnswerForSingleDrill getAnswerForDrillAsync(List<String> drillName, Map<String, ResultSet> matchingResultSets) {

        // Create the dimension value combinations that answers the drill
        List<List<NodeDTO>> dimensionProductSet = getFullDimensionProductSet(drillName, matchingResultSets);

        // Async fill the values for the dimension value combinations from the results, row by row.
        var futureMovies = dimensionProductSet.stream().map(coordinates -> CompletableFuture.supplyAsync(() ->
                getMatchingResultValuesFromAllCubes(drillName, coordinates, matchingResultSets), executor)).toList();
        List<DimsAndValues> dataRows = futureMovies.stream().map(CompletableFuture::join).toList();

        return new AnswerForSingleDrill(formatDrillNameForFrontend(drillName), new DataRowsInResponse(dataRows));
    }

    private String formatDrillNameForFrontend(List<String> drillName) {
        List<String> resultArray = new ArrayList<>(report.getDimensions().size());
        for (Dimension h : report.getDimensions()) {
            resultArray.add(drillName.contains(h.getName()) ? h.getName() : "0");
        }
        return String.join(":", resultArray);
    }

    private DimsAndValues getMatchingResultValuesFromAllCubes(List<String> drillName, List<NodeDTO> resultRowDimValues, Map<String, ResultSet> matchingResultSets) {

        // Get the matching dataRowByCubeName value rows for each cube.
        // TODO: extract to a separate method
        Map<String, double[]> dataRowByCubeName = new HashMap<>(report.getCubes().size());
        for (Cube cube : report.getCubes()) {
            ResultSet rs = matchingResultSets.get(cube.getName());
            NodeDTO[] matchPattern = getMatchPattern(drillName, resultRowDimValues, rs.dimensionHeader());
            dataRowByCubeName.put(cube.getName(), getMatchingResultValues(matchPattern, rs));
        }

        List<ValueElement> valueElementList = new ArrayList<>(report.getIndicators().size());

        // Crate the report measures
        // TODO: extract...
        // TODO: extracalculations???
        for (Indicator indicator : report.getIndicators()) {

            // Value
            String valueCubeName = indicator.getValueCubeName();
            ResultSet valueResultSet = matchingResultSets.get(valueCubeName);
            int valueIndex = valueResultSet.measures().indexOf(indicator.getValueName());
            double[] dataRow = dataRowByCubeName.get(valueCubeName);
            double value = (dataRow != null) ? dataRow[valueIndex] : 0.0;

            // Denominator
            String denominatorCubeName = indicator.getDenominatorCubeName();
            ResultSet denominatorResultSet = matchingResultSets.get(denominatorCubeName);
            int denominatorIndex = denominatorResultSet.measures().indexOf(indicator.getDenominatorName());
            double[] denominatorDataRow = dataRowByCubeName.get(denominatorCubeName);
            double denominator = (denominatorDataRow != null) ? denominatorDataRow[denominatorIndex] : 0.0;

            valueElementList.add(new ValueElement(value, denominator));
        }

        return new DimsAndValues(resultRowDimValues, valueElementList);
    }

}
