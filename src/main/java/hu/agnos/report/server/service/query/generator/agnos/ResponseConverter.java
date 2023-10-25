package hu.agnos.report.server.service.query.generator.agnos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.driver.zolikaokos.ResultElement;
import hu.agnos.cube.meta.drillDto.DrillVector;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.molap.dimension.DimValue;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Hierarchy;
import hu.agnos.report.entity.Indicator;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.resultDTO.AnswerForAllDrills;
import hu.agnos.report.server.resultDTO.AnswerForSingleDrill;
import hu.agnos.report.server.resultDTO.DataRowsInResponse;
import hu.agnos.report.server.resultDTO.DimElement;
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
                    if (rs.getOriginalName().equals(dv.dimsToDrill())) {
                        matchingResultSets.put(rs.getCubeName(), rs);
                        break;
                    }
                }
            }
            answerList.add(getAnswerForDrill(dv.dimsToDrill(), matchingResultSets));
        }
        return new AnswerForAllDrills(answerList);
    }

    private AnswerForSingleDrill getAnswerForDrill(List<String> drillName, Map<String, ResultSet> matchingResultSets) {
        List<List<DimValue>> fullDimensionProductSet = getFullDimensionProductSet(drillName, matchingResultSets);
        List<DimsAndValues> dataRows = new ArrayList<>();
        for (List<DimValue> resultRowDimensionValues : fullDimensionProductSet) {
            dataRows.add(getMatchingResultValuesFromAllCubes(drillName, resultRowDimensionValues, matchingResultSets));
        }

        return new AnswerForSingleDrill(formatDrillNameForFrontend(drillName), new DataRowsInResponse(dataRows));
    }


    private String formatDrillNameForFrontend(List<String> drillName) {
        List<String> resultArray = new ArrayList<>();
        for (Hierarchy h : report.getHierarchies()) {
            resultArray.add(drillName.contains(h.getName()) ? h.getName() : "0");
        }
        return String.join(":", resultArray);
    }

    private DimsAndValues getMatchingResultValuesFromAllCubes(List<String> drillName, List<DimValue> resultRowDimValues, Map<String, ResultSet> matchingResultSets) {

        // Get the matching dataRowByCubeName value rows for each cube.
        // TODO: extract to a separate method
        Map<String, double[]> dataRowByCubeName = new HashMap<>();
        for (Cube cube : report.getCubes()) {
            ResultSet rs = matchingResultSets.get(cube.getName());
            DimValue[] matchPattern = getMatchPattern(drillName, resultRowDimValues, rs.getName());
            dataRowByCubeName.put(cube.getName(), getMatchingResultValues(matchPattern, rs));
        }

        List<ValueElement> valueElementList = new ArrayList<>();

        // Crate the report measures
        // TODO: extract...
        // TODO: extracalculations???
        for (Indicator indicator : report.getIndicators()) {

            String valueCubeName = indicator.getValueCubeName();
            ResultSet valueResultSet = matchingResultSets.get(valueCubeName);
            int valueIndex = valueResultSet.getMeasures().indexOf(indicator.getValueName());
            double[] dataRow = dataRowByCubeName.get(valueCubeName);
            double sz = (dataRow != null) ? dataRow[valueIndex] : 0;

            String denominatorCubeName = indicator.getDenominatorCubeName();
            ResultSet denominatorResultSet = matchingResultSets.get(denominatorCubeName);
            int denominatorIndex = denominatorResultSet.getMeasures().indexOf(indicator.getDenominatorName());
            double[] denominatorDataRow = dataRowByCubeName.get(denominatorCubeName);
            double n = (denominatorDataRow != null) ? denominatorDataRow[denominatorIndex] : 0;

            valueElementList.add(new ValueElement(sz, n));
        }

        List<DimElement> dimElements = resultRowDimValues.stream().map(DimElement::from).toList();

        return new DimsAndValues(dimElements, valueElementList);
    }

    private double[] getMatchingResultValues(DimValue[] matchPattern, ResultSet resultSet) {
        for (ResultElement element : resultSet.getResponse()) {
            if (isMatches(matchPattern, element)) {
                return element.getMeasureValues();
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
    private boolean isMatches(DimValue[] matchPattern, ResultElement element) {
        for (int i = 0; i < matchPattern.length; i++) {
            if (matchPattern[i] != null && !Objects.equals(matchPattern[i], element.getHeader()[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines a dim-matching-pattern to look for values in a resultSet.
     * @param dimNames Names of the drill dimensions (e.g. 'TERULETI','NEM')
     * @param dimValues Corresponding dim values (e.g. '{"id":"2","knownId":"03","name":"Békés"}', '{"id":"0","knownId":"1","name":"férfi"}')
     * @param cubeDrillName Drill string of the cube (e.g. '0:TERULETI_HIER:0')
     * @return The dim-matching-pattern (e.g. null, '{"id":"2","knownId":"03","name":"Békés"}', null)
     */
    private DimValue[] getMatchPattern(List<String> dimNames, List<DimValue> dimValues, String cubeDrillName) {
        String[] dimValuesAsArray = cubeDrillName.split(":");
        DimValue[] pattern = new DimValue[dimValuesAsArray.length];
        for (int i = 0; i < dimNames.size(); i++) {
            int positionInDrillVector = Arrays.asList(dimValuesAsArray).indexOf(dimNames.get(i));
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
    private List<List<DimValue>> getFullDimensionProductSet(List<String> drillName, Map<String, ResultSet> matchingResultSets) {
        List<Set<DimValue>> dimValuesinDrill = new ArrayList<>();
        for (String drillDimensionName : drillName) {
            dimValuesinDrill.add(ExtractDimensionValues(drillDimensionName, matchingResultSets));
        }
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
    private Set<DimValue> ExtractDimensionValues(String dimName, Map<String, ResultSet> resultSets) {
        Set<DimValue> dimensionValues = new HashSet<>();
        for (ResultSet resultSet : resultSets.values()) {
            int positionInDrillVector = Arrays.asList(resultSet.getName().split(":")).indexOf(dimName);
            if (positionInDrillVector >= 0) {
                for (ResultElement resultElement : resultSet.getResponse()) {
                    dimensionValues.add(resultElement.getHeader()[positionInDrillVector]);
                }
            }
        }
        return dimensionValues;
    }

}
