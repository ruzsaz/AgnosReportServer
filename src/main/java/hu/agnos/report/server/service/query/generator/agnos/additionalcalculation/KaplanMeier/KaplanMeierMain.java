package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.AbstractAdditionalCalculation;
import hu.agnos.report.server.util.CubeServerClient;

/**
 *
 * @author parisek
 */
public class KaplanMeierMain extends AbstractAdditionalCalculation {

    private int kaplanMeierDimensioIdx;
    private int kaplanMeierMeasureIdx;
    private String kaplanMeierDimensioOriginBaseVectorValue;
    private int kaplanMeierDimensioLastLevelId;

    public KaplanMeierMain(String args, String cubeName, String[] hierarchyHeader, String[] measureHeader, String cubeServerUri) {
        super(args, cubeName, hierarchyHeader, measureHeader, cubeServerUri);
        //ez az attribútum tényleges értékét a getNewBaseVectorForBaseVectorProcessor eljárásban kapja
        this.kaplanMeierDimensioOriginBaseVectorValue = null;
        kaplanMeierDimensioLastLevelId = -1;

        //ha megfelelő paraméterekkel lett meghívva
        if (this.args.split(",").length == 2) {
            boolean isFound = false;
            String hierName = this.args.split(",")[0].trim().toUpperCase();

            for (int i = 0; i < hierarchyHeader.length; i++) {
                if (hierarchyHeader[i].toUpperCase().equals(hierName)) {
                    isFound = true;
                    this.kaplanMeierDimensioIdx = i;
                    break;
                }
            }
            if (isFound) {
                String measureName = this.args.split(",")[1].trim().toUpperCase();
                for (int i = 0; i < measureHeader.length; i++) {
                    if (measureHeader[i].toUpperCase().equals(measureName)) {
                        this.kaplanMeierMeasureIdx = i;
                        this.correctArgs = true;
                        break;
                    }
                }
            }
        }
    }

    /**
     *
     * @param baseVector
     * @param drillVectorsArray
     * @return
     */
    @Override
    public ResultSet[] process(String baseVector, String[] drillVectorsArray) {
        String newBaseVector = getNewBaseVector(baseVector);

//        long startTime = System.nanoTime();    
        //ha nincs fúrva a Kaplan-Meire dimenzió mentén
        if (newBaseVector.equals(baseVector)) {
            KaplanMeierConstBaseVector kmcb = new KaplanMeierConstBaseVector(kaplanMeierDimensioIdx, kaplanMeierMeasureIdx, 
                    baseVector, drillVectorsArray, cubeName);
            ResultSet[] result = kmcb.process(cubeServerUri);
//            long estimatedTime = System.nanoTime() - startTime;
//            System.out.println("A konstans teljes eddig tartott: " + estimatedTime);

            return result;
            //ha van fúrva a Kaplan-Meire dimenzió mentén
        } else {
            KaplanMeierVariableBaseVector kmvbv = new KaplanMeierVariableBaseVector(kaplanMeierDimensioIdx, kaplanMeierMeasureIdx, baseVector, 
                    drillVectorsArray, this.cubeName, kaplanMeierDimensioLastLevelId, newBaseVector);
            ResultSet[] result = kmvbv.process(cubeServerUri);
//            long estimatedTime = System.nanoTime() - startTime;
//            System.out.println("A válltozó teljes eddig tartott: " + estimatedTime);

            return result;
        }

    }

    /**
     *
     * @param originBaseVector
     * @return
     */
    private String getNewBaseVector(String originBaseVector) {
        // az eredmény kezdetben azért egyenlő a kapott baseVector-ral, mert ha
        // a KaplanMeier dimenzióban toplevel szinten vagyunk, akkor változatlanul hadjuk a basvector-t
        String result = originBaseVector;

        String[] baseVectorArray = originBaseVector.split(":", -1);

        String kaplanMeierDimension = baseVectorArray[this.kaplanMeierDimensioIdx];
        this.kaplanMeierDimensioOriginBaseVectorValue = kaplanMeierDimension;

        if (!kaplanMeierDimension.isEmpty()) {

            String[] kaplanMeierDimensionArray = kaplanMeierDimension.split(",", -1);

            if (kaplanMeierDimensionArray.length < 2) {
                kaplanMeierDimension = "";
                kaplanMeierDimensioLastLevelId = Integer.parseInt(kaplanMeierDimensionArray[0]);
            } else {

                StringBuilder newkaplanMeierDimensionSB = new StringBuilder();
                //az utolsó elemet levágjuk, mivel egy szintel feljebb akarunk vizsgálni
                int length = kaplanMeierDimensionArray.length - 1;
                for (int i = 0; i < length; i++) {
                    newkaplanMeierDimensionSB.append(kaplanMeierDimensionArray[i]).append(",");
                }

                kaplanMeierDimensioLastLevelId = Integer.parseInt(kaplanMeierDimensionArray[length]);

                kaplanMeierDimension = newkaplanMeierDimensionSB.substring(0, newkaplanMeierDimensionSB.length() - 1);
            }

            int length = baseVectorArray.length;
            StringBuilder newBaseVectorSB = new StringBuilder();

            for (int i = 0; i < length; i++) {
                if (i == this.kaplanMeierDimensioIdx) {
                    newBaseVectorSB.append(kaplanMeierDimension).append(":");
                } else {
                    newBaseVectorSB.append(baseVectorArray[i]).append(":");
                }
            }
            result = newBaseVectorSB.substring(0, newBaseVectorSB.length() - 1);
        }
        return result;
    }

}
