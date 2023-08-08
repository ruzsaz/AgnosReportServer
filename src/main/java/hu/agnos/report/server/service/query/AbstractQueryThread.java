/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query;

/**
 *
 * @author parisek
 */
public interface AbstractQueryThread {

    public abstract QueryStateEnum getDownloaderState();

    public abstract StringBuilder getResult();

    public abstract String getTHREADNAME();

    public abstract Exception getException();
}
