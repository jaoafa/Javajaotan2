/*
 * jaoLicense
 *
 * Copyright (c) 2021 jao Minecraft Server
 *
 * The following license applies to this project: jaoLicense
 *
 * Japanese: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE.md
 * English: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE-en.md
 */

package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class HTTPServer extends Thread {
    static HttpServer server;

    @Override
    public void run() {
        int port = 31002;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", exc -> {
                final String response = "{\"status\":true,\"message\":\"Hello world.\"}";
                exc.sendResponseHeaders(200, response.length());
                OutputStream os = exc.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });
            server.createContext("/docs", new HTTP_GetDocs());
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stopServer() {
        server.stop(0);
    }

    static class HTTP_GetDocs implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Headers resHeaders = t.getResponseHeaders();
            resHeaders.set("Content-Type", "application/json");
            resHeaders.set("Last-Modified",
                ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));

            resHeaders.set("Server", "Javajaotan2 Server (" + Main.class.getPackage().getImplementationVersion() + ")");

            long contentLength = Main.getCommands().toString().getBytes(StandardCharsets.UTF_8).length;
            t.sendResponseHeaders(200, contentLength);

            OutputStream os = t.getResponseBody();
            os.write(Main.getCommands().toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}
