/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hu.agnos.report.server.configuration;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.cube.meta.http.CubeClient;
import hu.agnos.report.server.util.CubeServerClient;
import hu.mi.agnos.report.entity.Report;
import hu.mi.agnos.report.repository.ReportRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
/**
 *
 * @author parisek
 */
public class ReportServerConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public List<Report> getReportList() {
        return (new ReportRepository()).findAll();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Optional<CubeList> getCubetList() {
        return (new CubeServerClient()).getCubetList();
    }

}
