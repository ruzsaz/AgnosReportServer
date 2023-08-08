 
package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.cube.driver.zolikaokos.ResultElement;
import hu.agnos.cube.meta.http.CubeClient;
import hu.agnos.report.server.util.CubeServerClient;
import hu.agnos.report.server.util.DrillVectorCompressor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author parisek
 */


public class KaplanMeierVariableBaseVector {

    private int kaplanMeierDimensioIdx;
    private int kaplanMeierMeasureIdx;
    private String baseVector;
    private String[] originDrillVectors;
    private String cubeName;
    private int kaplanMeierDimensioLastLevelId;
    private String auxBaseVector;
    private KaplanMeierTool tool;
    private CubeServerClient cubeServerClient;


    public KaplanMeierVariableBaseVector(int kaplanMeierDimensioIdx, int kaplanMeierMeasureIdx, String baseVector, String[] originDrillVectors,
            String cubeName, int kaplanMeierDimensioLastLevelId, String auxBaseVector, CubeServerClient cubeServerClient) {
        this.kaplanMeierDimensioIdx = kaplanMeierDimensioIdx;
        this.kaplanMeierMeasureIdx = kaplanMeierMeasureIdx;
        this.baseVector = baseVector;
        this.originDrillVectors = originDrillVectors;
        this.cubeName = cubeName;
        this.kaplanMeierDimensioLastLevelId = kaplanMeierDimensioLastLevelId;
        this.auxBaseVector = auxBaseVector;
        this.tool = new KaplanMeierTool(kaplanMeierDimensioIdx, kaplanMeierMeasureIdx);
        this.cubeServerClient = cubeServerClient;
    }

    public ResultSet[] process() {

        ResultSet[] result = new ResultSet[originDrillVectors.length];

        //Ha egy drillVector nem bontja meg a KM dimenziót, akkor szükség van egy plusz drillVectorra, amely megbontja. 
        //Ebbe a map-be kulcs az ilyen drillVector, míg érték a új drillVector
        HashMap<String, String> drillVectorMapper = new HashMap<>();

        //ez az elöző lépésben létrehozott új drillVector-ok tömbje
        //erre azért van szükség, mert ezen drillvektorok eredményhalmazát nem kell önmagában feldolgozni
        Set<String> auxiliaryDrillVectors = new HashSet<>();

        for (String drillVector : originDrillVectors) {
            String auxDrillVector = tool.getNewDrillVector(drillVector);
            drillVectorMapper.put(drillVector, auxDrillVector);
            auxiliaryDrillVectors.add(auxDrillVector);
        }

        String[] auxDrillVectorsArray = auxiliaryDrillVectors.toArray(new String[auxiliaryDrillVectors.size() ]);


        String drillVectorsComrressOneString = DrillVectorCompressor.compressDrillVectorsInOneString(originDrillVectors);

        ResultSet[] originResultSets = null;
        
        
        Optional<ResultSet[]> optionalResultSet = cubeServerClient.getCubeData( cubeName, baseVector, drillVectorsComrressOneString);
        if (optionalResultSet.isPresent()) {
            originResultSets = optionalResultSet.get();
        }

        drillVectorsComrressOneString = DrillVectorCompressor.compressDrillVectorsInOneString(auxDrillVectorsArray);
        
        ResultSet[] auxResultSets = null;
        optionalResultSet = cubeServerClient.getCubeData( cubeName, baseVector, drillVectorsComrressOneString);
        if (optionalResultSet.isPresent()) {
            auxResultSets = optionalResultSet.get();
        }

        HashMap<String, KaplenMaierValue[]> sortedOriginRawResults = new HashMap<>();

        for (ResultSet rs : originResultSets) {
            String name = rs.getName();
            KaplenMaierValue[] r = tool.getSortedResult(rs);
            sortedOriginRawResults.put(name, r);
        }
        
        HashMap<String, KaplenMaierValue[]> sortedAuxRawResults = new HashMap<>();
        
        for (ResultSet rs : auxResultSets) {
            String name = rs.getName();
            KaplenMaierValue[] r = tool.getSortedResult(rs);
            sortedAuxRawResults.put(name, r);
        }
       
        int cnt = 0;
        
        for (String name : originDrillVectors) {
            KaplenMaierValue[] finalRs = sortedOriginRawResults.get(name);
            KaplenMaierValue[] auxRs = sortedAuxRawResults.get(drillVectorMapper.get(name));

            ResultSet newRs = getKMProcessedResultSet(finalRs, auxRs, name);
            result[cnt] = newRs;
            cnt++;
        }
        return result;
    }


    /**
     * Ez a függvény kiszámolja a ResultSetben lévő elemek, levelId alapján
     * rendezett halmazon, levelId-ig tartó KM mutatók produktumát (a levelId
     * elem még beleszámítodik).
     *
     * @param finalRs ezen a halmazon kell számolni LevelId-ig a KM mutatók
     * produktumát
     * @param auxRs eddig az elemig számítodik a produktum
     * @return a levelId-gy számított produktum
     */
    private ResultSet getKMProcessedResultSet(KaplenMaierValue[] finalRs, KaplenMaierValue[] auxRs, String finalName) {
        ResultSet result = null;
        List<ResultElement> auxElements = getKMProcessedSelectedResponse(auxRs);
        List<ResultElement> finalElements = tool.mergedResultElementLists(finalRs, auxElements);

        result = new ResultSet(finalName);
        result.setResponse(finalElements);

        return result;
    }

   

    /**
     * Ez az eljárás kiszámolja a Kaplan-Meier mutató értékét, és az
     * eredményhalmazba beleteszi azon sorokat, amikos a Kaplan-Meier dimenzió
     * knownId értéke mehegyezik az eredeti baseVector KM dim legutolsó szintjén lévő értékkel.
     * Ezt akkor használjuk, ha van fúrás a baseVectorban a KM dim mentén. 
     * Ekkor egy újjabb base vektort veszünk hozunk létre, amelyben a km dim menti fúrást egy szinttel feljebb léptetjük,
     * majd ezen eredményhalmazt adjuk ennek a fv-nek.
     *
     * @param sortedArray a rendezett halmaz amelyből a megfelelő knownId-hoz tartozó értéket meg szeretnlénk határozni.
     * @return a KM feldolgozott és szelektált sorok listája
     */
    private List<ResultElement> getKMProcessedSelectedResponse(KaplenMaierValue[] sortedArray) {
        List<ResultElement> result = new ArrayList<>();

        double produktum = 1.0;
        int lastKaplanMeierDimensionId = -1979;
        
        for (KaplenMaierValue k : sortedArray) {
            
            if (lastKaplanMeierDimensionId >= k.kaplenMaierDimensionId) {                
                produktum = 1.0;
            }
            
            produktum = produktum * k.row.getMeasureValues()[this.kaplanMeierMeasureIdx];
            
            if(k.kaplenMaierDimensionId == this.kaplanMeierDimensioLastLevelId){
                ResultElement tempRow = k.row.deepCopy();
                tempRow.getMeasureValues()[this.kaplanMeierMeasureIdx] = produktum;
                result.add(tempRow);
            }            
            lastKaplanMeierDimensionId = k.kaplenMaierDimensionId;
        }      
        return result;
    }
}
