/**
 * Itt a baseVektor mindvégig változatlan, viszont a drillVektor(ok)ban minimum egy, de akár több dimenzió mentén is fúrhatunk.
 *
 *      Vizsgálni kell, hogy fúrtunk-e a Kaplan-Meier dimenzió mentén.
 *
 *      Ha van fúrás, akkor a drillvektort változatlanúl hagyjuk, majd az eredményhalmazon képezzük
 *      a Kaplan-Meier mutató kommulát szorzatát (sorról-sorra), figyelve arra, hogy akár több dimenzió mentén is alábonthatunk egyszerre.
 *
 *      Ha eredetileg nincs fúrás a Kaplan-Meier dimenzió mentén, akkor szükség van egy újjabb drillvektorra, amely az eredetitől anyiban tér el,
 *      hogy a Kaplan-Meier dimenzió mentén is alábont (elképzelhető, hogy az így kapott drillVektor szerepel az eredeti drillVektorok között,
 *      ez esetben ezt nem kell kétszer kiszámolni, csak megfelelően kezelni). A végleges eredményt a két (eredeti és újjonan meghatározott) drillVektor
 *      eredményhalmazát közösen felhasználva határozzuk meg.
 *      Vesszük az újonnan létrehozott drillvektor eredményhalmazát és abban meghatározzuk a kommulát szorzatot (sorról-sorra), majd második lépésben,
 *      ebből kidobáljuk azon sorokat, amelyben a Kaplan-Meier dimenzió értéke nem maximális.
 *      Az így kapott eredményhalmaz sorainak száma meg kell egyezen az eredeti drillVektor eredményhalmazának sorszámával, a két tábla sorainak megfelelő
 *      párosításával az új drilvektor eredményhalmazának Kaplan-Meier mutató értékeivel felülírjuk az eredezi drillVektor eredményhalmazának Kaplan-Meier
 *      mutató értékeit.
 */
package hu.agnos.report.server.service.additionalcalculation.KaplanMeier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import hu.agnos.cube.meta.resultDto.ResultSet;
import hu.agnos.cube.meta.resultDto.ResultElement;
import hu.agnos.report.server.service.CubeServerClient;
import hu.agnos.report.server.util.DrillVectorCompressor;

/**
 *
 * @author parisek
 */
public class KaplanMeierConstBaseVector {

    private int kaplanMeierDimensioIdx;
    private int kaplanMeierMeasureIdx;
    private String baseVector;
    private String[] originDrillVectors;
    private String cubeName;
    private KaplanMeierTool tool;
    private CubeServerClient cubeServerClient;

    
    public KaplanMeierConstBaseVector(int kaplanMeierDimensioIdx, int kaplanMeierMeasureIdx, String baseVector, String[] originDrillVectors, String cubeName) {
        this.kaplanMeierDimensioIdx = kaplanMeierDimensioIdx;
        this.kaplanMeierMeasureIdx = kaplanMeierMeasureIdx;
        this.baseVector = baseVector;
        this.originDrillVectors = originDrillVectors;
        this.cubeName = cubeName;
        this.tool = new KaplanMeierTool(kaplanMeierDimensioIdx, kaplanMeierMeasureIdx);
        this.cubeServerClient = cubeServerClient;
    }

    public ResultSet[] process(String cubeServerUri) {

        ResultSet[] result = new ResultSet[originDrillVectors.length];

//        System.out.println("result.length: " + result.length);
        // ha egy drillVector nem bontja meg a KM dimenziót, akkor szükség van egy
        //plusz drillVectorra, amely megbontja. Ebbe a map-be kulcs az ilyen drillVector,
        //míg érték a új drillVector
        HashMap<String, String> drillVectorMapper = new HashMap<>();

        //ez az elöző lépésben létrehozott új drillVector-ok tömbje
        //erre azért van szükség, mert ezen drillvektorok eredményhalmazát nem kell önmagában feldolgozni
        Set<String> auxiliaryDrillVectors = new HashSet<>();

        //Ebbe a halmazba azon drillvektorok kerülnek, amelyek eredetileg nem bontották meg a Kaplen-Meier dimenziót
        //Mivel ezekhez van aux drillvektor és önmagába nem használhatóak, ezért értékeit felülírhatjuk, mivel az eredeti 
        //eredményt egy más drillvektor mértékének meghatározásához nem fogjuk felhasználni. Ők az első körben feldolgozandó
        //drillvektorok
        Set<String> firstStepProcessedDrillVectors = new HashSet<>();
        //Ebbe a halmazba azon drillvektorok kerülnek, amelyek eredetileg már megbontották a Kaplen-Meier dimenziót (ezek csak originális drillvektorok lehetnek, aux nem).
        //Őket azért kell másodkörben feldolgozni, mert lehetséges, hogy első lépésben felhasználjuk őket más drillvektorok eredményhalmazának meghatározásához.
        Set<String> secondStepProcessedDrillVectors = new HashSet<>();
        long startTime = System.nanoTime();
        for (String drillVector : originDrillVectors) {
            String newDrillVector = tool.getNewDrillVector(drillVector);
//            System.out.println("newDrillVector: " + newDrillVector + ", drillVector: " + drillVector);
            if (!newDrillVector.equals(drillVector)) {

                firstStepProcessedDrillVectors.add(drillVector);

                drillVectorMapper.put(drillVector, newDrillVector);
                if (!hasSuchDrillVector(newDrillVector)) {
                    auxiliaryDrillVectors.add(newDrillVector);
                }
            } else {
                secondStepProcessedDrillVectors.add(drillVector);
            }
        }
//long nextTime = System.nanoTime();
// System.out.println("A preproc eddig tartott: " + (nextTime-startTime));
// startTime = System.nanoTime();
        // ebbe a tömbbe kerülnek az originális és az aux drillvektorok, őket adjuk oda a statementnek
        String[] extendedDrillVectorsArray = new String[auxiliaryDrillVectors.size() + originDrillVectors.length];

        int i = 0;
        for (String drillV : originDrillVectors) {
            extendedDrillVectorsArray[i] = drillV;
            i++;
        }

        for (String drillV : auxiliaryDrillVectors) {
            extendedDrillVectorsArray[i] = drillV;
            i++;
        }
//nextTime = System.nanoTime();
// System.out.println("A preproc2 eddig tartott: " + (nextTime-startTime));
// startTime = System.nanoTime();
       String drillVectorsComrressOneString = DrillVectorCompressor.compressDrillVectorsInOneString(extendedDrillVectorsArray);

        ResultSet[] normalResultSets = null;
        
        Optional<ResultSet[]> optionalResultSet = cubeServerClient.getCubeData(cubeServerUri, cubeName, baseVector, drillVectorsComrressOneString);
        if (optionalResultSet.isPresent()) {
            normalResultSets = optionalResultSet.get();
        }

//        nextTime = System.nanoTime();
// System.out.println("A nyuszi eddig tartott: " + (nextTime-startTime));
// startTime = System.nanoTime();
        //a statement-től visszakapott eredményhalhazokat rendezzük, majd ebbe a tárba rakjuk
        HashMap<String, KaplenMaierValue[]> sortedRawResults = new HashMap<>();

        for (ResultSet rs : normalResultSets) {
//            String name = rs.name();
//            KaplenMaierValue[] r = tool.getSortedResult(rs);
//            sortedRawResults.put(name, r);
        }

//        nextTime = System.nanoTime();
// System.out.println("A sort eddig tartott: " + (nextTime-startTime));
// startTime = System.nanoTime();
        //erre a tömbre az első és a másodkörös feldolgozás miatt van szükség, ide kerülnek a már feldolgozott eredményhalmazok, 
        //de a sorrendjük még nem megfelelő
        HashMap<String, ResultSet> majdnemResult = new HashMap<>();

        for (String name : firstStepProcessedDrillVectors) {

            KaplenMaierValue[] finalRs = sortedRawResults.get(name);
            KaplenMaierValue[] auxRs = sortedRawResults.get(drillVectorMapper.get(name));

            ResultSet newRs = getKMProcessedResultSet(finalRs, auxRs, name);
            //TODO
            majdnemResult.put(name, newRs);

        }
//nextTime = System.nanoTime();
// System.out.println("A firstStap eddig tartott: " + (nextTime-startTime));
// startTime = System.nanoTime();
        for (String name : secondStepProcessedDrillVectors) {
            KaplenMaierValue[] finalRs = sortedRawResults.get(name);
            ResultSet newRs = getKMProcessedResultSet(finalRs, name);
            majdnemResult.put(name, newRs);
        }
//nextTime = System.nanoTime();
// System.out.println("A secund eddig tartott: " + (nextTime-startTime));
// startTime = System.nanoTime();
        int cnt = 0;
        //az eredményt a kérések sorrendjébe adjuk
        for (String name : originDrillVectors) {
            result[cnt] = majdnemResult.get(name);
            cnt++;
        }

        return result;
    }

    private boolean hasSuchDrillVector(String testedDrillVector) {
        boolean result = false;
        for (String s : this.originDrillVectors) {
            if (testedDrillVector.equals(s)) {
                result = true;
                break;
            }
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

        // TODO: a két null csak azéet van itt, hogy forduljon,
        result = null;
        //result.setResponse(finalElements);

        return result;
    }

    /**
     * Ez az eljárás a paraméteréül kapott eredményhalmazának KM mutató értékét
     * módosítja a megfelelő KM értékkel
     *
     * @param rs a módosítandó halmaz
     * @return egy új eredményhalmaz (másik objektum), a módosított értékekkel
     */
    private ResultSet getKMProcessedResultSet(KaplenMaierValue[] rs, String finalName) {
        // TODO: A két null kamu itt
        ResultSet result = null;
        List<ResultElement> newResponse = getKMProcessedResponse(rs);
        //result.setResponse(newResponse);
        return result;
    }

    private List<ResultElement> getKMProcessedResponse(KaplenMaierValue[] sortedArray) {
        List<ResultElement> result = new ArrayList<>();

        double produktum = 1.0;
        int lastKaplanMeierDimensionId = -1979;

        for (KaplenMaierValue k : sortedArray) {

            if (lastKaplanMeierDimensionId >= k.kaplenMaierDimensionId) {
                produktum = 1.0;
            }

            ResultElement tempRow = k.row;
            double[] measureValues = tempRow.measureValues();
            produktum = produktum * measureValues[this.kaplanMeierMeasureIdx];
            measureValues[this.kaplanMeierMeasureIdx] = produktum;

            result.add(tempRow);

            lastKaplanMeierDimensionId = k.kaplenMaierDimensionId;

        }
        return result;
    }

    /**
     * Ez az eljárás kiszámolja a Kaplan-Meier mutató értékét, és az
     * eredményhalmazba beleteszi azon sorokat, amikos a Kaplan-Meier dimenzió
     * újabb csoportra vált (az aktuális KM dim knownId kisseb vagy egyenlő az
     * elöző KM dim knownId értéknél). Ezt akkor használjuk, amikor van egy
     * segéd drillVektor, amellyel a KM mutató értékét alábontjuk és ezen
     * alábontást szeretnénk eltüntetni.
     *
     * @param sortedArray a rendezett aux halmaz, amelyből a kummulált produktum
     * értékének meghatározása történi
     * @return a KM feldolgozott és szelektált sorok listája
     */
    private List<ResultElement> getKMProcessedSelectedResponse(KaplenMaierValue[] sortedArray) {
        List<ResultElement> result = new ArrayList<>();

        double produktum = 1.0;
        int lastKaplanMeierDimensionId = -1979;

        ResultElement tempRow = null;
        KaplenMaierValue k = null;

        for (int i = 0; i < sortedArray.length; i++) {
            k = sortedArray[i];

            if (lastKaplanMeierDimensionId >= k.kaplenMaierDimensionId) {
                tempRow = sortedArray[i - 1].row.deepCopy();
                tempRow.measureValues()[this.kaplanMeierMeasureIdx] = produktum;
                result.add(tempRow);

                produktum = 1.0;
            }

            produktum = produktum * k.row.measureValues()[this.kaplanMeierMeasureIdx];

            lastKaplanMeierDimensionId = k.kaplenMaierDimensionId;

        }
        tempRow = k.row.deepCopy();
        tempRow.measureValues()[this.kaplanMeierMeasureIdx] = produktum;
        result.add(tempRow);

        return result;
    }

}
