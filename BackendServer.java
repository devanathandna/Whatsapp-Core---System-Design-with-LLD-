import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.*;
import java.util.*;

public class BackendServer {
    static List<Map<String,String>> messages = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/messages", new MessagesHandler());
        server.createContext("/api/health", exchange -> {
            addCorsHeaders(exchange);
            String resp = "{\"status\":\"ok\"}";
            byte[] b = resp.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
            exchange.close();
        });
        server.setExecutor(null);
        System.out.println("Java backend started at http://localhost:" + port);
        server.start();
    }

    static class MessagesHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                addCorsHeaders(exchange);
                String resp = buildJson();
                byte[] bytes = resp.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            } else if (method.equalsIgnoreCase("POST")) {
                addCorsHeaders(exchange);
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), "UTF-8");
                Map<String,String> m = parseJson(body);
                if (!m.containsKey("author")) m.put("author", "anon");
                if (!m.containsKey("text")) m.put("text", "");
                messages.add(m);
                String resp = "{\"status\":\"ok\"}";
                byte[] bytes = resp.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(201, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static void addCorsHeaders(HttpExchange e) {
        var h = e.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
    }

    static String buildJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        synchronized(messages) {
            boolean first = true;
            for (Map<String,String> m : messages) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{");
                sb.append("\"author\":").append(escape(m.get("author"))).append(",");
                sb.append("\"text\":").append(escape(m.get("text")));
                sb.append("}");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    static String escape(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    static Map<String,String> parseJson(String body) {
        Map<String,String> m = new HashMap<>();
        try {
            // naive extractor for simple JSON objects like {"author":"x","text":"y"}
            String a = extract(body, "\"author\"");
            String t = extract(body, "\"text\"");
            if (a != null) m.put("author", a);
            if (t != null) m.put("text", t);
        } catch (Exception ex) {
            // ignore parse errors
        }
        return m;
    }

    static String extract(String body, String key) {
        int i = body.indexOf(key);
        if (i < 0) return null;
        int col = body.indexOf(":", i);
        if (col < 0) return null;
        int firstQuote = body.indexOf('"', col);
        if (firstQuote < 0) return null;
        int secondQuote = body.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return body.substring(firstQuote + 1, secondQuote);
    }
}
