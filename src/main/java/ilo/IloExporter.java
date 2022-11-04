package ilo;

import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class IloExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IloExporter.class);

    public static void main(String[] args) throws IOException {
        String port = System.getenv().getOrDefault(Environment.PORT, "9416");
        LOGGER.info("Starting server on port: {}", port);
        new IloCollector().register();
        new HTTPServer(Integer.parseInt(port));
    }

}
