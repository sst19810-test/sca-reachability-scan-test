package demo.sast.cross;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class HttpFetcher {
    // CWE-918: url is fully attacker-controlled (internal hosts, file:, cloud metadata)
    public String get(String url) throws Exception {
        URLConnection conn = new URL(url).openConnection();
        if (conn instanceof HttpsURLConnection https) {
            https.setSSLSocketFactory(trustAll().getSocketFactory());
            // CWE-297: hostname verification disabled
            https.setHostnameVerifier((host, session) -> true);
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // CWE-295: accepts any certificate chain
    private static SSLContext trustAll() throws Exception {
        TrustManager[] managers = {new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }};
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, managers, new SecureRandom());
        return context;
    }
}
