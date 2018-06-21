import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;


public class ServerProxy {

    static String host;

    public static void main(String[] args) throws Exception {
        int port = 8001;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {
            host = exchange.getRequestURI().getHost();
            Logger.logValues(host, 1, 0, 0);
            HttpURLConnection conn = (HttpURLConnection) exchange.getRequestURI().toURL().openConnection();
            String url = URLDecoder.decode(exchange.getRequestURI().toURL().toString(), "UTF-8");

            try {
                String[] blackListed = checkBlackList();
                for (String var : blackListed) {
                    if (url.contains(var)) {
                        System.out.println("Blacklisted");
                        String response = "<p style=\"color:red;font-size:30px;\">This page is blacklisted!</p>";
                        exchange.getResponseHeaders().set("Content-type", "text/html");
                        exchange.sendResponseHeaders(403, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                }

                conn = setupConn(exchange);
                readBodyBytes(conn, exchange);
                conn = transferData(exchange, conn);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                conn.disconnect();
            }
        }

        /* https://stackoverflow.com/questions/2163644/in-java-how-can-i-convert-an-inputstream-into-a-byte-array-byte */
        public static byte[] readFully(InputStream input) throws IOException {
            byte[] buffer = new byte[32768];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return output.toByteArray();
        }

        public HttpURLConnection setupConn(HttpExchange exchange) throws IOException {
            URL url = exchange.getRequestURI().toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            String requestMethod = exchange.getRequestMethod();
            conn.setRequestMethod(requestMethod);
            conn.setRequestProperty("Via", "localhost");
            conn.setRequestProperty("X-Forwarded-For", exchange.getRemoteAddress().toString());
            Map<String, List<String>> reqHead = exchange.getRequestHeaders();
            for (Map.Entry<String, List<String>> en : reqHead.entrySet()) {
                if (en != null && en.getKey() != null) {
                    for (String value : en.getValue()) {
                        conn.setRequestProperty(en.getKey(), value);
                    }
                }
            }
            return conn;
        }

        public void readBodyBytes(HttpURLConnection conn, HttpExchange exchange) throws IOException {
            byte[] reqBytes = null;
            InputStream bodyIs = exchange.getRequestBody();
            byte[] requestBodyBytes = readFully(bodyIs);
            int length = requestBodyBytes.length;
            Logger.logValues(host, 0, length, 0);
           // System.out.println("Body bytes: " + requestBodyBytes.length);
            if (exchange.getRequestMethod().equals("POST")) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(requestBodyBytes);
                // os.close();
            }

        }

        public HttpURLConnection transferData(HttpExchange exchange, HttpURLConnection conn) throws IOException {
            Map<String, List<String>> headers = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry != null && entry.getKey() != null && !entry.getKey().toLowerCase().equals("transfer-encoding")) {
                    for (String value : entry.getValue()) {
                        exchange.getResponseHeaders().set(entry.getKey(), value);
                    }
                }
            }
            int responseCode = conn.getResponseCode();
            InputStream inpstr;
            if (responseCode >= 400) {
                inpstr = conn.getErrorStream();
            } else {
                inpstr = conn.getInputStream();
            }
            byte[] response = readFully(inpstr);

            int length = 0;

            if (response == null) {
                length = -1;
           } else {
                length = response.length;
            }

          //  tempLogger.setBytesReceived(tempLogger.getBytesReceived()+length);
            Logger.logValues(host, 0, 0, length);
            exchange.sendResponseHeaders(conn.getResponseCode(), length);
            length = 0;
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();

            return conn;
        }

        public String[] checkBlackList() {
            List<String> blackPaths = new ArrayList<>();
            String[] blackPathsArray = null;
            File blackList = new File("blacklist.txt");
            try {
                BufferedReader br = new BufferedReader(new FileReader(blackList));
                String st;
                while ((st = br.readLine()) != null) {
                    blackPaths.add(st);
                }
                blackPathsArray = new String[blackPaths.size()];
                blackPaths.toArray(blackPathsArray);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return blackPathsArray;
        }

    ////////////////////


        }
/////////////////////////

    }



