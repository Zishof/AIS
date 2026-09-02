package ais.common.newui.rab;

import org.json.JSONObject;

/** Regression test kontrak aman preflight RKAKL. */
public final class NewUiImportRkaklSelfTest {
    private NewUiImportRkaklSelfTest() { }

    public static void main(String[] args) throws Exception {
        check(NewUiImportRkaklController.aksiDikenal("meta"), "meta ditolak");
        check(NewUiImportRkaklController.aksiDikenal("list"), "list ditolak");
        check(!NewUiImportRkaklController.aksiDikenal("import"),
                "eksekusi destruktif tidak boleh dibuka");
        check("".equals(NewUiImportRkaklController.alasanNama("DIPA_2026.KEU")),
                "nama DBF aman ditolak");
        check(NewUiImportRkaklController.alasanNama("../../rahasia.KEU").length() > 0,
                "path traversal diterima");
        check(NewUiImportRkaklController.alasanNama("data-log.KEU").length() > 0,
                "berkas log diterima");
        check(NewUiImportRkaklController.alasanNama("t_cek_01.KEU").length() > 0,
                "berkas t_cek diterima");
        check(NewUiImportRkaklController.alasanNama("data.csv").length() > 0,
                "ekstensi selain KEU diterima");
        JSONObject envelope = NewUiImportRkaklController.sukses(
                new JSONObject().put("executionBlocked", true));
        check(envelope.optBoolean("success"), "amplop native gagal");
        check(envelope.getJSONObject("data").optBoolean("executionBlocked"),
                "penanda blokir eksekusi hilang");
        check(!envelope.has("ok"), "amplop scaffold lama masih dipakai");
        System.out.println("NewUiImportRkaklSelfTest OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
