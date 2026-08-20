package ais.action.master.library.modern;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;

/** Configurable, disabled-by-default gateway for library interoperability. */
public final class LibraryIntegrationGateway {
    private static final int MAX_RESPONSE = 1024 * 1024;
    private LibraryIntegrationGateway() { }

    public static JSONObject handle(HttpServletRequest request) throws Exception {
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null || !Common.getApakahAdmin()) return fail("Hak administrator perpustakaan diperlukan.");
        String action = text(request.getParameter("action"), 40);
        if (action == null || "status".equals(action)) return status(request);
        if (!"execute".equals(action)) return fail("Operasi integrasi tidak dikenal.");
        if (!"POST".equalsIgnoreCase(request.getMethod())) return fail("Eksekusi integrasi hanya melalui POST.");
        if (!NewUiCsrfUtil.isValid(request)) return fail("Token keamanan tidak valid.");

        String adapter = text(request.getParameter("adapter"), 30);
        String payload = text(request.getParameter("payload"), 65536);
        if ("sru".equals(adapter)) return sru(payload);
        if ("ncip".equals(adapter)) return httpXml("ncip", payload);
        if ("sip2".equals(adapter)) return sip2(payload);
        if ("rfid".equals(adapter)) return httpJson("rfid", payload);
        if ("sushi".equals(adapter)) return sushi(payload);
        return fail("Adapter tidak dikenal.");
    }

    private static JSONObject status(HttpServletRequest request) throws Exception {
        JSONArray adapters = new JSONArray();
        adapters.put(info("oai", true, "Provider OAI-PMH tersedia pada endpoint _oai."));
        adapters.put(info("sru", enabled("library.integration.sru"), "Search/Retrieve via URL; dapat menjadi bridge Z39.50."));
        adapters.put(info("ncip", enabled("library.integration.ncip"), "Pertukaran pesan NCIP melalui HTTPS."));
        adapters.put(info("sip2", enabled("library.integration.sip2"), "Pertukaran SIP2 melalui socket TCP terkonfigurasi."));
        adapters.put(info("rfid", enabled("library.integration.rfid"), "Bridge RFID vendor melalui HTTPS."));
        adapters.put(info("sushi", enabled("library.integration.sushi"), "Pengambilan laporan COUNTER melalui SUSHI HTTPS."));
        return ok().put("data", adapters).put("csrf", NewUiCsrfUtil.getToken(request.getSession()));
    }

    private static JSONObject info(String id, boolean ready, String description) throws Exception {
        return new JSONObject().put("id", id).put("enabled", ready).put("description", description);
    }

    private static JSONObject sru(String query) throws Exception {
        requireEnabled("sru");
        if (query == null) throw new IllegalArgumentException("Query CQL wajib diisi.");
        String endpoint = endpoint("sru");
        String separator = endpoint.indexOf('?') >= 0 ? "&" : "?";
        String url = endpoint + separator + "version=1.2&operation=searchRetrieve&recordSchema=marcxml&maximumRecords=20&query="
                + URLEncoder.encode(query, "UTF-8");
        return ok().put("adapter", "sru").put("response", http("GET", url, null, "application/xml", token("sru")));
    }

    private static JSONObject httpXml(String adapter, String payload) throws Exception {
        requireEnabled(adapter);
        if (payload == null || payload.indexOf('<') < 0) throw new IllegalArgumentException("Payload XML wajib diisi.");
        return ok().put("adapter", adapter).put("response", http("POST", endpoint(adapter), payload, "application/xml", token(adapter)));
    }

    private static JSONObject httpJson(String adapter, String payload) throws Exception {
        requireEnabled(adapter);
        if (payload == null) throw new IllegalArgumentException("Payload JSON wajib diisi.");
        new JSONObject(payload);
        return ok().put("adapter", adapter).put("response", http("POST", endpoint(adapter), payload, "application/json", token(adapter)));
    }

    private static JSONObject sushi(String pathAndQuery) throws Exception {
        requireEnabled("sushi");
        String endpoint = endpoint("sushi");
        if (pathAndQuery != null) {
            if (pathAndQuery.contains("://") || pathAndQuery.contains("..")) throw new IllegalArgumentException("Path SUSHI tidak valid.");
            endpoint += (endpoint.endsWith("/") || pathAndQuery.startsWith("/") ? "" : "/") + pathAndQuery;
        }
        return ok().put("adapter", "sushi").put("response", http("GET", endpoint, null, "application/json", token("sushi")));
    }

    private static JSONObject sip2(String message) throws Exception {
        requireEnabled("sip2");
        if (message == null) throw new IllegalArgumentException("Pesan SIP2 wajib diisi.");
        message = message.replace("\r", "").replace("\n", "");
        String host = config("library.integration.sip2.host", null);
        int port = integer(config("library.integration.sip2.port", null), 1, 65535);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeout());
            socket.setSoTimeout(timeout());
            OutputStream output = socket.getOutputStream();
            output.write((message + "\r").getBytes("UTF-8")); output.flush();
            InputStream input = socket.getInputStream();
            StringBuilder response = new StringBuilder(); int value;
            while ((value = input.read()) != -1 && value != '\r' && response.length() < MAX_RESPONSE) response.append((char) value);
            return ok().put("adapter", "sip2").put("response", response.toString());
        } finally { try { socket.close(); } catch (Exception ignored) { } }
    }

    private static String http(String method, String url, String body, String contentType, String bearer) throws Exception {
        if (!url.startsWith("https://") && !Boolean.parseBoolean(config("library.integration.allow_http", "false")))
            throw new IllegalStateException("Endpoint harus HTTPS.");
        HttpURLConnection connection = (HttpURLConnection) new java.net.URL(url).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(timeout()); connection.setReadTimeout(timeout()); connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", contentType); connection.setRequestProperty("Content-Type", contentType + "; charset=UTF-8");
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
            if (body != null) { connection.setDoOutput(true); OutputStream out=connection.getOutputStream(); try{out.write(body.getBytes("UTF-8"));out.flush();}finally{out.close();} }
            int code=connection.getResponseCode(); InputStream stream=code>=400?connection.getErrorStream():connection.getInputStream();
            String response=read(stream); if(code<200||code>=300)throw new IllegalStateException("Endpoint mengembalikan HTTP " + code + ": " + response);
            return response;
        } finally { connection.disconnect(); }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return ""; BufferedReader reader=new BufferedReader(new InputStreamReader(stream,"UTF-8"));
        try{StringBuilder result=new StringBuilder();String line;while((line=reader.readLine())!=null){if(result.length()+line.length()>MAX_RESPONSE)throw new IllegalStateException("Respons integrasi melebihi batas 1 MB.");result.append(line).append('\n');}return result.toString();}finally{reader.close();}
    }

    private static void requireEnabled(String adapter) { if(!enabled("library.integration."+adapter))throw new IllegalStateException("Adapter " + adapter + " belum diaktifkan."); }
    private static boolean enabled(String prefix){return Boolean.parseBoolean(config(prefix+".enabled","false"));}
    private static String endpoint(String adapter){String value=config("library.integration."+adapter+".endpoint",null);if(value==null)throw new IllegalStateException("Endpoint " + adapter + " belum dikonfigurasi.");return value;}
    private static String token(String adapter){return config("library.integration."+adapter+".token",null);}
    private static int timeout(){try{return integer(config("library.integration.timeout_ms","10000"),1000,60000);}catch(Exception e){return 10000;}}
    private static int integer(String value,int min,int max){try{int n=Integer.parseInt(value);if(n<min||n>max)throw new Exception();return n;}catch(Exception e){throw new IllegalArgumentException("Konfigurasi angka tidak valid.");}}
    private static String config(String key,String fallback){try{Konfigurasi c=Common.getKonfigurasi(key,fallback==null?"":fallback);String v=c==null?null:c.getNilai();return v==null||v.trim().length()==0?fallback:v.trim();}catch(Exception e){return fallback;}}
    private static String text(String value,int max){if(value==null||value.trim().length()==0)return null;value=value.trim();if(value.length()>max)throw new IllegalArgumentException("Input terlalu panjang.");return value;}
    private static JSONObject ok()throws Exception{return new JSONObject().put("ok",true).put("status","success");}
    private static JSONObject fail(String message)throws Exception{return new JSONObject().put("ok",false).put("status","error").put("error",message).put("message",message);}
}
