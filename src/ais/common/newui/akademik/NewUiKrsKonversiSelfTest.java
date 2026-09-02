package ais.common.newui.akademik;

import org.json.JSONObject;

import ais.database.model.Detailperkuliahan;

/** Regression test aturan khusus KRS konversi. */
public final class NewUiKrsKonversiSelfTest {
    private NewUiKrsKonversiSelfTest() { }

    public static void main(String[] args) {
        check(NewUiKrsKonversiController.aksiDikenal("meta"), "meta ditolak");
        check(NewUiKrsKonversiController.aksiDikenal("list"), "list ditolak");
        check(NewUiKrsKonversiController.mengubah("update"), "update harus menulis");
        check(NewUiKrsKonversiController.mengubah("delete"), "delete harus menulis");
        check(!NewUiKrsKonversiController.aksiDikenal("create"), "create setengah jadi diterima");
        try {
            JSONObject envelope = NewUiKrsKonversiController.sukses(
                    new JSONObject().put("rows", "uji"));
            check(envelope.optBoolean("success"), "amplop Generic CRUD tidak sukses");
            check("uji".equals(envelope.getJSONObject("data").optString("rows")),
                    "payload Generic CRUD tidak berada di data");
            check(!envelope.has("ok"), "amplop lama ok masih terkirim");
        } catch (Exception e) {
            throw new AssertionError("kontrak JSON gagal diuji", e);
        }

        Detailperkuliahan kosong = new Detailperkuliahan();
        kosong.setTotalNilai(Double.valueOf(0));
        check(NewUiKrsKonversiController.bolehHapus(kosong), "nilai nol tidak dapat dihapus");
        Detailperkuliahan bernilai = new Detailperkuliahan();
        bernilai.setTotalNilai(Double.valueOf(0.01));
        check(!NewUiKrsKonversiController.bolehHapus(bernilai), "nilai 0,01 dapat dihapus");
        Detailperkuliahan tidakDiketahui = new Detailperkuliahan();
        tidakDiketahui.setTotalNilai(null);
        check(NewUiKrsKonversiController.bolehHapus(tidakDiketahui),
                "getter model menormalkan nilai null menjadi nol");

        check("4".equals(NewUiKrsKonversiController.labelSemester(4, null)),
                "semester konversi murni berubah");
        check("4 / 2 (Mengulang)".equals(NewUiKrsKonversiController.labelSemester(4, 2)),
                "label mengulang berubah");
        check("2 / 4 (Menabung)".equals(NewUiKrsKonversiController.labelSemester(2, 4)),
                "label menabung berubah");
        System.out.println("NewUiKrsKonversiSelfTest OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
