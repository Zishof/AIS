package ais.action.master.library.modern.test;

import org.json.JSONObject;

import ais.action.master.library.modern.LibraryDigitalUrlPolicy;
import ais.action.master.library.modern.LibraryRateLimiter;
import ais.action.master.library.modern.LibraryTelemetry;

/** Fast dependency-free contract checks for public URL, limiter, and telemetry policies. */
public final class LibraryModernPolicySelfTest {
    private LibraryModernPolicySelfTest() { }

    public static void main(String[] args) throws Exception {
        check("https://example.org/book.pdf".equals(LibraryDigitalUrlPolicy.safe("https://example.org/book.pdf")), "HTTPS URL ditolak");
        check("/document?id=1".equals(LibraryDigitalUrlPolicy.safe("/document?id=1")), "Path lokal aman ditolak");
        check(LibraryDigitalUrlPolicy.safe("//evil.example/book") == null, "Protocol-relative URL diterima");
        check(LibraryDigitalUrlPolicy.safe("/../WEB-INF/web.xml") == null, "Traversal diterima");
        check(LibraryDigitalUrlPolicy.safe("/files/%2e%2e/secret.pdf") == null, "Traversal terenkode diterima");
        check(LibraryDigitalUrlPolicy.safe("javascript:alert(1)") == null, "Skema berbahaya diterima");
        check(LibraryDigitalUrlPolicy.safe("/file\\name") == null, "Backslash diterima");

        String remote = "policy-test-" + System.nanoTime();
        check(LibraryRateLimiter.allow("library-self-test", remote, 2, 60000L), "Request pertama ditolak");
        check(LibraryRateLimiter.allow("library-self-test", remote, 2, 60000L), "Request kedua ditolak");
        check(!LibraryRateLimiter.allow("library-self-test", remote, 2, 60000L), "Limit tidak diterapkan");
        check(!LibraryRateLimiter.allow("bad namespace", remote, 2, 60000L), "Namespace invalid diterima");

        LibraryTelemetry.record("self-test", 200, 10L);
        LibraryTelemetry.record("self-test", 500, 30L);
        JSONObject health = LibraryTelemetry.snapshot();
        check("UP".equals(health.optString("status")), "Telemetry tidak UP");
        check(health.optLong("requests") >= 2L && health.optLong("errors") >= 1L, "Counter telemetry tidak bertambah");
        System.out.println("LibraryModernPolicySelfTest OK url-policy rate-limit telemetry");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
