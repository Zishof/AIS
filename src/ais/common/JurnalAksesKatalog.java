package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * Katalog tunggal hak akses modul jurnal ilmiah.
 *
 * <p>Kontrak selalu fail-closed: JSON kosong/rusak, versi yang tidak didukung,
 * key yang tidak dikenal, atau nilai non-boolean tidak pernah menghasilkan
 * grant. Seluruh UI, servlet, dan service harus memanggil kelas ini dan tidak
 * menafsirkan {@code Tbmrole.jurnalAksesJson} sendiri.</p>
 */
public final class JurnalAksesKatalog {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_JSON_CHARS = 131072;

    public static final String[] AKSI_CRUD = {"read", "create", "update", "delete", "approve", "reject"};
    public static final String[] AKSI_WORKFLOW = {
        "assignEditor", "assignReviewer", "viewReviewerIdentity", "makeFinalDecision",
        "publish", "retract", "manageImport", "retryJob", "viewAudit",
        "manageSubscription", "managePayment", "manageIdentifier"
    };

    public static final class Entri {
        public final long child;
        public final String kunci;
        public final String label;

        private Entri(long child, String kunci, String label) {
            this.child = child;
            this.kunci = kunci;
            this.label = label;
        }
    }

    public static final List<Entri> DAFTAR;
    private static final Map<String, Entri> MENURUT_KUNCI;

    static {
        List<Entri> daftar = new ArrayList<Entri>();
        tambah(daftar, 460501, "dashboard", "Dashboard Jurnal");
        tambah(daftar, 460502, "masterJurnal", "Master Jurnal");
        tambah(daftar, 460503, "bagianKategori", "Bagian dan Kategori");
        tambah(daftar, 460504, "edisiDaftarIsi", "Edisi dan Daftar Isi");
        tambah(daftar, 460505, "submission", "Naskah dan Submission");
        tambah(daftar, 460506, "penugasanEditor", "Penugasan Editor");
        tambah(daftar, 460507, "reviewerKeahlian", "Reviewer dan Keahlian");
        tambah(daftar, 460508, "prosesReview", "Form dan Proses Review");
        tambah(daftar, 460509, "copyediting", "Copyediting");
        tambah(daftar, 460510, "produksiGalley", "Produksi, Proof, dan Galley");
        tambah(daftar, 460511, "publikasi", "Artikel dan Versi Publikasi");
        tambah(daftar, 460512, "identifier", "DOI, URN, dan Identifier");
        tambah(daftar, 460513, "penggunaPeran", "Pengguna, Peran, dan Undangan");
        tambah(daftar, 460514, "pengumuman", "Pengumuman dan Sorotan");
        tambah(daftar, 460515, "situsNavigasi", "Situs, Halaman, dan Navigasi");
        tambah(daftar, 460516, "emailNotifikasi", "Email dan Notifikasi");
        tambah(daftar, 460517, "langganan", "Langganan dan Hak Akses");
        tambah(daftar, 460518, "pembayaran", "Pembayaran Jurnal");
        tambah(daftar, 460519, "statistik", "Statistik dan COUNTER");
        tambah(daftar, 460520, "pluginIntegrasi", "Plugin dan Integrasi");
        tambah(daftar, 460521, "importOjs", "Import dari OJS");
        tambah(daftar, 460522, "rekonsiliasiImport", "Pemetaan dan Rekonsiliasi Import");
        tambah(daftar, 460523, "laporan", "Laporan Jurnal");
        tambah(daftar, 460524, "workflow", "Pengaturan Workflow");
        tambah(daftar, 460525, "templateKosakata", "Template dan Kosakata");
        tambah(daftar, 460526, "jobIntegrasi", "Job, Antrian, dan Integrasi Gagal");
        tambah(daftar, 460527, "audit", "Audit Trail Jurnal");
        tambah(daftar, 460528, "sistem", "Pengaturan Sistem Jurnal");
        DAFTAR = Collections.unmodifiableList(daftar);
        Map<String, Entri> indeks = new LinkedHashMap<String, Entri>();
        for (Entri e : daftar) indeks.put(e.kunci, e);
        MENURUT_KUNCI = Collections.unmodifiableMap(indeks);
    }

    private JurnalAksesKatalog() {}

    private static void tambah(List<Entri> daftar, long child, String kunci, String label) {
        daftar.add(new Entri(child, kunci, label));
    }

    public static boolean dikenal(String kunci) {
        return kunci != null && MENURUT_KUNCI.containsKey(kunci);
    }

    public static boolean bolehMenu(String raw, String kunci) {
        if (!dikenal(kunci)) return false;
        JSONObject akar = parseValid(raw);
        return booleanEksplisit(akar == null ? null : akar.optJSONObject("menu"), kunci);
    }

    public static boolean bolehCrud(String raw, String resource, String aksi) {
        if (!dikenal(resource) || !termasuk(AKSI_CRUD, aksi)) return false;
        JSONObject akar = parseValid(raw);
        JSONObject crud = akar == null ? null : akar.optJSONObject("crud");
        JSONObject perResource = crud == null ? null : crud.optJSONObject(resource);
        return booleanEksplisit(perResource, aksi);
    }

    public static boolean bolehWorkflow(String raw, String aksi) {
        if (!termasuk(AKSI_WORKFLOW, aksi)) return false;
        JSONObject akar = parseValid(raw);
        return booleanEksplisit(akar == null ? null : akar.optJSONObject("workflow"), aksi);
    }

    /** Menghasilkan struktur lengkap default-deny untuk role baru atau JSON invalid. */
    public static JSONObject modelUntukEditor(String raw) {
        try {
            JSONObject valid = parseValid(raw);
            JSONObject hasil = valid == null ? new JSONObject() : new JSONObject(valid.toString());
            hasil.put("schemaVersion", SCHEMA_VERSION);
            JSONObject menu = hasil.optJSONObject("menu");
            if (menu == null) menu = new JSONObject();
            JSONObject crud = hasil.optJSONObject("crud");
            if (crud == null) crud = new JSONObject();
            for (Entri e : DAFTAR) {
                if (!booleanEksplisit(menu, e.kunci)) menu.put(e.kunci, false);
                JSONObject aksi = crud.optJSONObject(e.kunci);
                if (aksi == null) aksi = new JSONObject();
                for (String nama : AKSI_CRUD) if (!booleanEksplisit(aksi, nama)) aksi.put(nama, false);
                crud.put(e.kunci, aksi);
            }
            JSONObject workflow = hasil.optJSONObject("workflow");
            if (workflow == null) workflow = new JSONObject();
            for (String nama : AKSI_WORKFLOW) if (!booleanEksplisit(workflow, nama)) workflow.put(nama, false);
            hasil.put("menu", menu);
            hasil.put("crud", crud);
            hasil.put("workflow", workflow);
            return hasil;
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membentuk model hak akses jurnal", e);
        }
    }

    /** Validasi ketat untuk payload yang akan disimpan. */
    public static JSONObject validasiUntukSimpan(String raw) {
        JSONObject akar = parseValid(raw);
        if (akar == null) throw new IllegalArgumentException("Konfigurasi hak akses jurnal tidak valid");
        validasiBooleanMap(akar.optJSONObject("menu"), MENURUT_KUNCI.keySet().toArray(new String[MENURUT_KUNCI.size()]));
        JSONObject crud = akar.optJSONObject("crud");
        if (crud == null) throw new IllegalArgumentException("Bagian crud wajib tersedia");
        for (Entri e : DAFTAR) validasiBooleanMap(crud.optJSONObject(e.kunci), AKSI_CRUD);
        validasiBooleanMap(akar.optJSONObject("workflow"), AKSI_WORKFLOW);
        return akar;
    }

    private static JSONObject parseValid(String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.length() > MAX_JSON_CHARS) return null;
        try {
            JSONObject akar = new JSONObject(raw);
            Object versi = akar.opt("schemaVersion");
            if (!(versi instanceof Number) || ((Number) versi).intValue() != SCHEMA_VERSION) return null;
            if (akar.optJSONObject("menu") == null || akar.optJSONObject("crud") == null
                    || akar.optJSONObject("workflow") == null) return null;
            return akar;
        } catch (Exception e) {
            return null;
        }
    }

    private static void validasiBooleanMap(JSONObject obj, String[] keys) {
        if (obj == null) throw new IllegalArgumentException("Bagian hak akses wajib tersedia");
        for (String key : keys) {
            if (!obj.has(key) || !(obj.opt(key) instanceof Boolean))
                throw new IllegalArgumentException("Nilai hak akses wajib boolean: " + key);
        }
    }

    private static boolean booleanEksplisit(JSONObject obj, String key) {
        return obj != null && obj.has(key) && obj.opt(key) instanceof Boolean
                && ((Boolean) obj.opt(key)).booleanValue();
    }

    private static boolean termasuk(String[] daftar, String nilai) {
        if (nilai == null) return false;
        for (String item : daftar) if (item.equals(nilai)) return true;
        return false;
    }
}
