/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hu.agnos.report.server.util;

/**
 *
 * @author parisek
 */
public class DrillVectorCompressor {
    
    private static final String SEPARATOR = ",";

    public static String compressDrillVectorsInOneString(String[] drillVectorsArray) {
        String result = "";
        
        StringBuilder drillVectorsBuinder = new StringBuilder();
        for (String oneVector : drillVectorsArray) {
            drillVectorsBuinder.append(oneVector).append(SEPARATOR);
        }
        
        if (!drillVectorsBuinder.isEmpty()) {

            result = (drillVectorsBuinder.substring(0, drillVectorsBuinder.length() - SEPARATOR.length()));
        }
        return result;
    }

}
