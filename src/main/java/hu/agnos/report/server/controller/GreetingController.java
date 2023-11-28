package hu.agnos.report.server.controller;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GreetingController.class);

    private static final String template = "Hello, %s!";

    /**
     * Endpoint to check if the service is up and running.
     *
     * @param name Name of the caller
     * @return "Hello" if any
     */
    @GetMapping("/greeting")
    public static String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        log.debug("Hearth beat check");
        return String.format(template, name);
    }

}
