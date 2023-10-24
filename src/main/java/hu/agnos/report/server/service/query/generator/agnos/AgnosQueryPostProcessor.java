package hu.agnos.report.server.service.query.generator.agnos;

import java.util.List;

import hu.agnos.cube.driver.CubeHandler;
import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.driver.zolikaokos.ResultElement;
import hu.agnos.report.entity.Hierarchy;
import hu.agnos.report.entity.Indicator;
import hu.agnos.report.entity.Report;

/**
 *
 * @author parisek
 */
public class AgnosQueryPostProcessor {

    protected final String DRILL_VECTOR;
    protected final String BASE_VECTOR;
    private final Report report;
    private final CubeHandler cubeHandler;
    private ResultSet resultSet;

    public AgnosQueryPostProcessor(String DRILL_VECTOR, String BASE_VECTOR, Report report, CubeHandler cubeHandler) {
        this.DRILL_VECTOR = DRILL_VECTOR;
        this.BASE_VECTOR = BASE_VECTOR;
        this.report = report;
        this.cubeHandler = cubeHandler;
    }

    public void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public StringBuilder getResult() {
        StringBuilder result = new StringBuilder();

        String[] drillVectorArray = this.DRILL_VECTOR.split(":", -1);

        List<Hierarchy> hierarchies = report.getHierarchies();
        int hierarchyCnt = hierarchies.size();

        int[] hierarchyIdx = new int[hierarchyCnt];

        for (int i = 0; i < hierarchyCnt; i++) {

            // ha van lefúrás
            if (!drillVectorArray[i].equals("0")) {

//                System.out.println("van nem 0: "+this.cubeHandler.getHierarchyIndexByUniqueName(hierarchies.get(i).getHierarchyUniqueName()));
                hierarchyIdx[i] = this.cubeHandler.getHierarchyIndexByUniqueName(hierarchies.get(i).getName());
            } else {
                hierarchyIdx[i] = -1;
            }
        }

        int[] indicatorsIdx = new int[report.getIndicators().size() * 2];
        int idx = 0;
        for (Indicator indicator : report.getIndicators()) {

            indicatorsIdx[idx] = this.cubeHandler.getMeasureIndexByUniqueName(indicator.getValueName());
            idx++;

            indicatorsIdx[idx] = this.cubeHandler.getMeasureIndexByUniqueName(indicator.getDenominatorName());
            idx++;
        }

        result.append("{\"name\":\"").append(this.DRILL_VECTOR).append("\",");
        result.append("\"response\":{\"rows\":[");

        List<ResultElement> response = resultSet.getResponse();
        for (ResultElement r : response) {
            processOneRow(r, result, hierarchyIdx, indicatorsIdx);
        }

        result.replace(result.length() - 1, result.length(), "");
        result.append("]}}");
        return result;
    }

    private void processOneRow(ResultElement oneRow, StringBuilder result, int[] hierarchyIdx, int[] indicatorsIdx) {
        boolean isAppend = false;
        result.append("{\"dims\": [");

        for (int dimIdx : hierarchyIdx) {
            if (dimIdx >= 0) {
//                System.out.println("oneRow[0]["+dimIdx+"]: " + oneRow[0][dimIdx]);
                result.append(oneRow.getHeader()[dimIdx]).append(",");
                isAppend = true;
            }
        }
        if (isAppend) {
            result.replace(result.length() - 1, result.length(), "");
        }
        result.append("],\"vals\": [");

        for (int j = 0; j < indicatorsIdx.length; j++) {
            int measureIdx = indicatorsIdx[j];
            if (j % 2 == 0) {
                result.append("{\"sz\":").append(oneRow.getMeasureValues()[measureIdx]).append(",");
            } else {
                result.append("\"n\":").append(oneRow.getMeasureValues()[measureIdx]).append("},");
            }
        }
        result.replace(result.length() - 1, result.length(), "");
        result.append("]},");
    }

}
