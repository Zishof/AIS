package ais.common.newui.laporan;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/** Self-test parameter tanggal Laporan Keuangan tanpa container atau database. */
public final class NewUiLaporanAkuntingSaldoBulanSelfTest {

    private NewUiLaporanAkuntingSaldoBulanSelfTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) throws Exception {
        String webapp = args.length > 0 ? args[0] : "src/main/webapp";
        File template = new File(webapp, "report/"
                + NewUiLaporanAkuntingSaldoBulanController.TEMPLATE + ".jasper");
        check(template.exists(), "template Jasper Laporan Keuangan tidak ditemukan: " + template);

        JSONArray baku = NewUiLaporanAkuntingSaldoBulanController.filterUntukMode(false);
        JSONArray kustom = NewUiLaporanAkuntingSaldoBulanController.filterUntukMode(true);
        check(baku.length() == 7, "mode template baku harus mempunyai tujuh filter");
        check(kustom.length() == 4, "mode JRXML kustom harus mempunyai empat filter");
        check(filter(kustom, "jenis_laporan").getBoolean("wajib"),
                "Jenis Laporan wajib pada mode JRXML kustom");
        check(!filter(baku, "jenis_laporan").getBoolean("wajib"),
                "Jenis Laporan tetap opsional pada mode template baku");
        check(filter(kustom, "grup") == null && filter(kustom, "kelompok") == null,
                "mode JRXML kustom tidak boleh menampilkan grup/kelompok tersembunyi");
        JSONObject grup = filter(baku, "grup");
        check(grup != null && !grup.getBoolean("dependensiWajib")
                        && grup.getJSONArray("tergantungPada").length() == 2,
                "Grup mode baku harus dimuat ulang oleh Jenis dan Tipe Laporan");

        Date mulai = tanggal(2026, Calendar.MARCH, 1);
        Date sampai = tanggal(2026, Calendar.MARCH, 31);
        Map parameters = new HashMap();
        NewUiLaporanAkuntingSaldoBulanController.isiParameterTanggal(parameters, mulai, sampai);
        check(tanggal(2025, Calendar.MARCH, 1).equals(parameters.get("tanggal0")),
                "tanggal0 harus satu tahun sebelum tanggal mulai");
        check(tanggal(2027, Calendar.MARCH, 31).equals(parameters.get("tanggal3")),
                "tanggal3 harus satu tahun setelah tanggal sampai");
        check(mulai.equals(parameters.get("tanggal1")) && sampai.equals(parameters.get("tanggal2")),
                "tanggal1/tanggal2 harus mempertahankan rentang pilihan");
        check(tanggal(2026, Calendar.FEBRUARY, 28).equals(parameters.get("tanggal1_1")),
                "tanggal1_1 harus sehari sebelum tanggal mulai");
        check(tanggal(2026, Calendar.MARCH, 30).equals(parameters.get("tanggal2_1")),
                "tanggal2_1 harus sehari sebelum tanggal sampai");
        check("2026-02-28".equals(parameters.get("tanggalSaldoAwal")),
                "tanggalSaldoAwal harus memakai format database");
        check(parameters.get("tanggalSaldoAwalType").equals(parameters.get("tanggal1_1")),
                "tanggalSaldoAwalType harus sama dengan tanggal1_1");
        System.out.println("NewUiLaporanAkuntingSaldoBulanSelfTest OK");
    }

    private static Date tanggal(int tahun, int bulan, int hari) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(tahun, bulan, hari, 0, 0, 0);
        return c.getTime();
    }

    private static JSONObject filter(JSONArray daftar, String nama) throws Exception {
        for (int i = 0; i < daftar.length(); i++) {
            JSONObject item = daftar.getJSONObject(i);
            if (nama.equals(item.getString("nama"))) return item;
        }
        return null;
    }
}
