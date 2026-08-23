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

    public static void proses(String action, ais.database.model.Tbmuser tbmuser, JSONObject payload,
            JSONObject hasil) throws Exception {
        if ("draft_jurnal_ringkasan".equals(action)) {
            ringkasan(payload, hasil);
        } else if ("draft_jurnal_rincian".equals(action)) {
            rincian(payload, hasil);
        } else if ("draft_jurnal_posting".equals(action)) {
            jalankanPosting(tbmuser, payload, hasil, true);
        } else if ("draft_jurnal_batal_posting".equals(action)) {
            jalankanPosting(tbmuser, payload, hasil, false);
        } else {
            hasil.put("status", "91");
            hasil.put("description", "Aksi draft jurnal tidak dikenal: " + action);
        }
    }

    /**
     * Kunci hak akses modul yang mesin postingnya SUDAH tersedia lewat API; null bila modul itu
     * belum punya mesin.
     *
     * <p>Layar ZK sendiri baru menyediakan satu tombol posting massal ({@code Kas Kecil}); mesin
     * modul lain dibuka satu per satu di sisi API (kini {@code Kas Besar} menyusul), dan sisanya
     * masih menjawab "belum tersedia". Daftar ini disimpan di satu tempat supaya dasbor tidak pernah
     * menawarkan tombol yang ujungnya menolak -- klien membacanya lewat bendera {@code bisaPosting}
     * pada ringkasan.</p>
     */
    private static String modulPosting(String namaBaris) {
        if ("Kas Kecil".equals(namaBaris)) return "kas_kecil";
        if ("Kas Besar".equals(namaBaris)) return "kas_besar";
        if ("Uang Muka".equals(namaBaris)) return "uang_muka";
        if ("Pertanggungjawaban Uang Muka".equals(namaBaris)) return "pj_uang_muka";
        if ("Dana Talangan".equals(namaBaris)) return "dana_talangan";
        if ("Pertanggungjawaban Kas Besar".equals(namaBaris)) return "pj_kas_besar";
        if ("Penggantian Kas Kecil".equals(namaBaris)) return "penggantian_kas_kecil";
        // PERBAIKAN: dua baris ini dulu mengembalikan "pengajuan_transfer" dan "transitori",
        // yang BUKAN kunci menu -- EbisnisMenuKatalog.bolehAksi mengembalikan true untuk kunci
        // di luar KUNCI_CRUD, sehingga keduanya FAIL-OPEN: siapa pun yang dapat membuka Draft
        // Jurnal boleh memposting maupun membatalkan jurnal pergerakan dana, tanpa gerbang per
        // peran. Kini menunjuk kunci menu modulnya sendiri, sama seperti baris lain di sini.
        if ("Jurnal Pengajuan Transfer".equals(namaBaris)) return "proses_transfer";
        if ("Transitori".equals(namaBaris)) return "proses_transitori";
        // Kunci modulnya sendiri: layar POS "Bayar Pajak (PPh/PPN)" bekerja pada entitas
        // akunting.Pajak yang SAMA dengan yang diposting di sini, dan pengadaan_pajak ada
        // di KUNCI_CRUD sehingga hak "create"-nya benar-benar dapat dibatasi admin.
        if ("Pajak".equals(namaBaris)) return "pengadaan_pajak";
        if ("Penerimaan Tagihan Vendor".equals(namaBaris)) return "pengadaan_tagihan";
        if ("Pekerjaan Vendor".equals(namaBaris)) return "pengadaan_tagihan";
        // Rantai DP vendor. Kuncinya mengikuti DOKUMEN yang dijurnal, bukan jenis
        // jurnalnya: DP Vendor dan jurnal baliknya melekat pada pemesanan (pengadaan_po),
        // sedangkan DP Pekerjaan melekat pada tagihannya (pengadaan_tagihan). Keduanya
        // ada di KUNCI_CRUD sehingga hak "create"-nya dapat dibatasi admin per peran.
        if ("DP Vendor".equals(namaBaris)) return "pengadaan_po";
        if ("Jurnal Balik DP Pekerjaan".equals(namaBaris)) return "pengadaan_po";
        if ("DP Pekerjaan Vendor".equals(namaBaris)) return "pengadaan_tagihan";
        // DP dibayarkan pada tahap PEMESANAN, jadi haknya ikut kunci menu pemesanan.
        if ("DP Vendor".equals(namaBaris)) return "pengadaan_po";
        if ("DP Pekerjaan Vendor".equals(namaBaris)) return "pengadaan_po";
        if ("Pajak".equals(namaBaris)) return "pengadaan_pajak";
        return null;
    }

    private static boolean bolehAksi(ais.database.model.Tbmuser tbmuser, String kunciMenu, String aksi) {
        if (ais.common.Common.getApakahAdminLain(tbmuser)) return true;
        ais.database.model.Tbmrole peran = tbmuser == null ? null : tbmuser.hakAkses();
        if (peran == null) return true;
        return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(peran.getEbisnisMenu(), peran.getRoleId(),
                kunciMenu, aksi);
    }

    /**
     * Posting atau batal posting seluruh dokumen satu modul pada rentang terpilih.
     *
     * <p>Menulis jurnal adalah kewenangan yang lazim dipisah dari sekadar melihat drafnya, jadi
     * gerbangnya memakai hak "create" pada kunci MODUL-nya sendiri (mis. {@code kas_kecil}),
     * bukan pada kunci dasbor -- supaya peran yang boleh membaca dasbor tidak otomatis boleh
     * memposting isi modul yang bukan wewenangnya.</p>
     */
    private static void jalankanPosting(ais.database.model.Tbmuser tbmuser, JSONObject payload,
            JSONObject hasil, boolean posting) throws Exception {
        String nama = payload == null ? "" : payload.optString("nama", "").trim();
        if (nama.length() == 0) {
            hasil.put("status", "91");
            hasil.put("description", "Jenis jurnal wajib dipilih.");
            return;
        }
        String kunciModul = modulPosting(nama);
        if (kunciModul == null) {
            hasil.put("status", "91");
            hasil.put("description", "Posting massal \"" + nama + "\" belum tersedia dari aplikasi. "
                    + "Untuk sementara jalankan dari layar posting modul tersebut di aplikasi web.");
            return;
        }
        if (!bolehAksi(tbmuser, kunciModul, "create")) {
            hasil.put("status", "91");
            hasil.put("description", "Anda tidak memiliki hak untuk memposting jurnal " + nama
                    + ". Hubungi admin bila ini keliru.");
            return;
        }

        Date mulai = tanggal(payload, "mulai", awalBawaan());
        Date sampai = tanggal(payload, "sampai", akhirBawaan());

        // Berhenti bila memang tidak ada yang perlu dikerjakan. Bukan sekadar demi pesan yang
        // enak dibaca: PostingKasKecilAction.postingSemua MENYIMPAN satu baris PostingHistory
        // SEBELUM memeriksa ada-tidaknya dokumen, sehingga menekan tombol pada angka nol
        // meninggalkan riwayat posting kosong di basis data.
        int tersedia = hitungDokumen(nama, posting ? "draft" : "posting", mulai, sampai);
        if (tersedia == 0) {
            hasil.put("status", "91");
            hasil.put("description", posting
                    ? "Tidak ada draft \"" + nama + "\" pada periode ini yang perlu diposting."
                    : "Tidak ada jurnal \"" + nama + "\" terposting pada periode ini yang dapat dibatalkan.");
            return;
        }

        int jumlah;
        if ("Kas Kecil".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingKasKecilAction.postingSemua(mulai, sampai, tbmuser,
                            new Date())
                    : ais.action.master.akunting.PostingKasKecilAction.batalkanPostingSemua(mulai, sampai);
        } else if ("Kas Besar".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingKasBesarAction.postingSemua(mulai, sampai, tbmuser,
                            new Date())
                    : ais.action.master.akunting.PostingKasBesarAction.batalkanPostingSemua(mulai, sampai);
        } else if ("Pertanggungjawaban Uang Muka".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingPertangungjawabanAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.akunting.PostingPertangungjawabanAction.batalkanPostingSemua(mulai,
                            sampai);
        } else if ("Uang Muka".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingUangMukaAction.postingSemua(mulai, sampai, tbmuser,
                            new Date())
                    : ais.action.master.akunting.PostingUangMukaAction.batalkanPostingSemua(mulai, sampai);
        } else if ("Pertanggungjawaban Kas Besar".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingPertangungjawabanKasBesarAction.postingSemua(mulai,
                            sampai, tbmuser, new Date())
                    : ais.action.master.akunting.PostingPertangungjawabanKasBesarAction
                            .batalkanPostingSemua(mulai, sampai);
        } else if ("Penggantian Kas Kecil".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingPenggantianKasKecilAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.akunting.PostingPenggantianKasKecilAction.batalkanPostingSemua(mulai,
                            sampai);
        } else if ("Jurnal Pengajuan Transfer".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingProsesTransferAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.akunting.PostingProsesTransferAction.batalkanPostingSemua(mulai,
                            sampai);
        } else if ("Transitori".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingProsesTransitoriAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.akunting.PostingProsesTransitoriAction.batalkanPostingSemua(mulai,
                            sampai);
        } else if ("Penerimaan Tagihan Vendor".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingPengadaanAction.postingSemua(mulai, sampai, tbmuser,
                            new Date())
                    : ais.action.master.asset.PostingPengadaanAction.batalkanPostingSemua(mulai, sampai);
        } else if ("Pekerjaan Vendor".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingPemesananPekerjaanAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.asset.PostingPemesananPekerjaanAction.batalkanPostingSemua(mulai,
                            sampai);
        } else if ("Dana Talangan".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingDanaTalanganAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.akunting.PostingDanaTalanganAction.batalkanPostingSemua(mulai, sampai);
        } else if ("Pajak".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingPertangungjawabanPajakAction.postingSemua(
                        mulai, sampai, tbmuser, new Date())
                    : ais.action.master.akunting.PostingPertangungjawabanPajakAction
                        .batalkanPostingSemua(mulai, sampai);
        } else if ("DP Vendor".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingPemesananDpAction.postingSemua(mulai, sampai,
                            tbmuser, new Date())
                    : ais.action.master.asset.PostingPemesananDpAction.batalkanPostingSemua(mulai, sampai);
        } else if ("DP Pekerjaan Vendor".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingDpPemesananPekerjaanAction.postingSemua(mulai,
                            sampai, tbmuser, new Date())
                    : ais.action.master.asset.PostingDpPemesananPekerjaanAction.batalkanPostingSemua(
                            mulai, sampai);
        } else if ("Pajak".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.akunting.PostingPertangungjawabanPajakAction.postingSemua(mulai,
                            sampai, tbmuser, new Date())
                    : ais.action.master.akunting.PostingPertangungjawabanPajakAction
                            .batalkanPostingSemua(mulai, sampai);
        } else if ("DP Vendor".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingPemesananDpAction.postingSemua(mulai, sampai,
                        tbmuser, new Date())
                    : ais.action.master.asset.PostingPemesananDpAction.batalkanPostingSemua(mulai,
                        sampai);
        } else if ("DP Pekerjaan Vendor".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingDpPemesananPekerjaanAction.postingSemua(
                        mulai, sampai, tbmuser, new Date())
                    : ais.action.master.asset.PostingDpPemesananPekerjaanAction
                        .batalkanPostingSemua(mulai, sampai);
        } else if ("Jurnal Balik DP Pekerjaan".equals(nama)) {
            jumlah = posting
                    ? ais.action.master.asset.PostingJurnalBalikDpPemesananPekerjaanAction
                        .postingSemua(mulai, sampai, tbmuser, new Date())
                    : ais.action.master.asset.PostingJurnalBalikDpPemesananPekerjaanAction
                        .batalkanPostingSemua(mulai, sampai);
        } else {
            hasil.put("status", "91");
            hasil.put("description", "Mesin posting \"" + nama + "\" belum terpasang.");
            return;
        }

        if (jumlah == 0) {
            // Ada dokumen yang memenuhi syarat, tetapi mesinnya tidak memproses satu pun. Ini
            // TIDAK dilaporkan sebagai sukses: mesin posting lama menelan kegagalan per dokumen
            // (Common.tampilErrorJikaAdmin), sehingga "berhasil, 0 dokumen" adalah kalimat yang
            // menyesatkan persis ketika ada yang perlu diperiksa.
            hasil.put("status", "91");
            hasil.put("nama", nama);
            hasil.put("jumlah", 0);
            hasil.put("description", tersedia + " dokumen \"" + nama + "\" memenuhi syarat, tetapi tidak "
                    + "satu pun berhasil diproses. Periksa Error Log server, lalu ulangi.");
            return;
        }

        // Sebagian dokumen dapat dilewati mesin: akun jurnalnya belum lengkap, atau
        // penyimpanannya gagal. Selisihnya DISEBUTKAN -- tanpa itu angkanya hanya terasa
        // kurang, dan sisa yang tidak pernah turun ke nol tampak seperti cacat hitungan.
        String sisa = "";
        if (jumlah < tersedia) {
            // Alasannya berbeda menurut arah: memposting dapat gagal karena akun jurnalnya
            // belum lengkap, sedangkan membatalkan tidak pernah gagal karena itu.
            sisa = " " + (tersedia - jumlah) + " dokumen lain dilewati: "
                    + (posting
                        ? "jurnalnya belum lengkap (akun belum diisi pada masternya) atau gagal disimpan."
                        : "pembatalannya gagal.")
                    + " Periksa Error Log.";
        }
        hasil.put("status", "00");
        hasil.put("nama", nama);
        hasil.put("jumlah", jumlah);
        hasil.put("dilewati", tersedia - jumlah);
        hasil.put("description", jumlah + " dokumen \"" + nama + "\" "
                + (posting ? "berhasil diposting." : "posting-nya dibatalkan.")
                + (posting ? "" : " Jurnal yang sudah closing tidak ikut dibatalkan.")
                + sisa);
    }

    /** Jumlah dokumen satu baris pada satu status -- dipakai penjaga sebelum menjalankan mesin. */
    private static int hitungDokumen(String nama, String status, Date mulai, Date sampai) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return DraftJurnalRingkasanUtil.hitungStatus(session, nama, status, mulai, sampai);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
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
                // Bendera kemampuan: klien hanya menawarkan tombol yang benar-benar ada mesinnya,
                // sehingga tidak ada tombol yang ujungnya menolak.
                j.put("bisaRincian", DraftJurnalRingkasanUtil.punyaRincian(b.getNama()));
                j.put("bisaPosting", modulPosting(b.getNama()) != null);
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
