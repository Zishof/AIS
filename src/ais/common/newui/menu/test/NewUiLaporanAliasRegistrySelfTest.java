package ais.common.newui.menu.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ais.common.newui.menu.NewUiLaporanAliasRegistry;

/**
 * Menjaga {@link NewUiLaporanAliasRegistry} tetap sama dengan rantai
 * {@code else if} di {@code Common.launchMenu}.
 *
 * <p>Tabel alias dibangkitkan dari sumber itu, tetapi tersimpan sebagai kode.
 * Begitu seseorang menambah menu laporan baru di {@code Common}, tabel ini
 * menjadi usang — dan gejalanya hanyalah satu menu yang melaporkan "adaptor
 * belum dikonfigurasi", persis gejala yang membuat 67 menu salah dihitung
 * sebagai pekerjaan sisa. Uji ini mengubah penyimpangan diam itu menjadi
 * kegagalan uji.</p>
 */
public final class NewUiLaporanAliasRegistrySelfTest {

    private static final Pattern POLA = Pattern.compile(
            "equals\(\"([A-Za-z0-9_]+)\"\)\s*\)\s*\{\s*(?:final\s+)?"
            + "([A-Za-z0-9_]+Window)\s+\w+\s*=\s*new\s+\2\s*\(");

    /** Lokasi sumber yang dicoba berurutan; relatif terhadap direktori kerja. */
    private static final String[] KANDIDAT = {
        "src/main/src/ais/common/Common.java",
        "src/main/java/ais/common/Common.java",
        "ais/common/Common.java",
        "C:/opt/AIS/ais/src/main/src/ais/common/Common.java",
    };

    private static int gagal = 0;

    public static void main(String[] args) throws Exception {
        File sumber = cariSumber();
        if (sumber == null) {
            // Sengaja gagal, bukan lewat diam-diam. Uji yang melewatkan dirinya
            // sendiri ketika tidak menemukan berkas akan selalu hijau di CI yang
            // tata letaknya berbeda, dan justru di sanalah penyimpangan lolos.
            System.out.println("GAGAL: Common.java tidak ditemukan; uji drift tidak dapat dijalankan.");
            System.out.println("       dicari relatif terhadap: " + new File(".").getAbsolutePath());
            System.exit(1);
        }

        Map<String, String> dariSumber = ekstraksi(sumber);
        Map<String, String> dariTabel = new TreeMap<String, String>(NewUiLaporanAliasRegistry.semuaAlias());

        for (Map.Entry<String, String> e : dariSumber.entrySet()) {
            String ada = dariTabel.get(e.getKey());
            if (ada == null) {
                lapor("alias '" + e.getKey() + "' ada di Common.launchMenu tetapi tidak di registry");
            } else if (!ada.equals(e.getValue())) {
                lapor("alias '" + e.getKey() + "' menunjuk " + ada
                        + " di registry, tetapi " + e.getValue() + " di Common.launchMenu");
            }
        }
        for (String kunci : dariTabel.keySet()) {
            if (!dariSumber.containsKey(kunci)) {
                lapor("alias '" + kunci + "' ada di registry tetapi sudah tidak ada di Common.launchMenu");
            }
        }

        // Sembilan menu yang menjadi sebab lapisan ini dibuat.
        String[] wajib = {
            "rekapitulasiValidasiKeuangan", "rekapitulasiItemBiaya",
            "laporanDaftarHadirDosenSemua", "laporanTranskripAkademik",
            "laporanUjianSidangSkripsi", "rekapDosenPa",
            "rekap_jumlah_mahasiswa_fakultas", "rekapitulasiAlumniJurusan",
            "rekapitulasiDataMahasiswa",
        };
        for (int i = 0; i < wajib.length; i++) {
            if (NewUiLaporanAliasRegistry.windowUntuk(wajib[i]) == null) {
                lapor("alias wajib '" + wajib[i] + "' tidak terpetakan");
            }
        }

        if (NewUiLaporanAliasRegistry.windowUntuk("tidak_ada_alias_ini") != null) {
            lapor("alias tak dikenal seharusnya mengembalikan null");
        }
        if (NewUiLaporanAliasRegistry.windowUntuk(null) != null) {
            lapor("url null seharusnya mengembalikan null");
        }

        if (gagal > 0) {
            System.out.println("GAGAL NewUiLaporanAliasRegistry self-test: " + gagal + " masalah");
            System.exit(1);
        }
        System.out.println("PASS NewUiLaporanAliasRegistry self-test ("
                + dariSumber.size() + " alias cocok dengan Common.launchMenu)");
    }

    private static void lapor(String pesan) {
        gagal++;
        System.out.println("  - " + pesan);
    }

    private static File cariSumber() {
        for (int i = 0; i < KANDIDAT.length; i++) {
            File f = new File(KANDIDAT[i]);
            if (f.isFile()) return f;
        }
        return null;
    }

    private static Map<String, String> ekstraksi(File sumber) throws Exception {
        StringBuilder isi = new StringBuilder();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(sumber), "UTF-8"));
        try {
            String baris;
            while ((baris = r.readLine()) != null) isi.append(baris).append((char) 10);
        } finally {
            r.close();
        }
        Map<String, String> hasil = new TreeMap<String, String>();
        Matcher m = POLA.matcher(isi.toString());
        while (m.find()) {
            if (!hasil.containsKey(m.group(1))) hasil.put(m.group(1), m.group(2));
        }
        return hasil;
    }
}
