package hu.agnos.report.server.util;

/**
 * TODO: delete class
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
