package ais.common.newui.laporan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memastikan tiap filter laporan cocok dengan deklarasi {@code <parameter>}
 * pada template Jasper-nya — namanya ada, dan tipe Java yang dikirim controller
 * sama dengan yang dituntut template.
 *
 * <h3>Mengapa perlu uji terpisah</h3>
 * <p>{@code NewUiLaporanUmumSelfTest} sudah memeriksa berkas {@code .jasper}
 * ada. Itu tidak cukup: sebuah filter dapat menunjuk template yang ada namun
 * memakai nama parameter yang tidak dikenal template, atau mengirim
 * {@code Long} ke parameter yang dideklarasikan {@code String}.</p>
 *
 * <p>Keduanya gagal dengan diam. Nama yang salah membuat filter tidak
 * berpengaruh apa pun — pengguna mengisi kolom lalu heran hasilnya sama.
 * Tipe yang salah membuat Jasper menyaring dengan nilai yang tidak pernah
 * cocok, atau mencetak kop kosong. Tidak ada galat, tidak ada baris log; yang
 * ada hanya laporan yang salah dan tampak wajar.</p>
 *
 * <h3>Yang dibandingkan</h3>
 * <table><tr><th>Jenis filter</th><th>Tipe yang dikirim</th></tr>
 * <tr><td>tahun, bulan</td><td>java.lang.Integer</td></tr>
 * <tr><td>tanggal</td><td>java.lang.String (format basis data)</td></tr>
 * <tr><td>relasi</td><td>java.lang.Long, atau String bila {@code idTeks()}</td></tr>
 * <tr><td>relasi_banyak</td><td>java.util.List</td></tr>
 * <tr><td>teks</td><td>java.lang.String</td></tr></table>
 */
public final class NewUiLaporanTipeParameterSelfTest {

    private static final String[] KANDIDAT_WEBAPP = {
        "src/main/webapp", "webapp", "C:/opt/AIS/ais/src/main/webapp",
    };

    private static int gagal = 0;
    private static int diperiksa = 0;
    private static int takDideklarasikan = 0;

    public static void main(String[] args) throws Exception {
        File webapp = cariWebapp();
        if (webapp == null) {
            System.out.println("GAGAL: direktori webapp tidak ditemukan.");
            System.exit(1);
        }

        String[] kunci = NewUiLaporanUmumController.semuaLaporan();
        for (int i = 0; i < kunci.length; i++) {
            String k = kunci[i];
            String template = NewUiLaporanUmumController.templateUntuk(k);
            File jrxml = new File(webapp, "report/" + template + ".jrxml");
            if (!jrxml.isFile()) {
                // Bukan kegagalan: sebagian template hanya dikirim dalam bentuk
                // .jasper terkompilasi, dan tipe parameternya tidak dapat
                // dibaca dari sana. Dihitung agar cakupan uji ini jujur.
                takDideklarasikan++;
                continue;
            }
            Map<String, String> tipe = parameterJrxml(jrxml);
            List<NewUiLaporanUmumController.Filter> filter =
                    NewUiLaporanUmumController.filterUntuk(k);
            if (filter == null) continue;
            for (int f = 0; f < filter.size(); f++) {
                NewUiLaporanUmumController.Filter x = filter.get(f);
                periksa(k, template, tipe, x);
            }
        }

        System.out.println("laporan: " + kunci.length
                + ", filter diperiksa: " + diperiksa
                + ", template tanpa jrxml: " + takDideklarasikan);
        if (gagal > 0) {
            System.out.println("GAGAL NewUiLaporanTipeParameter self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiLaporanTipeParameter self-test");
    }

    private static void periksa(String kunci, String template,
            Map<String, String> tipe, NewUiLaporanUmumController.Filter f) {
        diperiksa++;
        if (f.diolahUlang) {
            // Namanya memang tidak sampai ke template apa adanya; controller
            // menggantinya sebelum mengirim. Ditandai eksplisit di registri
            // supaya pengecualian ini terlihat di tempat keputusannya dibuat,
            // bukan tersembunyi sebagai daftar putih di dalam uji.
            return;
        }
        String dideklarasikan = tipe.get(f.nama);
        if (dideklarasikan == null) {
            lapor(kunci + ": filter '" + f.nama + "' tidak dideklarasikan template "
                    + template + " -- filter ini tidak berpengaruh apa pun");
            return;
        }
        if ("java.lang.Object".equals(dideklarasikan)) {
            // Parameter bertipe Object menerima nilai apa pun; tidak ada yang
            // dapat disimpulkan dari deklarasinya.
            return;
        }
        String diharapkan = diharapkan(f);
        if (diharapkan != null && !diharapkan.equals(dideklarasikan)) {
            lapor(kunci + ": filter '" + f.nama + "' mengirim " + diharapkan
                    + " tetapi template " + template + " menuntut " + dideklarasikan);
        }
        if (f.paramTanggalTampilan != null) {
            String t = tipe.get(f.paramTanggalTampilan);
            if (t == null) {
                lapor(kunci + ": parameter tampilan '" + f.paramTanggalTampilan
                        + "' tidak dideklarasikan template " + template);
            } else if (!"java.lang.String".equals(t)) {
                lapor(kunci + ": parameter tampilan '" + f.paramTanggalTampilan
                        + "' seharusnya String, template menuntut " + t);
            }
        }
    }

    private static String diharapkan(NewUiLaporanUmumController.Filter f) {
        String t = f.tipe;
        if ("tahun".equals(t) || "bulan".equals(t)) return "java.lang.Integer";
        if ("tanggal".equals(t)) {
            return f.tanggalSebagaiObjek ? "java.util.Date" : "java.lang.String";
        }
        if ("relasi".equals(t)) return f.idSebagaiTeks ? "java.lang.String" : "java.lang.Long";
        if ("relasi_banyak".equals(t)) return "java.util.List";
        if ("teks".equals(t)) return "java.lang.String";
        return null;
    }

    private static Map<String, String> parameterJrxml(File jrxml) throws Exception {
        Map<String, String> hasil = new HashMap<String, String>();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(jrxml), "UTF-8"));
        try {
            String baris;
            while ((baris = r.readLine()) != null) {
                int i = baris.indexOf("<parameter name=");
                if (i < 0) continue;
                String nama = antara(baris, "name=", i);
                String kelas = antara(baris, "class=", i);
                if (nama != null && kelas != null) hasil.put(nama, kelas);
            }
        } finally {
            r.close();
        }
        return hasil;
    }

    private static String antara(String baris, String kunci, int dari) {
        int i = baris.indexOf(kunci, dari);
        if (i < 0) return null;
        int buka = baris.indexOf(34, i);
        if (buka < 0) return null;
        int tutup = baris.indexOf(34, buka + 1);
        if (tutup < 0) return null;
        return baris.substring(buka + 1, tutup);
    }

    private static void lapor(String pesan) {
        gagal++;
        System.out.println("  - " + pesan);
    }

    private static File cariWebapp() {
        for (int i = 0; i < KANDIDAT_WEBAPP.length; i++) {
            File f = new File(KANDIDAT_WEBAPP[i]);
            if (f.isDirectory() && new File(f, "report").isDirectory()) return f;
        }
        return null;
    }
}
