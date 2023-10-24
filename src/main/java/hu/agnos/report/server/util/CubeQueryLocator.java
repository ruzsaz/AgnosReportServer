package hu.agnos.report.server.util;

/**
 *
 * @author ruzsaz
 */
public class CubeQueryLocator {

    //éppen hova van lefurva, melyik dim melyik level-jén
    private final String[][] baseLevelString;
    //mit kell alábontani 
    //  ha 0 -> az adott dimenziót nem bontjuk, 
    //  ha 1 -> akkor 1 szintet lefúrunk
    private final int[] drillVector;

    /**
     *
     * @param base Pl. ":1:29,19"
     * @param drill Pl. "0:1:1"
     */
    public CubeQueryLocator(String base, String drill) {
        String[] baseSplitted = base.split(":");
        String[] drillSplitted = drill.split(":");
        int dimNumber = drillSplitted.length;
        this.baseLevelString = new String[dimNumber][];
        this.drillVector = new int[dimNumber];

        for (int i = 0; i < dimNumber; i++) {
            this.baseLevelString[i] = (baseSplitted.length <= i || baseSplitted[i].isEmpty()) ? new String[]{} : baseSplitted[i].split(",");
            this.drillVector[i] = Integer.parseInt(drillSplitted[i]);
        }
    }

    public CubeQueryLocator(String[][] baseLevelString, int[] drillVector) {
        this.baseLevelString = baseLevelString;
        this.drillVector = drillVector;
    }

    public String[][] getBaseLevelString() {
        return baseLevelString;
    }

    public int[] getDrillVector() {
        return drillVector;
    }
}
