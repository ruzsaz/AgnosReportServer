/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator;

import hu.agnos.report.server.util.CubeQueryLocator;
import hu.mi.agnos.report.entity.Report;


/**
 *
 * @author parisek
 */
public interface AbstractQueryGenerator {
    
    public abstract String getQuery(CubeQueryLocator cql, Report report);
    
}
