/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation.KaplanMeier;

import hu.agnos.cube.driver.zolikaokos.ResultElement;

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
        String[] kaplanMeierDimensioArray = this.row.getHeader()[kaplanMeierDimensioIdx].split(",", -1);
        String idAsString = kaplanMeierDimensioArray[0].split(":", -1)[1];

        kaplenMaierDimensionId = Integer.parseInt(idAsString.substring(1, idAsString.length() - 1));
        kaplenMaierDimensionKnownId = kaplanMeierDimensioArray[1].split(":", -1)[1];
    }
}
