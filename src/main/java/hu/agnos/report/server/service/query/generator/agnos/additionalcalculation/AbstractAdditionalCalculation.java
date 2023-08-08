/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation;

import hu.agnos.cube.driver.ResultSet;
import hu.agnos.report.server.util.CubeServerClient;


/**
 *
 * @author parisek
 */
public abstract class AbstractAdditionalCalculation {
    
    protected String[] hierarchyHeader;
    protected String[] measureHeader;
    protected String args;
    protected boolean correctArgs;
    protected String cubeName;
    protected CubeServerClient cubeServerClient;

    public AbstractAdditionalCalculation( String args, String cubeName, String[] hierarchyHeader, String[] measureHeader, CubeServerClient cubeServerClient) { 
        this.cubeName = cubeName;
        this.hierarchyHeader= hierarchyHeader;
        this.measureHeader = measureHeader;
        this.correctArgs = false;
        this.cubeServerClient = cubeServerClient;
    }
    
    public boolean isCorrectArgs(){
        return this.correctArgs;
    }
    
    public abstract ResultSet[] process(String newBaseVector, String[] drillVectorsArray);
    
    
}
