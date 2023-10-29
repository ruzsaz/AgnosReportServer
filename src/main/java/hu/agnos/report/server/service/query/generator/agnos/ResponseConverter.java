package hu.agnos.report.server.service.query.generator.agnos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import hu.agnos.cube.meta.drillDto.DrillVector;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.cube.meta.dto.NodeDTO;
import hu.agnos.cube.meta.dto.ResultElement;
import hu.agnos.cube.meta.dto.ResultSet;
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
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResponseConverter {

    private Report report;
    private ReportQuery query;

    public AnswerForAllDrills getAnswer(List<ResultSet[]> resultSetsList) {
        List<AnswerForSingleDrill> answerList = new ArrayList<>();
        for (DrillVector dv : query.drillVectors()) {
            Map<String, ResultSet> matchingResultSets = new HashMap<>();
            for (ResultSet[] rss : resultSetsList) {
                for (ResultSet rs : rss) {
                    DrillVector originalDrillVector = rs.originalDrill();
                    // System.out.println(originalDrillVector.dimsToDrill() + " =?= " + dv.dimsToDrill());
                    // TODO: ehelyett equals?
                    if (SetFunctions.haveSameElements(originalDrillVector.dimsToDrill(), dv.dimsToDrill())) {
                        // System.out.println("Megvan!");
                        matchingResultSets.put(rs.cubeName(), rs);
                        //break;
                    }
                }
            }
            System.out.println("drill: " + dv.dimsToDrill());
            System.out.println("match: " + matchingResultSets);
            answerList.add(getAnswerForDrill(dv.dimsToDrill(), matchingResultSets));
            System.out.println("Drill: " + dv + " -hez találtam: " + answerList.size());
        }
        return new AnswerForAllDrills(answerList);
    }

    private AnswerForSingleDrill getAnswerForDrill(List<String> drillName, Map<String, ResultSet> matchingResultSets) {
        List<List<NodeDTO>> fullDimensionProductSet = getFullDimensionProductSet(drillName, matchingResultSets);
        List<DimsAndValues> dataRows = new ArrayList<>();
        for (List<NodeDTO> resultRowDimensionValues : fullDimensionProductSet) {
            dataRows.add(getMatchingResultValuesFromAllCubes(drillName, resultRowDimensionValues, matchingResultSets));
        }

        return new AnswerForSingleDrill(formatDrillNameForFrontend(drillName), new DataRowsInResponse(dataRows));
    }


    private String formatDrillNameForFrontend(List<String> drillName) {
        List<String> resultArray = new ArrayList<>();
        for (Dimension h : report.getDimensions()) {
            resultArray.add(drillName.contains(h.getName()) ? h.getName() : "0");
        }
        return String.join(":", resultArray);
    }

    private DimsAndValues getMatchingResultValuesFromAllCubes(List<String> drillName, List<NodeDTO> resultRowDimValues, Map<String, ResultSet> matchingResultSets) {

        // Get the matching dataRowByCubeName value rows for each cube.
        // TODO: extract to a separate method
        Map<String, double[]> dataRowByCubeName = new HashMap<>();
        //System.out.println("DRILL: " + drillName);
        for (Cube cube : report.getCubes()) {
            //System.out.println("Ezt keresem: " + cube.getName()+ ", ennyiből: " + matchingResultSets.size());
            ResultSet rs = matchingResultSets.get(cube.getName());
            NodeDTO[] matchPattern = getMatchPattern(drillName, resultRowDimValues, rs.dimensionHeader());
            dataRowByCubeName.put(cube.getName(), getMatchingResultValues(matchPattern, rs));
        }

        List<ValueElement> valueElementList = new ArrayList<>();

        // Crate the report measures
        // TODO: extract...
        // TODO: extracalculations???
        for (Indicator indicator : report.getIndicators()) {

            String valueCubeName = indicator.getValueCubeName();
            ResultSet valueResultSet = matchingResultSets.get(valueCubeName);
            //System.out.println(valueResultSet.measures() + " -ben hol van " + indicator.getValueName());
            int valueIndex = valueResultSet.measures().indexOf(indicator.getValueName());

            //System.out.println(valueResultSet.measures());

            //System.out.println(indicator.getValueName() + " ??? " + valueCubeName + ":" + valueIndex);

            double[] dataRow = dataRowByCubeName.get(valueCubeName);
            double sz = (dataRow != null) ? dataRow[valueIndex] : 0;

            String denominatorCubeName = indicator.getDenominatorCubeName();
            ResultSet denominatorResultSet = matchingResultSets.get(denominatorCubeName);
            int denominatorIndex = denominatorResultSet.measures().indexOf(indicator.getDenominatorName());
            double[] denominatorDataRow = dataRowByCubeName.get(denominatorCubeName);
            double n = (denominatorDataRow != null) ? denominatorDataRow[denominatorIndex] : 0;

            valueElementList.add(new ValueElement(sz, n));
        }

        return new DimsAndValues(resultRowDimValues, valueElementList);
    }

    private double[] getMatchingResultValues(NodeDTO[] matchPattern, ResultSet resultSet) {
        for (ResultElement element : resultSet.response()) {
            if (isMatches(matchPattern, element)) {
                return element.measureValues();
            }
        }
        return null;
    }

    /**
     * Determines if a pattern matches to the element's dimension values.
     * Nnull matches to anything, notNull matches with String.equals().
     *
     * @param matchPattern Like ([null], [{"id":"0","knownId":"01","name":"Baranya"}], [null])
     * @param element Single result element from a resultSet
     * @return True of matches, false if not
     */
    private boolean isMatches(NodeDTO[] matchPattern, ResultElement element) {
        for (int i = 0; i < matchPattern.length; i++) {
            if (matchPattern[i] != null && !Objects.equals(matchPattern[i], element.header()[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines a dim-matching-pattern to look for values in a resultSet.
     * @param dimNames Names of the drill dimensions (e.g. 'TERULETI','NEM')
     * @param dimValues Corresponding dim values (e.g. '{"id":"2","knownId":"03","name":"Békés"}', '{"id":"0","knownId":"1","name":"férfi"}')
     * @param dimensionHeader
     * @return The dim-matching-pattern (e.g. null, '{"id":"2","knownId":"03","name":"Békés"}', null)
     */
    private NodeDTO[] getMatchPattern(List<String> dimNames, List<NodeDTO> dimValues, String[] dimensionHeader) {
        NodeDTO[] pattern = new NodeDTO[dimensionHeader.length];
        for (int i = 0; i < dimNames.size(); i++) {
            int positionInDrillVector = Arrays.asList(dimensionHeader).indexOf(dimNames.get(i));
            if (positionInDrillVector >= 0) {
                pattern[positionInDrillVector] = dimValues.get(i);
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
    private List<List<NodeDTO>> getFullDimensionProductSet(List<String> drillName, Map<String, ResultSet> matchingResultSets) {
        List<Set<NodeDTO>> dimValuesinDrill = new ArrayList<>();
        for (String drillDimensionName : drillName) {
            dimValuesinDrill.add(ExtractDimensionValues(drillDimensionName, matchingResultSets));
        }
        System.out.println(SetFunctions.cartesianProduct(dimValuesinDrill));
        return SetFunctions.cartesianProduct(dimValuesinDrill);
    }

    /**
     * Extracts all occurring dimension values from a list of ResultSets
     * (a ResultSet is an answer from the cubeServer to a single cube+drill).
     *
     * @param dimName Name string of the dimension to extract
     * @param resultSets List of resultSet to look in for dimension values
     * @return Set of the occurring dimension values
     */
    private Set<NodeDTO> ExtractDimensionValues(String dimName, Map<String, ResultSet> resultSets) {
        System.out.println("Ehhez a drillhez keresek: " + dimName);
        Set<NodeDTO> dimensionValues = new HashSet<>();
        for (ResultSet resultSet : resultSets.values()) {
            int positionInDrillVector = Arrays.asList(resultSet.dimensionHeader()).indexOf(dimName);
            if (positionInDrillVector >= 0) {
                for (ResultElement resultElement : resultSet.response()) {
                    dimensionValues.add(resultElement.header()[positionInDrillVector]);
                }
            }
        }
        return dimensionValues;
    }

}
