package hu.agnos.report.server.entity;

import java.util.List;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.repository.ReportRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ReportList {

    private List<Report> reportList;

    @Autowired
    public ReportList(CubeList cubeList) {
        this.init(cubeList);
    }

    public void init(CubeList cubeList) {
        this.reportList = (new ReportRepository()).findAll();
        //System.out.println(reportList.size());
        setBrokenStatesFromAvailableCubes(cubeList);
    }

    public void setBrokenStatesFromAvailableCubes(CubeList cubeList) {
        for (Report r : reportList) {
            for(Cube c : r.getCubes()) {
                if ((cubeList == null) || !cubeList.cubeMap().containsKey(c.getName())) {
                    // System.out.println("BROKKKI");
                    r.setBroken(true);
                    break;
                }
            }
        }
    }

}
