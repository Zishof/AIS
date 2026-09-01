package ais.common.newui.laporan;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Self-test registri laporan umum tanpa container maupun basis data.
 *
 * <p>Yang dijaga — dua hal yang paling mudah salah ketik dan paling mahal bila
 * lolos ke produksi:</p>
 * <ol>
 *   <li>setiap laporan menunjuk template Jasper yang BENAR-BENAR ada di
 *       {@code webapp/report}; salah ketik nama template baru ketahuan saat
 *       pengguna menekan Cetak dan hanya menghasilkan galat;</li>
 *   <li>setiap filter relasi menunjuk kelas entity yang dapat dimuat, sehingga
 *       lookup tidak gagal pada saat dipakai.</li>
 * </ol>
 *
 * <p>Argumen opsional: akar direktori webapp (bawaan: src/main/webapp).</p>
 */
public final class NewUiLaporanUmumSelfTest {

    private NewUiLaporanUmumSelfTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String webapp = args.length > 0 ? args[0] : "src/main/webapp";

        String[] kunci = NewUiLaporanUmumController.semuaLaporan();
        check(kunci.length > 0, "registri laporan kosong");

        Field registri = NewUiLaporanUmumController.class.getDeclaredField("REGISTRI");
        registri.setAccessible(true);
        java.util.Map<String, Object> peta = (java.util.Map<String, Object>) registri.get(null);

        int relasi = 0;
        for (String k : kunci) {
            String template = NewUiLaporanUmumController.templateUntuk(k);
            check(template != null && template.length() > 0, "template kosong untuk " + k);
            File jasper = new File(webapp, "report/" + template + ".jasper");
            check(jasper.exists(), "template Jasper tidak ada: " + jasper.getPath() + " (laporan " + k + ")");

            Object laporan = peta.get(k);
            Field fFilter = laporan.getClass().getDeclaredField("filter");
            fFilter.setAccessible(true);
            List<Object> filters = (List<Object>) fFilter.get(laporan);
            for (Object f : filters) {
                Field fEntity = f.getClass().getDeclaredField("entity");
                fEntity.setAccessible(true);
                Object entity = fEntity.get(f);
                if (entity == null) continue;
                relasi++;
                try {
                    Class.forName(String.valueOf(entity));
                } catch (ClassNotFoundException tidakAda) {
                    throw new IllegalStateException(
                            "entity filter tidak ditemukan: " + entity + " (laporan " + k + ")");
                }
            }
        }

        // Kunci yang tidak terdaftar harus ditolak, bukan diam-diam memakai
        // template laporan lain.
        Method handle = null;
        for (Method m : NewUiLaporanUmumController.class.getDeclaredMethods()) {
            if ("handle".equals(m.getName())) { handle = m; break; }
        }
        check(handle != null, "method handle tidak ditemukan");
        check(NewUiLaporanUmumController.templateUntuk("laporan_karangan") == null,
                "kunci tak dikenal harus mengembalikan null");
        check("penelitiandanpengabdian/Rekap_Penelitian".equals(
                        NewUiLaporanUmumController.templateUntuk("penelitian_rekap_penelitian")),
                "Rekap Penelitian/Pengabdian harus memakai template legacy yang tepat");
        check("penelitiandanpengabdian/Rekap_Artikel".equals(
                        NewUiLaporanUmumController.templateUntuk("penelitian_rekap_artikel")),
                "Rekap Publikasi/Jurnal harus tetap memakai template legacy yang tepat");

        System.out.println("NewUiLaporanUmumSelfTest OK (" + kunci.length
                + " laporan, " + relasi + " filter relasi)");
    }
}
