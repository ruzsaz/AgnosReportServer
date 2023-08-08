/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.driver.zolikaokos.ResultElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        List<ResultElement> elementList = rs.getResponse();
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
                
                re.getMeasureValues()[this.kaplanMeierMeasureIdx] = aux.get(i).getMeasureValues()[this.kaplanMeierMeasureIdx];
                result.add(re);
            }
        }
        return result;
    }

    private int[] getSortOrder(ResultElement oneRow) {
        int length = oneRow.getHeader().length;
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
