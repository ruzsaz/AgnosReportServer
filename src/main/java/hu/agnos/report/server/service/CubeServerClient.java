package hu.agnos.report.server.service;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.cube.meta.resultDto.ResultSet;
import hu.agnos.report.server.service.queryGenerator.CubeQueryCreator;
import org.apache.http.client.utils.URIBuilder;

/**
 *
 * @author parisek
 */

public class CubeServerClient {

    // TODO: nem kéne new cubeservice... sőt, az egész osztály nem kéne

    public static Optional<ResultSet[]> getCubeData(String cubeServerUri, String cubeName, String baseVector, String drillVectorsComrressOneString) {
        return null;
    }

    public static ResultSet[] getCubeData(String cubeServerUri, CubeQuery cubeQuery) {
        try {
            String body = new ObjectMapper().writeValueAsString(cubeQuery);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URIBuilder(cubeServerUri + "/data").build())
                    .timeout(Duration.of(20, SECONDS))
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            return new ObjectMapper().readValue(response.body(), ResultSet[].class);
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(CubeQueryCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Optional<CubeList> getCubeList(String cubeServerUri) {
        CubeList cubeList = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(cubeServerUri + "/cube_list"))
                    .timeout(Duration.of(10, SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            cubeList = new ObjectMapper().readValue(response.body(), CubeList.class);
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(CubeQueryCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.ofNullable(cubeList);
    }
}
