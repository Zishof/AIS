package ais.common.newui.menu.test;

import java.io.File;
import java.util.Map;

import ais.common.newui.menu.NewUiRuteEksplisitRegistry;
import ais.common.newui.menu.NewUiRuteEksplisitRegistry.Tujuan;

/**
 * Memastikan setiap keputusan pemetaan di {@link NewUiRuteEksplisitRegistry}
 * benar-benar menunjuk halaman yang ada.
 *
 * <p>Registry itu dikonsultasikan lebih dulu daripada seluruh pencarian nama,
 * sehingga satu salah ketik di sana mengalahkan mekanisme yang tadinya bekerja
 * dan mengirim pengguna ke halaman yang tidak ada. Uji ini memeriksa keberadaan
 * berkas {@code uiux} maupun {@code services} untuk tiap entri, sehingga entri
 * yang salah ketik atau usang menggagalkan uji alih-alih menunggu ditemukan
 * pengguna.</p>
 */
public final class NewUiRuteEksplisitRegistrySelfTest {

    private static final String[] KANDIDAT_WEBAPP = {
        "src/main/webapp",
        "webapp",
        "C:/opt/AIS/ais/src/main/webapp",
    };

    private static int gagal = 0;

    public static void main(String[] args) {
        File webapp = cariWebapp();
        if (webapp == null) {
            // Gagal, bukan melewatkan diri: uji yang diam-diam lulus ketika
            // berkasnya tak ditemukan akan selalu hijau justru di lingkungan
            // yang tata letaknya berbeda.
            System.out.println("GAGAL: direktori webapp tidak ditemukan; uji tidak dapat dijalankan.");
            System.out.println("       dicari relatif terhadap: " + new File(".").getAbsolutePath());
            System.exit(1);
        }

        Map<String, Tujuan> semua = NewUiRuteEksplisitRegistry.semua();
        if (semua.isEmpty()) {
            lapor("registry kosong; entri hilang tanpa uji ikut diperbarui");
        }

        for (Map.Entry<String, Tujuan> e : semua.entrySet()) {
            String route = e.getKey();
            Tujuan t = e.getValue();

            if (t.getModule() == null || t.getModule().trim().length() == 0) {
                lapor(route + ": modul kosong");
            }
            if (t.getPage() == null || t.getPage().trim().length() == 0) {
                lapor(route + ": page kosong");
            }
            if (t.getAlasan() == null || t.getAlasan().trim().length() == 0) {
                // Alasan wajib: entri tanpa dasar adalah tebakan, dan tebakan
                // pada pemetaan halaman menghasilkan layar yang tampak wajar
                // dengan isi yang salah.
                lapor(route + ": tidak menyertakan dasar keputusan");
            }

            File uiux = new File(webapp, "WEB-INF/new/" + t.getModule()
                    + "/uiux/" + t.getPage() + ".jsp");
            File service = new File(webapp, "WEB-INF/new/" + t.getModule()
                    + "/services/" + t.getPage() + "_service.jsp");
            if (!uiux.isFile()) lapor(route + ": halaman tidak ada -> " + uiux.getPath());
            if (!service.isFile()) lapor(route + ": service tidak ada -> " + service.getPath());
        }

        if (NewUiRuteEksplisitRegistry.cari("route/yang/tidak/terdaftar") != null) {
            lapor("route tak terdaftar seharusnya mengembalikan null");
        }
        if (NewUiRuteEksplisitRegistry.cari(null) != null) {
            lapor("route null seharusnya mengembalikan null");
        }

        if (gagal > 0) {
            System.out.println("GAGAL NewUiRuteEksplisitRegistry self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiRuteEksplisitRegistry self-test ("
                + semua.size() + " keputusan, semuanya menunjuk halaman yang ada)");
    }

    private static void lapor(String pesan) {
        gagal++;
        System.out.println("  - " + pesan);
    }

    private static File cariWebapp() {
        for (int i = 0; i < KANDIDAT_WEBAPP.length; i++) {
            File f = new File(KANDIDAT_WEBAPP[i]);
            if (f.isDirectory() && new File(f, "WEB-INF/new").isDirectory()) return f;
        }
        return null;
    }
}
