package hu.agnos.report.server.service.answerProcessor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import hu.agnos.cube.meta.queryDto.DrillScenario;
import hu.agnos.cube.meta.resultDto.ResultElement;
import hu.agnos.cube.meta.resultDto.ResultSet;

public class KaplanMeierPreProcessor {


    public static void process(ResultSet resultSet, List<Integer> extraCalculatedIndices) {

        int num = extraCalculatedIndices.size();
        DrillScenario[] drillScenarios = resultSet.actualDrill();
        int dimIndex = 0;
        for (int i = 0; i < drillScenarios.length; i++) {
            if (drillScenarios[i].isShowExtraCalculationInstead()) {
                dimIndex = i;
            }
        }

        if (resultSet.actualDrill()[dimIndex].isShowResultAsDimValue()) {
            List<ResultElement> responseList = resultSet.response();
            KaplenMaierSortbyKnownId sorter = new KaplenMaierSortbyKnownId(dimIndex);
            responseList.sort(sorter);


            ResultElement previous = null;
            //double[] accumulator = new double[num];
            for (ResultElement re : responseList) {
                if (!isNew(previous, re, sorter)) {
                    //Arrays.fill(accumulator, 1.0);
                    //}
                    for (int i = 0; i < num; i++) {
                        assert previous != null;
                        re.measureValues()[extraCalculatedIndices.get(i)] = previous.measureValues()[extraCalculatedIndices.get(i)] * re.measureValues()[extraCalculatedIndices.get(i)];
                    }
                }
                previous = re;
            }
        } else {
            List<ResultElement> responseList = resultSet.response();
            KaplenMaierSortbyKnownId sorter = new KaplenMaierSortbyKnownId(dimIndex);
            responseList.sort(sorter);


            ResultElement previous = null;
            double[] accumulator = new double[num];
            for (ResultElement re : responseList) {
                if (isNew(previous, re, sorter)) {
                    Arrays.fill(accumulator, 1.0);
                } else {
                    assert previous != null;
                    previous.header()[dimIndex] = null;
                }
                for (int i = 0; i < num; i++) {
                    accumulator[i] = accumulator[i] * re.measureValues()[extraCalculatedIndices.get(i)];
                    re.measureValues()[extraCalculatedIndices.get(i)] = accumulator[i];
                }
                previous = re;
            }

            Iterator<ResultElement> ri  = responseList.iterator();
            while(ri.hasNext()) {
                ResultElement r = ri.next();
                if (r.header()[dimIndex] == null) {
                    ri.remove();
                }
            }


        }
    }

    private static boolean isNew(ResultElement prev, ResultElement curr, KaplenMaierSortbyKnownId sorter) {
        return !sorter.isSameWithoutIndex(prev, curr);
    }

}
