package hu.agnos.report.server.entity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import org.apache.commons.collections4.map.LRUMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.cube.meta.resultDto.ResultSet;

@Getter
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class Cache {

    private static final double LONGTIME = 100;

    private Map<CubeQuery, ResultSet[]> fastCache;
    private Map<CubeQuery, ResultSet[]> slowCache;

    @Autowired
    public Cache(CubeList cubeList) {
        init();
    }

    public void init() {
        fastCache = Collections.synchronizedMap(new LRUMap<>(1000));
        slowCache = Collections.synchronizedMap(new LRUMap<>(1000));
    }

    // TODO: thread-safe version
    public void insert(CubeQuery key, ResultSet[] value, long computeTime) {
        if (computeTime > LONGTIME) {
            slowCache.put(key, value);
        } else {
            fastCache.put(key, value);
        }
    }

    public Optional<ResultSet[]> get(CubeQuery key) {
        return Optional.ofNullable(slowCache.getOrDefault(key, fastCache.get(key)));
    }

}
