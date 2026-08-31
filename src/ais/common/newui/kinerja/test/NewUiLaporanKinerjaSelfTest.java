package ais.common.newui.kinerja;

import java.io.File;
import java.util.List;

/**
 * Self-test kontrak laporan kinerja tanpa container Servlet maupun basis data.
 *
 * <p>Yang dijaga: setiap jenis laporan menunjuk template Jasper yang benar-benar
 * ada di webapp, dan jenis yang tidak dikenal ditolak (fail-closed) alih-alih
 * diam-diam memakai template lain.</p>
 *
 * <p>Argumen opsional: akar direktori webapp (bawaan: src/main/webapp).</p>
 */
public final class NewUiLaporanKinerjaSelfTest {

    private NewUiLaporanKinerjaSelfTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) throws Exception {
        String webapp = args.length > 0 ? args[0] : "src/main/webapp";

        List<String> jenis = NewUiLaporanKinerjaController.semuaJenis();
        check(jenis.size() == 6, "jumlah jenis laporan kinerja");

        for (String kode : jenis) {
            java.lang.reflect.Method m = NewUiLaporanKinerjaController.class
                    .getDeclaredMethod("varian", String.class);
            m.setAccessible(true);
            Object v = m.invoke(null, kode);
            check(v != null, "varian null untuk " + kode);
            java.lang.reflect.Field ft = v.getClass().getField("template");
            String template = String.valueOf(ft.get(v));
            check(template.length() > 0, "template kosong untuk " + kode);
            File jasper = new File(webapp, "report/" + template + ".jasper");
            check(jasper.exists(), "template Jasper tidak ditemukan: " + jasper.getPath());
        }

        boolean ditolak = false;
        try {
            java.lang.reflect.Method m = NewUiLaporanKinerjaController.class
                    .getDeclaredMethod("varian", String.class);
            m.setAccessible(true);
            m.invoke(null, "jenis_karangan");
        } catch (java.lang.reflect.InvocationTargetException e) {
            ditolak = e.getCause() instanceof IllegalArgumentException;
        }
        check(ditolak, "jenis tak dikenal harus ditolak");

        System.out.println("NewUiLaporanKinerjaSelfTest OK (" + jenis.size() + " jenis)");
    }
}
