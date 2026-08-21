package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.util.DraftJurnalRingkasanUtil;
import ais.database.hibernate.HibernateUtil;

/**
 * API dasbor <b>Draft Jurnal</b> untuk POS Desktop/Android: berapa dokumen per jenis jurnal yang
 * masih draft, sudah terposting, dan sudah terkunci closing pada satu rentang tanggal.
 *
 * <p>Angkanya dihitung {@link DraftJurnalRingkasanUtil} -- port persis dari penghitung layar ZK
 * {@code draft_jurnal.zul} (lihat catatan peralihan pada util tsb). Aturan mainnya sengaja begitu: dasbor ini dipakai untuk memutuskan
 * pekerjaan mana yang harus diselesaikan lebih dulu, jadi dua kanal yang menjawab beda untuk
 * pertanyaan "berapa yang belum diposting" lebih buruk daripada tidak ada dasbor sama sekali.</p>
 *
 * <p>Rentang bawaannya menyalin layar ZK: enam bulan ke belakang sampai besok, supaya jurnal yang
 * dicatat hari ini (dan yang bertanggal maju satu hari karena zona waktu) tetap terlihat.</p>
 */
public final class DraftJurnalApiHelper {

    private DraftJurnalApiHelper() {
    }

    /** Kontrak tanggal API POS: yyyy-MM-dd. */
    private static SimpleDateFormat iso() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    public static void proses(String action, JSONObject payload, JSONObject hasil) throws Exception {
        if ("draft_jurnal_ringkasan".equals(action)) {
            ringkasan(payload, hasil);
        } else if ("draft_jurnal_rincian".equals(action)) {
            rincian(payload, hasil);
        } else {
            hasil.put("status", "91");
            hasil.put("description", "Aksi draft jurnal tidak dikenal: " + action);
        }
    }

    private static Date awalBawaan() {
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 6);
        return c.getTime();
    }

    private static Date akhirBawaan() {
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
        return c.getTime();
    }

    private static Date tanggal(JSONObject payload, String kunci, Date bawaan) {
        String teks = payload == null ? "" : payload.optString(kunci, "").trim();
        if (teks.length() == 0) return bawaan;
        try {
            return iso().parse(teks);
        } catch (Exception e) {
            // Tanggal yang tidak terbaca dikembalikan ke bawaan, BUKAN membuat seluruh dasbor gagal:
            // rentangnya ikut dikirim balik di respons sehingga klien tahu periode yang benar-benar dipakai.
            ais.common.ErrorAuditUtil.record(e, "auto-audit DraftJurnalApiHelper.tanggal " + kunci);
            return bawaan;
        }
    }

    private static void ringkasan(JSONObject payload, JSONObject hasil) throws Exception {
        Date mulai = tanggal(payload, "mulai", awalBawaan());
        Date sampai = tanggal(payload, "sampai", akhirBawaan());

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            List<DraftJurnalRingkasanUtil.Baris> baris = DraftJurnalRingkasanUtil.hitungSemua(session, mulai,
                    sampai);
            JSONArray data = new JSONArray();
            int totalDraft = 0;
            int totalPosting = 0;
            int totalClosing = 0;
            for (int i = 0; i < baris.size(); i++) {
                DraftJurnalRingkasanUtil.Baris b = baris.get(i);
                JSONObject j = new JSONObject();
                j.put("kunci", b.getKunci());
                j.put("nama", b.getNama());
                j.put("keterangan", b.getKeterangan());
                j.put("draft", b.getDraft());
                j.put("posting", b.getPosting());
                j.put("closing", b.getClosing());
                data.put(j);
                totalDraft += b.getDraft();
                totalPosting += b.getPosting();
                totalClosing += b.getClosing();
            }
            hasil.put("status", "00");
            hasil.put("mulai", iso().format(mulai));
            hasil.put("sampai", iso().format(sampai));
            hasil.put("draft", totalDraft);
            hasil.put("posting", totalPosting);
            hasil.put("closing", totalClosing);
            hasil.put("total", totalDraft + totalPosting + totalClosing);
            hasil.put("data", data);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /**
     * Daftar dokumen di balik satu angka dasbor -- inilah yang dibuka ketika angka Draft,
     * Terposting, atau Closing diketuk.
     *
     * <p>Daftarnya dibangun dari KRITERIA YANG SAMA dengan angkanya (lihat
     * {@code DraftJurnalRingkasanUtil.kriteriaDokumen}), jadi jumlah baris yang tampil tidak
     * mungkin berselisih dengan angka yang barusan diketuk pengguna.</p>
     */
    private static void rincian(JSONObject payload, JSONObject hasil) throws Exception {
        String nama = payload == null ? "" : payload.optString("nama", "").trim();
        if (nama.length() == 0) {
            hasil.put("status", "91");
            hasil.put("description", "Jenis jurnal wajib dipilih.");
            return;
        }
        String status = payload.optString("status", "draft").trim();
        if (!"draft".equals(status) && !"posting".equals(status) && !"closing".equals(status)) {
            hasil.put("status", "91");
            hasil.put("description", "Status rincian hanya boleh draft, posting, atau closing.");
            return;
        }
        if (!DraftJurnalRingkasanUtil.punyaRincian(nama)) {
            hasil.put("status", "91");
            hasil.put("description", "\"" + nama + "\" diposting per periode, bukan per dokumen, "
                    + "sehingga tidak memiliki daftar dokumen yang dapat dirinci.");
            return;
        }

        Date mulai = tanggal(payload, "mulai", awalBawaan());
        Date sampai = tanggal(payload, "sampai", akhirBawaan());
        int batas = payload.optInt("limit", 100);

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            List<DraftJurnalRingkasanUtil.Dokumen> dokumen = DraftJurnalRingkasanUtil.rincian(session, nama,
                    status, mulai, sampai, batas);
            JSONArray data = new JSONArray();
            for (int i = 0; i < dokumen.size(); i++) {
                DraftJurnalRingkasanUtil.Dokumen d = dokumen.get(i);
                JSONObject j = new JSONObject();
                j.put("id", d.getId());
                j.put("tanggal", d.getTanggal());
                j.put("uraian", d.getUraian());
                j.put("nilai", d.getNilai());
                data.put(j);
            }
            hasil.put("status", "00");
            hasil.put("nama", nama);
            hasil.put("statusRincian", status);
            hasil.put("mulai", iso().format(mulai));
            hasil.put("sampai", iso().format(sampai));
            hasil.put("jumlah", dokumen.size());
            hasil.put("data", data);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }
}
