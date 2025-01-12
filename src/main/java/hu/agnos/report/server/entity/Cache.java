package hu.agnos.report.server.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.resultDto.ResultSet;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class Cache {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Cache.class);
    private static final double LONGTIME = 100;
    private final String reportDirectoryURI = System.getenv("AGNOS_REPORTS_DIR");
    private final String CACHEFILENAME = "slowCache.cache";

    private final Map<CubeQuery, ResultSet> fastCache;
    private final Map<CubeQuery, ResultSet> slowCache;
    @Getter
    private int missCount = 0;   // Number of cache misses
    @Getter
    private int getCount = 0;   // Number of total cache lookups

    @Autowired
    public Cache() {
        fastCache = Collections.synchronizedMap(new LRUMap<>(1000));
        slowCache = Collections.synchronizedMap(new LRUMap<>(2500));
        restoreFromFile();
    }

    public void insert(CubeQuery key, ResultSet value, long computeTimeInMilliseconds, boolean isSlow) {
        missCount++;
        if (computeTimeInMilliseconds > Cache.LONGTIME || isSlow) {
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

    @SuppressWarnings("unchecked")
    private void restoreFromFile() {
        File path = new File(this.reportDirectoryURI, CACHEFILENAME);
        try (FileInputStream fileIn = new FileInputStream(path); ObjectInputStream in = new ObjectInputStream(fileIn)) {
            slowCache.putAll( (Map<CubeQuery, ResultSet>) in.readObject());
            log.info("Slow cache restored from disk.");
        } catch (IOException | ClassNotFoundException ex) {
            log.error("Slow cache restoration failed.", ex);
        }
    }

    public void saveToFile() {
        File path = new File(this.reportDirectoryURI, CACHEFILENAME);
        try (ObjectOutput out = new java.io.ObjectOutputStream(new java.io.FileOutputStream(path))) {
            out.writeObject(slowCache);
            log.info("Slow cache saved to disk.");
        } catch (IOException ex) {
            log.error("Slow cache saving failed.", ex);
        }
    }

    public String toString() {
        return "Cache: " + slowCache.size() + " slow, " + fastCache.size() + " fast, " + String.format("%2.2f", 100.0 * (1.0-((double)missCount/getCount))) + "% hit ratio";
    }

}
