/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.agnos;

/**
 *
 * @author parisek
 */
public class AgnosQueryPreProcessor {
    
     protected String besaVectorConverter(int[] temp, String baseVector) {
         
        String[] baseVectorArray = baseVector.split(":", -1);
        StringBuilder newBaseVectorSB = new StringBuilder();
         for (int newIdx : temp) {
             if (newIdx >= 0) {
                 newBaseVectorSB.append(baseVectorArray[newIdx]).append(":");
             } else {
                 newBaseVectorSB.append(":");
             }
         }
        return newBaseVectorSB.substring(0, newBaseVectorSB.length() - 1);
    }



    protected String drillVectorConverter(int[] temp, String drillVector) {
        String[] drillVectorArray = drillVector.split(":", -1);
        StringBuilder newDrillVectorSB = new StringBuilder();
        for (int newIdx : temp) {
            if (newIdx >= 0) {
                newDrillVectorSB.append(drillVectorArray[newIdx]).append(":");
            } else {
                newDrillVectorSB.append("0:");
            }
        }
        return newDrillVectorSB.substring(0, newDrillVectorSB.length() - 1);
    }
    
}
