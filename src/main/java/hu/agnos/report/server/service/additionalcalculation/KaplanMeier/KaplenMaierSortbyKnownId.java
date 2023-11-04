/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.additionalcalculation.KaplanMeier;

import java.util.Comparator;

/**
 *
 * @author parisek
 */
public class KaplenMaierSortbyKnownId  implements Comparator<KaplenMaierValue> {

        @Override
        public int compare(KaplenMaierValue a, KaplenMaierValue b) {
            int result = 0;
            int length = a.sortOrder.length;

            for (int i = 0; i < length; i++) {
                if (i == length - 1) {
                    result = a.kaplenMaierDimensionKnownId.compareTo(b.kaplenMaierDimensionKnownId);
                } else {
                    int idx = a.sortOrder[i];
                    int subResult = a.row.header()[idx].knownId().compareTo(b.row.header()[idx].knownId());
                    if (subResult != 0) {
                        result = subResult;
                        break;
                    }
                }
            }
            return result;
        }
    }
