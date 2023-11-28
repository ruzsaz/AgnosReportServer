package hu.agnos.report.server.service.answerProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
import hu.agnos.report.server.resultDTO.AnswerForAllDrills;
import hu.agnos.report.server.resultDTO.AnswerForSingleDrill;
import hu.agnos.report.server.resultDTO.DataRowsInResponse;
import hu.agnos.report.server.resultDTO.DimsAndValues;
import hu.agnos.report.server.resultDTO.ValueElement;
import hu.agnos.report.server.util.SetFunctions;

import static hu.agnos.report.server.service.answerProcessor.CoordinateExtractor.getFullDimensionProductSet;

/**
 * Tool to construct a response from the frontend from the individual responses from the different Cubes.
 */
public class ResponseConverter {

    private final Report report;
    private final ReportQuery query;
    private final Executor executor;

    /**
     * Manages the conversion of the responses from the cubes to a response to the request from the frontend.
     *
     * @param report The report the frontend is showing
     * @param query Original query from the frontend
     * @param executor Executor for multithreaded process
     */
    public ResponseConverter(Report report, ReportQuery query, Executor executor) {
        this.report = report;
        this.query = query;
        this.executor = executor;
    }

    /**
     * Converts the responses from the cubes to a response for the report query.
     *
     * @param resultSetsList List of resultSets, sent by the cubes
     * @return Response to send to the frontend
     */
    public AnswerForAllDrills getAnswer(List<ResultSet> resultSetsList) {
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
     * @param resultSetList List of resultSets to look for the answers in
     * @return The matching resultSet, as a cubeName -> ResultSet map
     */
    private Map<String, ResultSet> getResulSetsForDrill(DrillVector drill, List<ResultSet> resultSetList) {
        Map<String, ResultSet> matchingResultSets = new HashMap<>(report.getCubes().size());
        for (ResultSet resultSet : resultSetList) {
            DrillVector originalDrill = resultSet.originalDrill();
            if (SetFunctions.isHaveSameElements(originalDrill.dimsToDrill(), drill.dimsToDrill())) {
                matchingResultSets.put(resultSet.cubeName(), resultSet);
            }
        }
        return matchingResultSets;
    }

    private AnswerForSingleDrill getAnswerForDrillAsync(List<String> drillName, Map<String, ResultSet> matchingResultSets) {

        // Create the dimension value combinations that answers the drill
        List<List<NodeDTO>> dimensionProductSet = getFullDimensionProductSet(report, drillName, matchingResultSets);

        // Async fill the values for the dimension value combinations from the results, row by row.
        var futureValuesList = dimensionProductSet.stream().map(coordinates -> CompletableFuture.supplyAsync(() ->
                getMatchingResultValuesFromAllCubes(drillName, coordinates, matchingResultSets), executor)).toList();
        List<DimsAndValues> dataRows = futureValuesList.stream().map(CompletableFuture::join).toList();

        return new AnswerForSingleDrill(formatDrillNameForFrontend(drillName), new DataRowsInResponse(dataRows));
    }

    private DimsAndValues getMatchingResultValuesFromAllCubes(List<String> drillName, List<NodeDTO> resultRowDimValues, Map<String, ResultSet> matchingResultSets) {
        Map<String, double[]> dataRowByCubeName = getDataRowMap(drillName, resultRowDimValues, matchingResultSets);
        List<ValueElement> valueElementList = new ArrayList<>(report.getIndicators().size());
        for (Indicator indicator : report.getIndicators()) {
            double value = ResponseConverter.getMeasureValue(dataRowByCubeName, matchingResultSets, indicator.getValueCubeName(), indicator.getValueName());
            double denominator = ResponseConverter.getMeasureValue(dataRowByCubeName, matchingResultSets, indicator.getDenominatorCubeName(), indicator.getDenominatorName());
            valueElementList.add(new ValueElement(value, denominator));
        }
        return new DimsAndValues(resultRowDimValues, valueElementList);
    }

    private String formatDrillNameForFrontend(List<String> drillName) {
        List<String> resultArray = new ArrayList<>(report.getDimensions().size());
        for (Dimension h : report.getDimensions()) {
            resultArray.add(drillName.contains(h.getName()) ? h.getName() : "0");
        }
        return String.join(":", resultArray);
    }

    /**
     * Gets the matching data row from each Cube for a given drill. The result will be presented as a cubeName ->
     * dataRow map.
     *
     * @param drillNames Names of the drill dimensions (e.g. 'TERRITORIAL','SEX')
     * @param resultRowDimValues The coordinate values in the base&drill level
     * @param matchingResultSets The cubeName -> resultSet map
     * @return The cubeName -> dataRow map
     */
    private Map<String, double[]> getDataRowMap(List<String> drillNames, List<NodeDTO> resultRowDimValues, Map<String, ResultSet> matchingResultSets) {
        Map<String, double[]> dataRowByCubeName = new HashMap<>(report.getCubes().size());
        for (Cube cube : report.getCubes()) {
            ResultSet rs = matchingResultSets.get(cube.getName());
            NodeDTO[] matchPattern = ResponseConverter.getMatchPattern(drillNames, resultRowDimValues, rs.dimensionHeader());
            dataRowByCubeName.put(cube.getName(), ResponseConverter.getMatchingResultValues(matchPattern, rs));
        }
        return dataRowByCubeName;
    }

    /**
     * Gets a single measure's value from a result from a cube.
     *
     * @param dataRowByCubeName A CubeName -> resultDataRow map, according the selected base&drill
     * @param matchingResultSets A CubeName -> resultSet map, according the selected base&drill
     * @param cubeName The cube's name to get the measure from
     * @param measureName Measure's name inside the cube
     * @return The measure's value at the given base@drill from the cube
     */
    private static double getMeasureValue(Map<String, double[]> dataRowByCubeName, Map<String, ResultSet> matchingResultSets, String cubeName, String measureName) {
        if (cubeName.isEmpty()) {
            return 1.0;
        }
        ResultSet valueResultSet = matchingResultSets.get(cubeName);
        int measureIndex = valueResultSet.measures().indexOf(measureName);
        double[] dataRow = dataRowByCubeName.get(cubeName);
        return (dataRow != null) ? dataRow[measureIndex] : 0.0;
    }

    /**
     * Determines a dim-matching-pattern to look for values in a resultSet.
     *
     * @param drillDimNames Names of the drill dimensions (e.g. 'TERRITORIAL','SEX')
     * @param dimValues Corresponding dim values (e.g. '{"id":"02","name":"Pest"}',
     *         '{"id":"0","knownId":"1","name":"male"}')
     * @param dimensionHeader Array of dimension names in the cube
     * @return The dim-matching-pattern (e.g. null, '{"id":"02","name":"Pest"}', null)
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

    private static double[] getMatchingResultValues(NodeDTO[] matchPattern, ResultSet resultSet) {
        DrillScenario[] actualDrill = resultSet.actualDrill();
        for (ResultElement element : resultSet.response()) {
            if (ResponseConverter.isMatches(actualDrill, matchPattern, element)) {
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
     * @param matchPattern Like ([null], [{"id":"02","name":"Pest"}], [null])
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

}
