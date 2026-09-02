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
        check("library/tracking_stok_item".equals(
                        NewUiLaporanUmumController.templateUntuk("library_tracking_stok_item")),
                "Tracking Stok Item harus memakai template perpustakaan, bukan template SIRS");

        Object tracking = peta.get("library_tracking_stok_item");
        Field trackingFilterField = tracking.getClass().getDeclaredField("filter");
        trackingFilterField.setAccessible(true);
        boolean banyakItem = false;
        boolean tergantungPerpustakaan = false;
        for (Object f : (List<Object>) trackingFilterField.get(tracking)) {
            Field namaField = f.getClass().getDeclaredField("nama");
            namaField.setAccessible(true);
            if (!"items".equals(namaField.get(f))) continue;
            Field tipeField = f.getClass().getDeclaredField("tipe");
            tipeField.setAccessible(true);
            banyakItem = "relasi_banyak".equals(tipeField.get(f));
            Field tergantungField = f.getClass().getDeclaredField("tergantungPada");
            tergantungField.setAccessible(true);
            tergantungPerpustakaan = "perpustakaan".equals(tergantungField.get(f));
        }
        Field mundurField = tracking.getClass().getDeclaredField("mulaiBulanMundur");
        mundurField.setAccessible(true);
        check(banyakItem, "Tracking Stok Item harus menerima pilihan banyak item");
        check(tergantungPerpustakaan,
                "Pilihan item Tracking Stok harus mengikuti perpustakaan terpilih");
        check(((Number) mundurField.get(tracking)).intValue() == 3,
                "Periode awal Tracking Stok Item harus tiga bulan ke belakang");

        // TipePenelitianDanPengabdian berbeda dari relasi lazim: labelnya ada
        // pada properti `isi`, bukan `nama`. Default ketika tidak dipilih juga
        // harus sama persis dengan generateParameter() layar ZK.
        Object penelitian = peta.get("penelitian_rekap_penelitian");
        Field filterField = penelitian.getClass().getDeclaredField("filter");
        filterField.setAccessible(true);
        boolean tipeBenar = false;
        for (Object f : (List<Object>) filterField.get(penelitian)) {
            Field namaField = f.getClass().getDeclaredField("nama");
            namaField.setAccessible(true);
            if (!"tipePenelitianDanPengabdian".equals(namaField.get(f))) continue;
            Field propertiField = f.getClass().getDeclaredField("propertiNama");
            propertiField.setAccessible(true);
            Field bawaanField = f.getClass().getDeclaredField("paramNamaBawaan");
            bawaanField.setAccessible(true);
            tipeBenar = "isi".equals(propertiField.get(f))
                    && "Penelitian dan Pengabdian".equals(bawaanField.get(f));
        }
        check(tipeBenar, "filter tipe penelitian harus memakai properti isi dan default legacy");

        System.out.println("NewUiLaporanUmumSelfTest OK (" + kunci.length
                + " laporan, " + relasi + " filter relasi)");
    }
}
