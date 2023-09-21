package hu.agnos.report.server.entity;

import java.util.ArrayList;
import java.util.List;

import hu.agnos.report.entity.Report;
import hu.agnos.report.repository.ReportRepository;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ReportList {

    private List<Report> reportList;

    public ReportList() {
        this.init();
    }

    public void init() {
        List<Report> tmpReportList = (new ReportRepository()).findAll();
        this.reportList = tmpReportList;
    }


    public List<Report> getReportList() {
        return reportList;
    }

    public void setReportList(List<Report> reportList) {
        this.reportList = reportList;
    }
}
