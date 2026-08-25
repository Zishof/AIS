package ais.common.azure;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Small manual SharePoint token connectivity probe.
 *
 * Credentials are external-only. Never add defaults or print the returned token.
 */
public class SharepointAccessClass {
    private static final int TIMEOUT_MS = 15000;

    public static void main(String[] args) throws Exception {
        String clientId = requiredEnvironment("AIS_SHAREPOINT_CLIENT_ID");
        String tenantId = requiredEnvironment("AIS_SHAREPOINT_TENANT_ID");
        String clientSecret = requiredEnvironment("AIS_SHAREPOINT_CLIENT_SECRET");
        String sharepointHost = requiredEnvironment("AIS_SHAREPOINT_HOST");
        if (!sharepointHost.matches("[A-Za-z0-9.-]+")) {
            throw new IllegalArgumentException("AIS_SHAREPOINT_HOST tidak valid.");
        }

        String endpoint = "https://accounts.accesscontrol.windows.net/"
                + encodePathSegment(tenantId) + "/tokens/OAuth/2";
        String requestData = "grant_type=client_credentials"
                + "&client_id=" + encodeForm(clientId + "@" + tenantId)
                + "&client_secret=" + encodeForm(clientSecret)
                + "&resource=" + encodeForm("00000003-0000-0ff1-ce00-000000000000/"
                        + sharepointHost + "@" + tenantId);
        byte[] body = requestData.getBytes("UTF-8");

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setFixedLengthStreamingMode(body.length);

        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        try {
            output.write(body);
            output.flush();
        } finally {
            output.close();
        }

        int status = connection.getResponseCode();
        InputStreamReader stream = new InputStreamReader(
                status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream(),
                "UTF-8");
        StringBuilder response = new StringBuilder();
        BufferedReader reader = new BufferedReader(stream);
        try {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        } finally {
            reader.close();
            connection.disconnect();
        }

        if (status < 200 || status >= 300 || response.indexOf("\"access_token\"") < 0) {
            throw new IllegalStateException("SharePoint token probe gagal, HTTP " + status + ".");
        }
        System.out.println("SharePoint token probe berhasil; token tidak ditampilkan.");
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalStateException("Environment wajib: " + key);
        }
        return value.trim();
    }

    private static String encodeForm(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private static String encodePathSegment(String value) throws Exception {
        return encodeForm(value).replace("+", "%20");
    }
}

