package hu.agnos.report.server.configuration;

import java.util.List;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.server.util.CubeServerClient;
import hu.mi.agnos.report.entity.Report;
import hu.mi.agnos.report.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.stereotype.Component;

/**
 *
 * @author parisek
 */
@Component
@Configuration
public class ReportServerConfiguration {
    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri ;


    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public List<Report> getReportList() {
        return (new ReportRepository()).findAll();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CubeList getCubeList() {
        return CubeServerClient.getCubeList(cubeServerUri).orElse(null);
    }

}
