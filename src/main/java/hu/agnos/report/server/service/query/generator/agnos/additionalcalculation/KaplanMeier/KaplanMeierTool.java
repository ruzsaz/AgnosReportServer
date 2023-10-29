package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hu.agnos.cube.meta.dto.ResultElement;
import hu.agnos.cube.meta.dto.ResultSet;


/**
 *
 * @author parisek
 */
public class KaplanMeierTool {

    private int kaplanMeierDimensioIdx;
    private int kaplanMeierMeasureIdx;

    public KaplanMeierTool(int kaplanMeierDimensioIdx, int kaplanMeierMeasureIdx) {
        this.kaplanMeierDimensioIdx = kaplanMeierDimensioIdx;
        this.kaplanMeierMeasureIdx = kaplanMeierMeasureIdx;
    }

    public KaplenMaierValue[] getSortedResult(ResultSet rs) {
        List<ResultElement> elementList = rs.response();
        KaplenMaierValue[] result = new KaplenMaierValue[elementList.size()];
        // ebbe a tömbe tároljuk, hogy rendezéskor milyen sorrendben menjen végig a hierarchiák listáján
        // a Kaplan-Meier hierarchia indexe lesz ezen tömb utolsó eleme
        int[] sortOrder = getSortOrder(elementList.get(0));
        int i = 0;
        for (ResultElement r : elementList) {
            result[i] = new KaplenMaierValue(r, sortOrder, this.kaplanMeierDimensioIdx);
            i++;
        }

        Arrays.sort(result, new KaplenMaierSortbyKnownId());
        return result;
    }

    public List<ResultElement> mergedResultElementLists(KaplenMaierValue[] origin, List<ResultElement> aux) {
        // ha a két lista nem egyenlő hosszú, akkor baj van

        List<ResultElement> result = null;
        int originLength = origin.length;
        if (originLength == aux.size()) {
            result = new ArrayList<>();
            
            for (int i = 0; i < originLength; i++) {
                //mindkét tömb rendezve van, így az elemek egyezőségét nem vizsgáljuk
                ResultElement re = origin[i].row;
                
                re.measureValues()[this.kaplanMeierMeasureIdx] = aux.get(i).measureValues()[this.kaplanMeierMeasureIdx];
                result.add(re);
            }
        }
        return result;
    }

    private int[] getSortOrder(ResultElement oneRow) {
        int length = oneRow.header().length;
        int[] sortOrder = new int[length];

        int nonKMDimIdx = 0;
        for (int i = 0; i < length - 1; i++) {
            if (this.kaplanMeierDimensioIdx == nonKMDimIdx) {
                nonKMDimIdx++;
            }
            sortOrder[i] = nonKMDimIdx;
            nonKMDimIdx++;
        }
        sortOrder[length - 1] = this.kaplanMeierDimensioIdx;

        return sortOrder;
    }

    public String getNewDrillVector(String drillVector) {
        String result = drillVector;
        StringBuilder newDrillVectorSB = new StringBuilder();
        String[] drillVectorArrray = drillVector.split(":", -1);
        if (!drillVectorArrray[this.kaplanMeierDimensioIdx].equals("1")) {
            for (int i = 0; i < drillVectorArrray.length; i++) {
                if (i == this.kaplanMeierDimensioIdx) {
                    newDrillVectorSB.append("1:");
                } else {
                    newDrillVectorSB.append(drillVectorArrray[i]).append(":");
                }
            }
            result = newDrillVectorSB.substring(0, newDrillVectorSB.length() - 1);
        }
        return result;
    }

}
