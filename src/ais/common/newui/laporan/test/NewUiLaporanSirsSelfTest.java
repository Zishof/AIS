package ais.common.newui.laporan;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/** Self-test registri laporan SIRS tanpa container dan basis data. */
public final class NewUiLaporanSirsSelfTest {

    private NewUiLaporanSirsSelfTest() { }

    private static void check(boolean benar, String pesan) {
        if (!benar) throw new IllegalStateException(pesan);
    }

    public static void main(String[] args) throws Exception {
        String webapp = args.length > 0 ? args[0] : "src/main/webapp";
        Map<String, String> laporan = new LinkedHashMap<String, String>();
        laporan.put("data_pasien_rawat_inap", "sirs/data_pasien_rawat_inap");
        laporan.put("ranap_laporan_perruangan", "sirs/ranap_laporan_perruangan");
        laporan.put("informasi_tagihan", "sirs/informasi_tagihan");
        laporan.put("informasi_biaya_dan_retur", "sirs/informasi_biaya_dan_retur");
        laporan.put("struk_pembayaran", "sirs/struk_pembayaran");
        laporan.put("tracer_pasien", "sirs/tracer_pasien");
        laporan.put("rajal_tahunan_5", "sirs/rajal_laporan_kunjungan_pasien_tahunan");
        laporan.put("rajal_per_dokter", "sirs/laporan_kunjungan_pasien_rawat_jalan_per_dokter");
        laporan.put("rajal_periode", "sirs/laporan_kunjungan_pasien_rawat_jalan");
        laporan.put("rajal_tahunan_21", "sirs/rajal_laporan_kunjungan_pasien_tahunan_21");
        laporan.put("rajal_umum_21", "sirs/rajal_laporan_kunjungan_pasien_umum_21");
        laporan.put("rajal_umum_5", "sirs/rajal_laporan_kunjungan_pasien_umum_5");
        laporan.put("rajal_poli_baru_lama", "sirs/laporan_kunjungan_pasien_baru_lama");
        laporan.put("laporan_kasir_harian", "sirs/laporan_kasir_harian");
        laporan.put("laporan_kasir_per_shift", "sirs/laporan_kasir_per_shift");
        laporan.put("laporan_ranap_pasien_dinas", "sirs/laporan_ranap_pasien_dinas");
        laporan.put("ranap_laporan_perruangan_periode", "sirs/ranap_laporan_perruangan_periode");
        laporan.put("inventory_harga_beli", "sirs/daftar_harga_beli");
        laporan.put("inventory_harga_jual", "sirs/daftar_harga_jual_item");
        laporan.put("inventory_hpp", "sirs/hpp");
        laporan.put("inventory_stok", "sirs/laporan_stok");

        Method jenis = NewUiLaporanSirsController.class.getDeclaredMethod("jenis", String.class);
        jenis.setAccessible(true);
        Field template = null;
        Field saringan = null;
        Field format = null;
        Object lima = null;
        Object poliBaruLama = null;
        Object hargaBeli = null;
        for (Map.Entry<String, String> e : laporan.entrySet()) {
            check(NewUiLaporanSirsController.jenisDikenal(e.getKey()),
                    "laporan tidak dikenal: " + e.getKey());
            Object nilai = jenis.invoke(null, e.getKey());
            if (template == null) {
                template = nilai.getClass().getDeclaredField("template");
                template.setAccessible(true);
                saringan = nilai.getClass().getDeclaredField("saringan");
                saringan.setAccessible(true);
                format = nilai.getClass().getDeclaredField("format");
                format.setAccessible(true);
            }
            check(e.getValue().equals(template.get(nilai)),
                    "template keliru untuk " + e.getKey() + ": " + template.get(nilai));
            File jasper = new File(webapp, "report/" + e.getValue() + ".jasper");
            check(jasper.exists(), "template Jasper tidak ada: " + jasper.getPath());
            check(((String[]) saringan.get(nilai)).length > 0,
                    "laporan tanpa filter: " + e.getKey());
            boolean xls = e.getKey().startsWith("laporan_kasir_")
                    || e.getKey().startsWith("inventory_");
            check((xls ? "xls" : "pdf").equals(format.get(nilai)),
                    "format keluaran keliru untuk " + e.getKey() + ": " + format.get(nilai));
            if ("rajal_umum_5".equals(e.getKey())) lima = nilai;
            if ("rajal_poli_baru_lama".equals(e.getKey())) poliBaruLama = nilai;
            if ("inventory_harga_beli".equals(e.getKey())) hargaBeli = nilai;
        }
        check(!NewUiLaporanSirsController.jenisDikenal("laporan_karangan"),
                "laporan tak terdaftar harus ditolak");

        Method filter = NewUiLaporanSirsController.class.getDeclaredMethod(
                "filter", lima.getClass(), String.class);
        filter.setAccessible(true);
        JSONObject poli = (JSONObject) filter.invoke(null, lima, "poli");
        check("relasi_banyak".equals(poli.getString("tipe")),
                "poli Rajal harus memakai pemilih banyak");
        check(poli.getInt("maksimal") == 5, "Rajal lima-poli harus membatasi lima pilihan");
        JSONArray indeks = poli.getJSONArray("indeksBawaan");
        check(indeks.length() == 5 && indeks.getInt(2) == 3,
                "pilihan bawaan lima-poli harus sama dengan indeks layar ZK");
        JSONObject jenisPasien = (JSONObject) filter.invoke(null, poliBaruLama, "jenis_pasien");
        check(!jenisPasien.getBoolean("wajib") && !jenisPasien.getBoolean("pilihPertama"),
                "jenis pasien pada laporan baru/lama harus tetap opsional seperti layar ZK");
        JSONObject penyedia = (JSONObject) filter.invoke(null, hargaBeli, "penyedia");
        check("relasi_banyak".equals(penyedia.getString("tipe"))
                        && penyedia.getInt("maksimal") == 8
                        && penyedia.getJSONArray("indeksBawaan").length() == 8,
                "harga beli harus mempertahankan delapan pilihan supplier layar ZK");

        System.out.println("NewUiLaporanSirsSelfTest OK (" + laporan.size() + " laporan)");
    }
}
