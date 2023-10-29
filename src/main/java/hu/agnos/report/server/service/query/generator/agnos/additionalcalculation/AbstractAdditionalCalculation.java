package hu.agnos.report.server.service.query.generator.agnos.additionalcalculation;

import hu.agnos.cube.meta.dto.ResultSet;
import lombok.Getter;


/**
 *
 * @author parisek
 */
public abstract class AbstractAdditionalCalculation {
    
    protected String[] hierarchyHeader;
    protected String[] measureHeader;
    protected String args;

    @Getter
    protected boolean correctArgs;
    protected String cubeName;
    protected String cubeServerUri;

    public AbstractAdditionalCalculation(String args, String cubeName, String[] hierarchyHeader, String[] measureHeader, String cubeServerUri) {
        this.args = args;
        this.cubeName = cubeName;
        this.hierarchyHeader= hierarchyHeader;
        this.measureHeader = measureHeader;
        this.correctArgs = false;
        this.cubeServerUri = cubeServerUri;
    }

    public abstract ResultSet[] process(String newBaseVector, String[] drillVectorsArray);
    
    
}
