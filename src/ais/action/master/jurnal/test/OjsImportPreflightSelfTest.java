package ais.action.master.jurnal.test;

import ais.action.master.jurnal.importer.OjsImportPreflightService;

/**
 * Harness uji manual untuk {@link OjsImportPreflightService}, yang memeriksa (secara read-only)
 * kesiapan struktur database OJS (Open Journal Systems) sebelum proses impor data jurnal
 * dijalankan. Kredensial koneksi ke database fixture OJS TIDAK di-hardcode di sini — wajib
 * disuplai lewat variabel lingkungan {@code AIS_JURNAL_OJS_FIXTURE_JDBC},
 * {@code AIS_JURNAL_OJS_FIXTURE_USER}, {@code AIS_JURNAL_OJS_FIXTURE_PASSWORD}, dan harness
 * menolak berjalan (melempar {@link IllegalStateException}) bila salah satu kosong atau JDBC URL
 * tidak mengandung penanda {@code ojs_jurnal_fixture_3505} — pagar ini mencegah harness tidak
 * sengaja diarahkan ke database OJS produksi. Setelah preflight dijalankan, harness memvalidasi
 * bahwa struktur fixture persis sesuai ekspektasi: 134 tabel diharapkan dan 134 ditemukan, 905
 * kolom ditemukan, tidak ada tabel yang hilang, dan versi OJS terdeteksi {@code 3.5.0-5}.
 */
public final class OjsImportPreflightSelfTest {
    private OjsImportPreflightSelfTest() {}
    /** Menjalankan preflight terhadap database fixture OJS (kredensial dari variabel lingkungan) dan memvalidasi jumlah tabel/kolom serta versi OJS yang terdeteksi; lihat javadoc kelas. */
    public static void main(String[] args) throws Exception {
        String jdbc=System.getenv("AIS_JURNAL_OJS_FIXTURE_JDBC"),user=System.getenv("AIS_JURNAL_OJS_FIXTURE_USER"),password=System.getenv("AIS_JURNAL_OJS_FIXTURE_PASSWORD");
        if(jdbc==null||!jdbc.contains("ojs_jurnal_fixture_3505")||user==null||password==null)throw new IllegalStateException("Fixture environment tidak aman/lengkap.");
        OjsImportPreflightService.Config c=new OjsImportPreflightService.Config();c.jdbcUrl=jdbc;c.user=user;c.password=password;c.schema="public";
        OjsImportPreflightService.Result r=new OjsImportPreflightService().inspect(c);
        if(r.expectedTables!=134||r.foundTables!=134||r.foundFields!=905||!r.missing.isEmpty()||!"3.5.0-5".equals(r.version))throw new IllegalStateException("Preflight bukan 134/905 OJS 3.5.0-5.");
        System.out.println("OjsImportPreflightSelfTest OK version=3.5.0-5 tables=134 fields=905 read-only");
    }
}
