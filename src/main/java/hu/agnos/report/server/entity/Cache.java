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
import hu.agnos.cube.meta.resultDto.ResultSet;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class Cache {

    private static final double LONGTIME = 100;
    private final Map<CubeQuery, ResultSet> fastCache;
    private final Map<CubeQuery, ResultSet> slowCache;
    @Getter
    private int missCount = 0;   // Number of cache misses
    @Getter
    private int getCount = 0;   // Number of total cache lookups

    @Autowired
    public Cache() {
        fastCache = Collections.synchronizedMap(new LRUMap<>(1000));
        slowCache = Collections.synchronizedMap(new LRUMap<>(1000));
    }

    public void insert(CubeQuery key, ResultSet value, long computeTimeInMilliseconds) {
        missCount++;
        if (computeTimeInMilliseconds > Cache.LONGTIME) {
            slowCache.put(key, value);
        } else {
            fastCache.put(key, value);
        }
    }

    public Optional<ResultSet> get(CubeQuery key) {
        getCount++;
        return Optional.ofNullable(slowCache.getOrDefault(key, fastCache.get(key)));
    }

    public int getHitCount() {
        return getCount - missCount;
    }

    public String toString() {
        return "Cache: " + slowCache.size() + " slow, " + fastCache.size() + " fast, " + String.format("%2.2f", 100.0 * (1.0-((double)missCount/getCount))) + "% hit ratio";
    }

}
