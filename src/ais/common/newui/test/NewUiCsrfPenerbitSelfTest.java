package ais.common.newui.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Menjaga agar setiap controller yang MEMVALIDASI CSRF keluarga {@code ok}
 * juga MENERBITKAN tokennya.
 *
 * <p>Keluarga itu memakai namespace tersendiri: atribut sesi
 * {@code newUiCsrfToken} dan header {@code X-CSRF-Token}. Semula enam belas
 * controller memvalidasinya sementara hanya lima yang menerbitkannya, sehingga
 * jalur tulis sebelas layar hanya berfungsi bila pengguna kebetulan membuka
 * salah satu dari lima layar itu lebih dulu pada sesi yang sama. Cacat itu
 * tidak terlihat dari sisi mana pun sendirian: uji server hijau karena sesinya
 * sudah memegang token, dan uji halaman juga hijau karena token disuntik.</p>
 *
 * <p>Karena itu penjaganya membaca SUMBER, bukan menjalankan permintaan —
 * yang ingin dijamin adalah sifat struktural: pembaca dan penerbit tidak boleh
 * terpisah. Uji ini tidak memerlukan basis data maupun kontainer servlet.</p>
 */
public final class NewUiCsrfPenerbitSelfTest {

    private static final String SUMBER = "C:/opt/AIS/ais/src/main/src/ais";
    private static final String BACA = "getAttribute(\"newUiCsrfToken\")";
    private static final String TERBIT_LANGSUNG = "setAttribute(\"newUiCsrfToken\"";
    private static final String TERBIT_BERSAMA = "getTokenOkFlat(";

    private NewUiCsrfPenerbitSelfTest() { }

    public static void main(String[] args) {
        List<File> berkas = new ArrayList<File>();
        kumpulkan(new File(SUMBER), berkas);

        List<String> pembacaSaja = new ArrayList<String>();
        int pembaca = 0;
        for (int i = 0; i < berkas.size(); i++) {
            File f = berkas.get(i);
            String isi = baca(f);
            if (isi.indexOf(BACA) < 0) continue;
            pembaca++;
            boolean menerbitkan = isi.indexOf(TERBIT_LANGSUNG) >= 0
                    || isi.indexOf(TERBIT_BERSAMA) >= 0;
            if (!menerbitkan) pembacaSaja.add(f.getName());
        }

        check(pembaca > 0, "Sapuan tidak menemukan satu pun pembaca token; "
                + "kemungkinan jalur sumber salah: " + SUMBER);
        if (!pembacaSaja.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pembacaSaja.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(pembacaSaja.get(i));
            }
            throw new IllegalStateException(pembacaSaja.size()
                    + " controller memvalidasi newUiCsrfToken tanpa pernah menerbitkannya, "
                    + "sehingga seluruh jalur tulisnya dijawab 403 kecuali pengguna "
                    + "kebetulan membuka layar lain yang mencetaknya: " + sb);
        }
        System.out.println("PASS New UI CSRF penerbit self-test (" + pembaca + " pembaca)");
    }

    private static void kumpulkan(File dir, List<File> out) {
        File[] anak = dir.listFiles();
        if (anak == null) return;
        for (int i = 0; i < anak.length; i++) {
            if (anak[i].isDirectory()) kumpulkan(anak[i], out);
            else if (anak[i].getName().endsWith(".java")) out.add(anak[i]);
        }
    }

    private static String baca(File f) {
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
