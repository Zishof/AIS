package ais.action.master.jurnal.test;

import ais.action.master.jurnal.JurnalProfileValidator;

/**
 * Harness uji manual (bukan JUnit, dijalankan lewat {@code main}) untuk {@link JurnalProfileValidator}
 * — validator konfigurasi profil jurnal (integrasi OJS) berbasis JSON, mencakup tiga area:
 * {@code workflow} (alur review), {@code access} (kebijakan akses/harga), dan {@code metadata}
 * (UI publik jurnal). Kasus yang diuji: (1) workflow valid diterima dan menghasilkan JSON ternormalisasi
 * tidak kosong; (2) form review yang sudah berstatus {@code PUBLISHED} bersifat immutable — mencoba
 * menghapusnya dari workflow baru harus ditolak; (3) kebijakan akses dengan harga negatif ditolak;
 * (4) kebijakan akses format {@code OPEN} valid diterima; (5) metadata UI publik lengkap (locale,
 * blok kustom, analytics GA4, halaman publik) valid diterima; (6) blok kustom dengan {@code key}
 * duplikat ditolak; (7) konfigurasi analytics dengan {@code measurementId} berformat salah ditolak.
 * Helper {@link #deny} memverifikasi bahwa suatu aksi melempar {@link IllegalArgumentException}
 * sebagaimana diharapkan; {@link #check} melempar {@link IllegalStateException} bila asersi gagal.
 */
public final class JurnalProfileValidatorSelfTest {
    private JurnalProfileValidatorSelfTest() {}

    public static void main(String[] args) {
        final String old = "{\"schemaVersion\":1,\"reviewForms\":[{\"formKey\":\"standard\","
                + "\"version\":1,\"status\":\"PUBLISHED\",\"elements\":[{\"elementKey\":\"quality\","
                + "\"type\":\"SCALE\"}]}]}";
        check(JurnalProfileValidator.workflow(old, null).length() > 0, "valid workflow");
        deny(new Runnable() {
            public void run() {
                JurnalProfileValidator.workflow("{\"schemaVersion\":1,\"reviewForms\":[]}", old);
            }
        }, "published immutable");
        deny(new Runnable() {
            public void run() {
                JurnalProfileValidator.access("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"paid\","
                        + "\"format\":\"SUBSCRIPTION\",\"price\":-1}]}");
            }
        }, "negative price");
        check(JurnalProfileValidator.access("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"open\","
                + "\"format\":\"OPEN\"}]}").length() > 0, "valid access");

        String metadata = "{\"schemaVersion\":1,\"locales\":[\"id_ID\"],\"publicUi\":{"
                + "\"allowSubmissions\":true,\"information\":{\"reader\":\"Info\"},"
                + "\"customBlocks\":[{\"key\":\"about\",\"title\":\"Tentang\",\"bodyText\":\"Isi\"}],"
                + "\"analytics\":{\"provider\":\"GA4\",\"measurementId\":\"G-ABCDEF12\"}},"
                + "\"publicPages\":[{\"slug\":\"ethics\",\"title\":\"Etik\",\"bodyText\":\"Isi\"}]}";
        check(JurnalProfileValidator.metadata(metadata).length() > 0, "valid metadata UI");
        deny(new Runnable() {
            public void run() {
                JurnalProfileValidator.metadata("{\"schemaVersion\":1,\"publicUi\":{\"customBlocks\":["
                        + "{\"key\":\"x\",\"title\":\"A\",\"bodyText\":\"A\"},"
                        + "{\"key\":\"x\",\"title\":\"B\",\"bodyText\":\"B\"}]}}");
            }
        }, "duplicate custom block");
        deny(new Runnable() {
            public void run() {
                JurnalProfileValidator.metadata("{\"schemaVersion\":1,\"publicUi\":{\"analytics\":{"
                        + "\"provider\":\"GA4\",\"measurementId\":\"bad\"}}}");
            }
        }, "invalid analytics");
        System.out.println("JurnalProfileValidatorSelfTest OK workflow access public-ui bounded");
    }

    private static void deny(Runnable action, String message) {
        boolean denied = false;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            denied = true;
        }
        check(denied, message);
    }

    private static void check(boolean valid, String message) {
        if (!valid) {
            throw new IllegalStateException(message);
        }
    }
}
