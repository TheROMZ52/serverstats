package com.mahbod.serverstats;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves the dashboard (static HTML/JS/CSS embedded as jar resources)
 * plus a small JSON API, all from inside the plugin process.
 * No separate Node/Python process, no extra ports beyond the one configured.
 */
public class WebServer {

    private final StatsDatabase db;
    private final Logger logger;
    private final String serverName;
    private HttpServer server;

    public WebServer(StatsDatabase db, Logger logger, String serverName) {
        this.db = db;
        this.logger = logger;
        this.serverName = serverName;
    }

    public void start(String bindAddress, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext("/api/players", this::handlePlayers);
        server.createContext("/api/events", this::handleEvents);
        server.createContext("/api/meta", this::handleMeta);
        server.createContext("/", this::handleStatic);
        // small fixed thread pool so this never competes hard with the main server thread
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        logger.info("ServerStats web dashboard listening on " + bindAddress + ":" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handlePlayers(HttpExchange ex) throws IOException {
        String json = SimpleJson.toJson(db.getAllPlayers());
        sendJson(ex, json);
    }

    private void handleEvents(HttpExchange ex) throws IOException {
        String json = SimpleJson.toJson(db.getRecentEvents(50));
        sendJson(ex, json);
    }

    private void handleMeta(HttpExchange ex) throws IOException {
        String json = SimpleJson.toJson(Map.of(
                "server_name", serverName,
                "server_time", System.currentTimeMillis() / 1000L
        ));
        sendJson(ex, json);
    }

    /** Serves index.html and any assets embedded under resources/web/ in the jar. */
    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) path = "/index.html";

        String resourcePath = "/web" + path;
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            String notFound = "404 - not found";
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(404, notFound.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(notFound.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        byte[] bytes = in.readAllBytes();
        in.close();

        ex.getResponseHeaders().set("Content-Type", guessContentType(path));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendJson(HttpExchange ex, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
