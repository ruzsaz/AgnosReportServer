package hu.agnos.report.server.service.additionalcalculation.KaplanMeier;


import hu.agnos.cube.meta.resultDto.ResultElement;

/**
 *
 * @author parisek
 */
public class KaplenMaierValue {

    protected int kaplenMaierDimensionId;    
    protected String kaplenMaierDimensionKnownId;
    protected ResultElement row;
    protected int[] sortOrder;

    public KaplenMaierValue(ResultElement oneRow, int[] sortOrder, int kaplanMeierDimensioIdx) {
        this.row = oneRow;//.deepCopy();
        this.sortOrder = sortOrder;

//            System.out.println("this.row.Header: " + this.row.printHeader());
        //String idAsString = this.row.header()[kaplanMeierDimensioIdx].id().toString();

        //kaplenMaierDimensionId = Integer.parseInt(idAsString.substring(1, idAsString.length() - 1));
        //kaplenMaierDimensionKnownId = this.row.header()[kaplanMeierDimensioIdx].knownId();
    }
}
