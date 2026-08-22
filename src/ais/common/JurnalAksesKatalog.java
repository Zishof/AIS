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
    public static final int SCHEMA_VERSION = 2;
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
        public final String capability;

        private Entri(long child, String kunci, String label, String capability) {
            this.child = child;
            this.kunci = kunci;
            this.label = label;
            this.capability = capability;
        }
    }

    public static final List<Entri> DAFTAR;
    private static final Map<String, Entri> MENURUT_KUNCI;
    private static final Map<String, String> ALIAS_LEGACY;

    static {
        List<Entri> daftar = new ArrayList<Entri>();
        tambah(daftar,460501,"dashboard","Dashboard Jurnal","menu.dashboard.read");
        tambah(daftar,460502,"journals","Identitas Jurnal","journal.manage");
        tambah(daftar,460503,"workflow-settings","Kebijakan & Alur Kerja","workflow.configure");
        tambah(daftar,460504,"taxonomy","Bagian, Kategori & Kosakata","taxonomy.manage");
        tambah(daftar,460505,"people","Pengguna, Peran & Undangan","people.manage");
        tambah(daftar,460506,"editor-assignments","Penugasan Editor","editor.assign");
        tambah(daftar,460507,"submissions","Semua Naskah","submission.read");
        tambah(daftar,460508,"screening","Pemeriksaan Awal","submission.screen");
        tambah(daftar,460509,"reviewers","Reviewer & Minat Keahlian","reviewer.manage");
        tambah(daftar,460510,"review-assignments","Penugasan & Putaran Review","review.assign");
        tambah(daftar,460511,"review-forms","Formulir Review","review.form.manage");
        tambah(daftar,460512,"decisions","Keputusan Editorial","decision.manage");
        tambah(daftar,460513,"discussions","Diskusi Editorial","discussion.manage");
        tambah(daftar,460514,"copyediting","Copyediting","copyedit.manage");
        tambah(daftar,460515,"production","Produksi & Proof","production.manage");
        tambah(daftar,460516,"issues","Edisi & Daftar Isi","issue.manage");
        tambah(daftar,460517,"publications","Publikasi, Galley & Versi","publication.manage");
        tambah(daftar,460518,"identifiers","DOI, URN & Deposit","identifier.manage");
        tambah(daftar,460519,"portal","Portal, Navigasi & Halaman","portal.manage");
        tambah(daftar,460520,"announcements","Pengumuman & Sorotan","announcement.manage");
        tambah(daftar,460521,"communications","Template Email & Notifikasi","communication.manage");
        tambah(daftar,460522,"subscriptions","Langganan, Institusi & IP","subscription.manage");
        tambah(daftar,460523,"payments","Pembayaran & Rekonsiliasi","payment.manage");
        tambah(daftar,460524,"statistics","Statistik & COUNTER","statistics.read");
        tambah(daftar,460525,"reports","Laporan & Ekspor","report.export");
        tambah(daftar,460526,"integrations","Integrasi & Paritas Plugin","integration.manage");
        tambah(daftar,460527,"import-ojs","Import OJS","import.manage");
        tambah(daftar,460528,"operations","Audit, Job & Kesehatan Sistem","operations.read");
        DAFTAR = Collections.unmodifiableList(daftar);
        Map<String, Entri> indeks = new LinkedHashMap<String, Entri>();
        for (Entri e : daftar) indeks.put(e.kunci, e);
        MENURUT_KUNCI = Collections.unmodifiableMap(indeks);
        Map<String,String> alias=new LinkedHashMap<String,String>();
        alias(alias,"masterJurnal","journals");alias(alias,"workflow","workflow-settings");
        alias(alias,"bagianKategori","taxonomy");alias(alias,"templateKosakata","taxonomy");
        alias(alias,"penggunaPeran","people");alias(alias,"penugasanEditor","editor-assignments");
        alias(alias,"submission","submissions");alias(alias,"reviewerKeahlian","reviewers");
        alias(alias,"prosesReview","review-assignments");alias(alias,"copyediting","copyediting");
        alias(alias,"produksiGalley","production");alias(alias,"edisiDaftarIsi","issues");
        alias(alias,"publikasi","publications");alias(alias,"identifier","identifiers");
        alias(alias,"situsNavigasi","portal");alias(alias,"pengumuman","announcements");
        alias(alias,"emailNotifikasi","communications");alias(alias,"langganan","subscriptions");
        alias(alias,"pembayaran","payments");alias(alias,"statistik","statistics");
        alias(alias,"laporan","reports");alias(alias,"pluginIntegrasi","integrations");
        alias(alias,"importOjs","import-ojs");alias(alias,"rekonsiliasiImport","import-ojs");
        alias(alias,"jobIntegrasi","operations");alias(alias,"audit","operations");alias(alias,"sistem","operations");
        ALIAS_LEGACY=Collections.unmodifiableMap(alias);
    }

    private JurnalAksesKatalog() {}

    private static void tambah(List<Entri> daftar,long child,String kunci,String label,String capability) {
        daftar.add(new Entri(child,kunci,label,capability));
    }
    private static void alias(Map<String,String> target,String legacy,String canonical){target.put(legacy,canonical);}

    public static String canonical(String kunci){
        if(kunci==null)return null;
        return MENURUT_KUNCI.containsKey(kunci)?kunci:ALIAS_LEGACY.get(kunci);
    }

    public static boolean dikenal(String kunci) {
        return canonical(kunci) != null;
    }

    public static boolean bolehMenu(String raw, String kunci) {
        kunci=canonical(kunci); if (kunci==null) return false;
        JSONObject akar = parseValid(raw);
        return booleanEksplisit(akar == null ? null : akar.optJSONObject("menu"), kunci);
    }

    public static boolean bolehCrud(String raw, String resource, String aksi) {
        resource=canonical(resource); if (resource==null || !termasuk(AKSI_CRUD, aksi)) return false;
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
