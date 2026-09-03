package ais.action.master.koperasi.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Backend bersama untuk modul "Laporan-Laporan e-Kantin".
 *
 * Kelas ini memusatkan SELURUH logika kueri (SQL native skema "koperasi") + metadata kolom
 * untuk ~30 laporan kantin/koperasi, sehingga dipakai bersama oleh DUA penyaji:
 *   1. laporan_laporan_service.jsp  -> menghasilkan JSON (tampilan HTML di layar).
 *   2. ais.action.servlet.LaporanKantinPdf -> menghasilkan PDF (iText) dengan penomoran
 *      halaman di SISI PELADEN (tidak bergantung pada skala cetak peramban).
 *
 * Dengan memusatkan kueri di sini, kedua penyaji selalu memakai data, filter, dan aturan
 * keamanan yang IDENTIK. Lingkup toko DIPAKSA: bila pengguna login sebagai pedagang, hanya
 * tokonya yang dapat dilihat (parameter tokoId dari klien diabaikan).
 *
 * Aturan sesi: memakai HibernateUtil.currentSession() (dikelola kerangka kerja) sehingga
 * TIDAK ditutup manual di sini. Read-only.
 *
 * Penamaan kolom DB: kolom tanpa anotasi @Column dipetakan huruf kecil TANPA garis bawah
 * (hargasatuan, hargajual, hargabelisatuan, waktupengadaan, stoksistem, dst.), sedangkan kolom
 * ber-@Column memakai snake_case (total_biaya, bayar_tunai, tanggal_pembayaran, kode_identitas).
 * Tipe kolom keluaran: "num" (angka), "tgl" (tanggal), "text" (teks).
 *
 * Hasil#baris memuat nilai mentah per kolom sesuai Hasil#tipe:
 *   - "num" -> java.lang.Double
 *   - "tgl" -> java.util.Date (boleh null)
 *   - selain itu -> java.lang.String
 * Pemformatan (ribuan, desimal, format tanggal) diserahkan ke masing-masing penyaji.
 *
 * Java 1.7 kompatibel.
 */
public final class LaporanKantinUtil {

    private LaporanKantinUtil() {
    }

    /** Ekspresi omzet per baris pembelian (alias p=pembelian, pr=produk). */
    /**
     * Identitas kasir pada nota ({@code koperasi.pembelian_anggota_koperasi}, alias {@code h}).
     *
     * <p>Sebelumnya laporan kasir memakai {@code h.oleh}. Kolom itu METADATA AUDIT -- berisi
     * siapa yang terakhir menulis baris, dan pada jalur sinkronisasi dapat berisi penanda
     * sistem seperti {@code external_update} -- sehingga laporan bisa mengelompokkan penjualan
     * ke "kasir" yang tidak pernah melayani transaksinya. Snapshot kasir pada nota adalah
     * satu-satunya sumber identitas, sama seperti yang dipakai laporan di aplikasi kasir
     * ({@code PosApi.daftarOrderDenganSesi}).</p>
     */
    static final String KASIR_NOTA =
        "coalesce(nullif(trim(h.kasir_login_nama),''),'Kasir tidak tercatat')";

    /**
     * Label produk pada baris item: memakai master bila ada, dan JATUH ke nama/kode snapshot
     * yang tersimpan di baris penjualan bila produknya sudah dihapus. Dengan {@code left join},
     * penjualan produk terhapus tetap muncul; dengan {@code join} biasa baris itu hilang dari
     * laporan tanpa jejak sehingga total laporan lebih kecil daripada penjualan sebenarnya.
     */
    static final String LABEL_PRODUK_ITEM =
        "(coalesce(nullif(trim(pr.kode),''), nullif(trim(p.kode),''), '') || ' - '"
        + " || coalesce(nullif(trim(pr.nama),''), nullif(trim(p.nama),''), 'Produk tanpa nama'))";

    /**
     * Nilai penjualan satu baris item.
     *
     * <p>Memakai {@code p.total} — nilai FINAL yang benar-benar ditagihkan dan disimpan kasir,
     * termasuk hasil harga grosir, harga Pack, dan pembulatan. Sebelumnya laporan web menghitung
     * ulang {@code hargasatuan * qty - diskon}; untuk penjualan biasa hasilnya sama, tetapi pada
     * baris berharga grosir/Pack angka hitung-ulang itu menyimpang dari nota yang dipegang
     * pelanggan dan dari laporan di aplikasi kasir (yang memakai {@code total}).</p>
     *
     * <p>Baris lama yang belum menyimpan {@code total} tetap dihitung dengan rumus sebelumnya,
     * sehingga laporan periode lampau tidak berubah menjadi nol.</p>
     */
    static final String OMZET =
        "coalesce(p.total, (coalesce(p.hargasatuan, pr.hargajual, 0) * coalesce(p.qty,0)"
        + " - coalesce(p.diskon,0)))";

    /**
     * Satuan yang DIPILIH kasir saat menjual, mis. "1 Lusin" — kosong bila dijual per satuan
     * dasar. Laporan di aplikasi kasir menampilkan informasi ini (dok. 59, dok. 64); tanpa
     * padanannya di web, satu baris "12" pada laporan web dan "1 Lusin (12 Pcs)" pada aplikasi
     * tampak seperti dua transaksi berbeda.
     */
    static final String LABEL_SATUAN_JUAL =
        "case when p.qty_input is not null and nullif(trim(sj.nama),'') is not null"
        + " then (trim(to_char(p.qty_input,'FM999999990.###')) || ' ' || trim(sj.nama))"
        + " else '' end";

    /** Nama UOM transaksi untuk laporan agregat (tanpa angka qty per baris). */
    static final String NAMA_SATUAN_TRANSAKSI =
        "coalesce(nullif(trim(sj.nama),''), nullif(trim(sd.nama),''), '-')";

    /** Qty dalam UOM yang dipilih kasir; jatuh ke qty dasar untuk penjualan biasa. */
    static final String QTY_UOM_ITEM =
        "case when p.satuan_jual is not null and p.qty_input is not null"
        + " then p.qty_input else coalesce(p.qty,0) end";

    /** JOIN satuan jual + satuan dasar untuk baris item penjualan. */
    private static final String JOIN_SATUAN_ITEM =
        " left join koperasi.satuan_produk sj on sj.id=p.satuan_jual"
        + " left join koperasi.satuan_produk sd on sd.id=pr.satuan ";

    /** Kode produk pada baris item: master bila ada, jatuh ke snapshot baris penjualan. */
    static final String KODE_PRODUK_ITEM =
        "coalesce(nullif(trim(pr.kode),''), nullif(trim(p.kode),''), '')";

    /** Nama produk pada baris item: master bila ada, jatuh ke snapshot baris penjualan. */
    static final String NAMA_PRODUK_ITEM =
        "coalesce(nullif(trim(pr.nama),''), nullif(trim(p.nama),''), 'Produk tanpa nama')";

    /** Metadata satu kolom laporan. */
    public static final class Kolom {
        public final String label;
        public final String tipe; // num | tgl | text
        public Kolom(String label, String tipe) { this.label = label; this.tipe = tipe; }
    }

    /** Hasil pembentukan satu laporan. */
    /**
     * Batas jumlah baris satu laporan.
     *
     * <p>Seluruh baris laporan ditahan di memori sebagai {@code Object[]} lalu dirender ke
     * halaman. Tanpa batas, satu permintaan "Rincian Penjualan per Barang" untuk rentang
     * setahun pada toko ramai dapat menarik ratusan ribu baris sekaligus — membebani memori
     * server (bukan hanya laporan itu; seluruh aplikasi ikut terdampak) dan menghasilkan
     * halaman yang tidak mungkin dibaca. Batas ini memilih gagal-terkendali dengan
     * pemberitahuan, bukan gagal-total tanpa penjelasan.</p>
     */
    static final int BATAS_BARIS_LAPORAN = 20000;

    public static final class Hasil {
        public String status = "00";       // 00 ok | soon | 01 sesi habis | 99 error
        public String message = "";
        public String judul = "Laporan";
        public String catatan = "";
        public int grup = -1;              // indeks kolom utk pengelompokan + subtotal; -1 = tanpa grup
        public boolean grandTotal = true; // false utk laporan berbentuk "statement" (Laba Rugi/Neraca/Arus Kas)
        public boolean lockToko = false;
        public final List<Kolom> kolom = new ArrayList<Kolom>();
        public final List<Object[]> baris = new ArrayList<Object[]>();
        public String[] tipe = new String[0];
        /** true bila baris dipotong pada BATAS_BARIS_LAPORAN; isi laporan TIDAK lengkap. */
        public boolean terpotong = false;
    }

    /**
     * Jalankan SQL laporan lalu isi {@code H.baris} apa adanya (dipakai laporan "rincian"/drill-down
     * yang mengembalikan H sendiri, bukan lewat jalur eksekusi umum di akhir {@code build}).
     */
    private static void isiBarisDariSql(org.hibernate.Session session, Hasil H, String sql,
            Map<String, Object> prm, int jumlahKolom) {
        try {
            SQLQuery q = session.createSQLQuery(sql);
            for (Map.Entry<String, Object> e : prm.entrySet()) { q.setParameter(e.getKey(), e.getValue()); }
            List<?> rows = q.list();
            for (Object ro : rows) {
                Object[] src = (ro instanceof Object[]) ? (Object[]) ro : new Object[] { ro };
                Object[] baris = new Object[jumlahKolom];
                for (int i = 0; i < jumlahKolom; i++) { baris[i] = i < src.length ? src[i] : null; }
                H.baris.add(baris);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit LaporanKantinUtil.isiBarisDariSql");
            H.status = "99";
            H.message = "Rincian tidak dapat dimuat.";
        }
    }

    private static boolean ada(String s) { return s != null && s.trim().length() > 0; }

    /** Klausa rentang tanggal pada kolom tertentu + isi parameter ke map (idempoten). */
    /**
     * Klausa periode untuk baris ITEM PENJUALAN ({@code koperasi.pembelian}, alias {@code p})
     * SEKALIGUS menyingkirkan baris yang sudah tidak aktif.
     *
     * <p>Laporan POS di aplikasi kasir ({@code PosApi.daftarOrderDenganSesi}) menyaring
     * {@code COALESCE(a.aktif,true)=true}; laporan web sebelumnya tidak menyaring apa pun.
     * Akibatnya baris yang dinonaktifkan ikut terhitung di web, sehingga qty dan omzet untuk
     * periode yang sama bisa berbeda antara dua laporan tanpa penjelasan -- selisih seperti itu
     * merusak kepercayaan pada seluruh laporan. Dipusatkan di sini supaya setiap laporan
     * berbasis item memakai batas yang sama; jangan memanggil {@code klausaTanggal("p.waktu",
     * ...)} langsung lagi.</p>
     */
    static String klausaPeriodeItemPenjualan(String tglMulai, String tglSampai,
            Map<String, Object> p) {
        return klausaTanggal("p.waktu", tglMulai, tglSampai, p) + " and coalesce(p.aktif,true)=true ";
    }

    private static String klausaTanggal(String kolom, String tglMulai, String tglSampai, Map<String, Object> p) {
        StringBuilder sb = new StringBuilder();
        if (ada(tglMulai)) { sb.append(" AND cast(").append(kolom).append(" as date) >= cast(:tglMulai as date) "); p.put("tglMulai", tglMulai.trim()); }
        if (ada(tglSampai)) { sb.append(" AND cast(").append(kolom).append(" as date) <= cast(:tglSampai as date) "); p.put("tglSampai", tglSampai.trim()); }
        return sb.toString();
    }

    /**
     * Kondisi tanggal INLINE (bukan named-param) — dipakai pada SQL yang mengulang kolom tanggal
     * banyak kali (mis. UNION 5 slot bayar pada buku besar pembantu piutang), agar tidak bergantung
     * pada pengulangan {@code :tglMulai}/{@code :tglSampai}. Nilai tanggal berasal dari datebox
     * (format yyyy-MM-dd), tanda kutip dibuang untuk aman.
     */
    private static String kondTanggalInline(String kolom, String tglMulai, String tglSampai) {
        StringBuilder sb = new StringBuilder();
        if (ada(tglMulai)) {
            sb.append(" AND cast(").append(kolom).append(" as date) >= date '")
                    .append(tglMulai.trim().replace("'", "")).append("' ");
        }
        if (ada(tglSampai)) {
            sb.append(" AND cast(").append(kolom).append(" as date) <= date '")
                    .append(tglSampai.trim().replace("'", "")).append("' ");
        }
        return sb.toString();
    }

    /**
     * CTE {@code mutasi} piutang anggota — sumber Buku Besar Pembantu Piutang &amp; Histori Piutang.
     * DEBIT (bertambah) = belanja via metode "Masuk sebagai Hutang" (5 slot bayar), KREDIT (berkurang)
     * = {@code koperasi.pembayaran_hutang}. Pola SQL sama dengan {@code KantinHelper.mutasiHutangList}
     * (sudah dipakai di produksi), tanggal di-inline. {@code selectAkhir} = SELECT + window saldo.
     */
    private static String sqlMutasiPiutang(String condH, String condPh, String selectAkhir) {
        String[] slotJoin = { "h.cara_pembayaran_koperasi", "h.cara_pembayaran_koperasi_2",
                "h.cara_pembayaran_koperasi_3", "h.cara_pembayaran_koperasi_4", "h.cara_pembayaran_koperasi_5" };
        String[] slotNom = {
                "coalesce(h.total_biaya,0)-coalesce(h.nominal_bayar_2,0)-coalesce(h.nominal_bayar_3,0)"
                        + "-coalesce(h.nominal_bayar_4,0)-coalesce(h.nominal_bayar_5,0)",
                "coalesce(h.nominal_bayar_2,0)", "coalesce(h.nominal_bayar_3,0)",
                "coalesce(h.nominal_bayar_4,0)", "coalesce(h.nominal_bayar_5,0)" };
        StringBuilder sql = new StringBuilder("with mutasi as ( ");
        for (int slot = 1; slot <= 5; slot++) {
            if (slot > 1) {
                sql.append(" union all ");
            }
            sql.append(" select h.tanggal_pembayaran as waktu, ('H").append(slot).append("' || h.id) as baris_id, ")
                    .append("(a.kode || ' - ' || a.nama) as nama_anggota, h.anggota_koperasi as id_anggota, ")
                    .append("'Belanja (Hutang)' as jenis_mutasi, coalesce(h.kode,'') as keterangan, (")
                    .append(slotNom[slot - 1]).append(") as bertambah, 0 as berkurang ")
                    .append("from koperasi.pembelian_anggota_koperasi h ")
                    .append("join koperasi.anggota_koperasi a on h.anggota_koperasi = a.id ")
                    .append("join koperasi.cara_pembayaran_koperasi cpk").append(slot).append(" on ")
                    .append(slotJoin[slot - 1]).append(" = cpk").append(slot).append(".id ")
                    .append("where cpk").append(slot).append(".masuk_sebagai_hutang = true ")
                    .append("and h.anggota_koperasi is not null and (").append(slotNom[slot - 1]).append(") > 0 ")
                    .append(condH);
        }
        sql.append(" union all select ph.waktu, ('C' || ph.id), (a.kode || ' - ' || a.nama), ph.anggota_koperasi, ")
                .append("'Pembayaran Hutang', coalesce(ph.keterangan,''), 0, coalesce(ph.nominal,0) ")
                .append("from koperasi.pembayaran_hutang ph ")
                .append("join koperasi.anggota_koperasi a on ph.anggota_koperasi = a.id where 1=1 ").append(condPh);
        sql.append(" ) ").append(selectAkhir);
        return sql.toString();
    }

    /**
     * Sumber tunggal transaksi Kasbon per slot pembayaran. Setiap baris mewakili satu slot
     * Kasbon efektif pada satu nota; Voucher, QRIS, Tunai, dan Transfer tidak dapat masuk walau
     * flag master mereka salah, karena gerbang juga mewajibkan kode/nama mengandung "kasbon".
     */
    static String sqlSumberKasbon() {
        String[] fk = { "h.cara_pembayaran_koperasi", "h.cara_pembayaran_koperasi_2",
                "h.cara_pembayaran_koperasi_3", "h.cara_pembayaran_koperasi_4",
                "h.cara_pembayaran_koperasi_5" };
        String[] nominal = { LaporanRincianTransaksiUtil.nominalSlotSatu("h"),
                "COALESCE(h.nominal_bayar_2,0)", "COALESCE(h.nominal_bayar_3,0)",
                "COALESCE(h.nominal_bayar_4,0)", "COALESCE(h.nominal_bayar_5,0)" };
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < fk.length; i++) {
            if (i > 0) { sql.append(" UNION ALL "); }
            String cara = "ck" + (i + 1);
            sql.append("SELECT h.id id_nota, h.kode kode_nota, h.tanggal_pembayaran tanggal,")
                    .append(" h.toko, h.anggota_koperasi, COALESCE(h.total_biaya,0) total_nota,")
                    .append(" ").append(LaporanRincianTransaksiUtil.labelJenisKasbon(cara))
                    .append(" jenis_piutang, ").append(nominal[i]).append(" nilai_piutang")
                    .append(" FROM koperasi.pembelian_anggota_koperasi h")
                    .append(" JOIN koperasi.cara_pembayaran_koperasi ").append(cara)
                    .append(" ON ").append(cara).append(".id=").append(fk[i])
                    .append(" WHERE ").append(LaporanRincianTransaksiUtil.syaratKasbon(cara))
                    .append(" AND h.anggota_koperasi IS NOT NULL AND ").append(nominal[i]).append(" > 0")
                    .append(" AND EXISTS (SELECT 1 FROM koperasi.pembelian ip WHERE")
                    .append(" ip.pembelian_anggota_koperasi=h.id AND COALESCE(ip.aktif,true)=true)");
        }
        return sql.toString();
    }

    /** Klausa lingkup toko (kolom FK toko) + isi parameter :tokoId (idempoten). */
    private static String kondToko(String kolom, Long tokoId, Map<String, Object> p) {
        if (tokoId == null) { return ""; }
        p.put("tokoId", tokoId);
        return " AND " + kolom + " = :tokoId ";
    }

    /**
     * Cek apakah sebuah tabel/relasi ADA di database (mis. "koperasi.pemakaian_bahan_baku" yang
     * bersifat opsional dan tidak ada di sebagian instalasi). Memakai information_schema.tables
     * (ada di semua versi PostgreSQL, termasuk 9.3) sehingga TIDAK error walau tabel tak ada
     * (mengembalikan baris kosong). Aman dipakai sebelum membangun SQL agar laporan tidak gagal.
     */
    private static boolean tabelAda(org.hibernate.Session session, String namaTabel) {
        try {
            // PENTING: JANGAN pakai to_regclass() — fungsi itu baru ada di PostgreSQL 9.4, sedangkan
            // server ini 9.3. Bila dipanggil, query GAGAL dan MERACUNI transaksi thread-bound
            // (currentSession tak ditutup) sehingga SEMUA query berikutnya ikut "could not execute
            // query". information_schema.tables ada di semua versi & tak pernah error walau tabel tiada.
            String schema = "public", tbl = namaTabel;
            int dot = namaTabel.indexOf('.');
            if (dot > 0) { schema = namaTabel.substring(0, dot); tbl = namaTabel.substring(dot + 1); }
            SQLQuery q = session.createSQLQuery(
                "select 1 from information_schema.tables where table_schema = :sch and table_name = :tbl");
            q.setParameter("sch", schema.trim().toLowerCase());
            q.setParameter("tbl", tbl.trim().toLowerCase());
            q.setMaxResults(1);
            return q.uniqueResult() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Ubah SQL ber-parameter :nama menjadi JDBC positional (?) + kumpulkan nilainya berurutan
     * sesuai kemunculan (parameter yang muncul berkali-kali di-bind berkali-kali). Memakai negative
     * lookbehind agar '::' (cast) TIDAK ikut ter-parse sebagai parameter.
     */
    private static String toPositional(String sql, Map<String, Object> prm, java.util.List<Object> outVals) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<!:):([a-zA-Z][a-zA-Z0-9_]*)").matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            outVals.add(prm.get(m.group(1)));
            m.appendReplacement(sb, "?");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Sisipkan dimensi TOKO ke query laporan (mode "Per toko"): tambah kolom nama toko (subquery
     * id-&gt;nama) di AKHIR SELECT, tambahkan id toko ke GROUP BY terluar (bila ada), lalu urutkan
     * per toko lebih dulu. Kolom Toko sengaja di AKHIR agar TIDAK menggeser posisi kolom lain
     * (referensi positional seperti "order by 3" tetap valid). Hanya untuk query berstruktur sederhana
     * (FROM/GROUP BY/ORDER BY terluar = kemunculan pertama untuk FROM, terakhir untuk GROUP/ORDER).
     *
     * @param sql       SQL asli laporan
     * @param tokoIdCol ekspresi kolom id toko pada tabel utama (mis. "p.toko", "h.toko", "pr.toko")
     */
    private static String perTokoSql(String sql, String tokoIdCol) {
        if (sql == null || tokoIdCol == null) { return sql; }
        String tokoSel = "(select ___tk.nama from koperasi.toko ___tk where ___tk.id = " + tokoIdCol + ")";
        String low = sql.toLowerCase();
        int fromIdx = low.indexOf(" from ");
        if (fromIdx < 0) { return sql; }
        // 1. kolom nama toko di AKHIR daftar SELECT (tepat sebelum " from " terluar)
        String out = sql.substring(0, fromIdx) + ", " + tokoSel + " as laporan_toko " + sql.substring(fromIdx);
        // 2. GROUP BY terluar (kemunculan TERAKHIR): tambahkan id toko
        low = out.toLowerCase();
        int gb = low.lastIndexOf(" group by ");
        if (gb >= 0) { int a = gb + " group by ".length(); out = out.substring(0, a) + tokoIdCol + ", " + out.substring(a); }
        // 3. ORDER BY terluar: dahulukan id toko (agar tiap toko berurutan); bila tak ada, tambahkan
        low = out.toLowerCase();
        int ob = low.lastIndexOf(" order by ");
        if (ob >= 0) { int a = ob + " order by ".length(); out = out.substring(0, a) + tokoIdCol + " asc, " + out.substring(a); }
        else { out = out + " order by " + tokoIdCol + " asc "; }
        return out;
    }

    /** Jalankan kueri agregat 1-nilai (native SQL) -> double (0 bila null/gagal). */
    private static double execScalar(org.hibernate.Session session, String sql, Map<String, Object> prm) {
        try {
            SQLQuery q = session.createSQLQuery(sql);
            for (Map.Entry<String, Object> e : prm.entrySet()) { q.setParameter(e.getKey(), e.getValue()); }
            Object o = q.uniqueResult();
            return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0;
        } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/koperasi/helper/LaporanKantinUtil.java:172"); return 0.0; }
    }

    /** Klausa "<= tglSampai" (batas atas saja, utk Neraca per tanggal). */
    private static String klausaSampai(String kolom, String tglSampai, Map<String, Object> p) {
        if (!ada(tglSampai)) { return ""; }
        p.put("tglSampai", tglSampai.trim());
        return " AND cast(" + kolom + " as date) <= cast(:tglSampai as date) ";
    }

    // ==========================================================================================
    // AKUNTANSI — Laporan berbasis BUKU BESAR NYATA (akunting.transaksi), meniru pengalaman
    // Accurate (Jurnal, Buku Besar, Neraca Saldo, Rekening Koran, Laba Rugi). Berbeda dari
    // laporan operasional (fin_*, gl_rincian) yang hanya APPROKSIMASI dari tabel penjualan/pengadaan.
    // Data hanya muncul setelah transaksi kantin DIPOSTING ke jurnal (Posting HPP Kantin).
    // ==========================================================================================

    /** ID Satuan Kerja kantin dari konfigurasi 'satuan_kerja_kantin' (-1 = semua / belum diisi). */
    private static long kantinSatkerId(org.hibernate.Session session) {
        if (lintasSatker()) {
            return -1;   // -1 = tanpa penyaringan satuan kerja (lihat LINTAS_SATKER)
        }
        Long dipilih = SATKER_PILIHAN.get();
        if (dipilih != null) {
            // Pilihan pengguna mengalahkan konfigurasi; nilai <= 0 berarti SEMUA satuan kerja
            // (laporan konsolidasi seluruh unit).
            return dipilih.longValue() > 0 ? dipilih.longValue() : -1L;
        }
        try {
            String v = Common.getKonfigurasi("satuan_kerja_kantin", "").getNilai();
            if (v != null && v.trim().length() > 0) { return Long.parseLong(v.trim()); }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LaporanKantinUtil.java:194"); /* abaikan: -1 = tampilkan semua Satuan Kerja */ }
        return -1L;
    }

    /**
     * Klausa WHERE dasar Buku Besar akuntansi NYATA untuk laporan "Akuntansi" kantin.
     * Alias WAJIB: a = akunting.transaksi, a1 = akunting.grup_transaksi, d = akunting.akun.
     * Menyaring: HANYA jurnal TERPOSTING (a1.posting_history is not null) dan tersaring ke Satuan
     * Kerja kantin (a1.satuan_kerja) bila konfigurasi 'satuan_kerja_kantin' terisi. Mengisi :satker
     * (+ :tglMulai/:tglSampai bila ada) ke map. Pola IDENTIK dgn cek_pemetaan_akun.jsp.
     */
    private static String klausaLedger(org.hibernate.Session session, String tglMulai, String tglSampai, Map<String, Object> p) {
        long satker = kantinSatkerId(session);
        p.put("satker", Long.valueOf(satker));
        StringBuilder sb = new StringBuilder();
        sb.append(" where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) ");
        sb.append(klausaTanggal("a.tanggal_transaksi", tglMulai, tglSampai, p));
        return sb.toString();
    }

    /**
     * Penanda "tampilkan lintas satuan kerja" untuk satu permintaan laporan.
     *
     * <p>Semua laporan berbasis jurnal biasanya disaring ke satuan kerja kantin
     * ({@code konfigurasi satuan_kerja_kantin}). Akibatnya jurnal dari modul lain (payroll,
     * pengadaan aset, pembayaran transfer) yang diposting ke satuan kerja BERBEDA tidak muncul di
     * Neraca/Laba Rugi kantin &mdash; laporannya benar menurut definisinya, tetapi bukan gambaran
     * lembaga secara utuh. Parameter {@code lintasSatker=true} mematikan penyaringan itu.</p>
     *
     * <p>Disimpan per-thread karena {@code build()} bekerja satu permintaan per thread dan
     * klausa ledger dipakai puluhan cabang laporan; menambah parameter ke semua cabang hanya akan
     * menyebarkan hal yang sama ke mana-mana. Selalu dibersihkan di akhir {@code build()}.</p>
     */
    private static final ThreadLocal<Boolean> LINTAS_SATKER = new ThreadLocal<Boolean>();

    /**
     * Satuan Kerja yang DIPILIH pengguna untuk satu permintaan laporan (parameter {@code satkerId}).
     *
     * <p>Satu instalasi AIS melayani banyak unit usaha (mis. sekolah, mart, katering, laundry) yang
     * masing-masing menuntut paket laporan keuangannya sendiri plus konsolidasi. Tanpa pilihan ini
     * semua laporan berbasis jurnal terkunci pada konfigurasi {@code satuan_kerja_kantin}, sehingga
     * hanya satu unit yang bisa dilihat per instalasi. Nilai &lt;= 0 berarti SEMUA unit
     * (konsolidasi); tidak diisi berarti ikut konfigurasi seperti sebelumnya, jadi perilaku lama
     * tidak berubah bagi pemanggil yang belum mengirim parameter ini.</p>
     *
     * <p>Disimpan per-thread dengan alasan yang sama seperti {@link #LINTAS_SATKER}: klausa ledger
     * dipakai puluhan cabang laporan. Selalu dibersihkan di akhir {@code build()}.</p>
     */
    private static final ThreadLocal<Long> SATKER_PILIHAN = new ThreadLocal<Long>();

    private static boolean lintasSatker() {
        Boolean b = LINTAS_SATKER.get();
        return b != null && b.booleanValue();
    }

    /** Klausa ledger KUMULATIF: seluruh jurnal terposting s/d Tgl Sampai (Tgl Mulai diabaikan).
     *  Dipakai Neraca & saldo akhir Kas/Bank yang memang bersifat akumulatif, bukan periodik. */
    private static String klausaLedgerSampai(org.hibernate.Session session, String tglSampai, Map<String, Object> p) {
        long satker = kantinSatkerId(session);
        p.put("satker", Long.valueOf(satker));
        StringBuilder sb = new StringBuilder();
        sb.append(" where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) ");
        if (ada(tglSampai)) {
            sb.append(" and cast(a.tanggal_transaksi as date) <= cast(:tglSampai as date) ");
            p.put("tglSampai", tglSampai.trim());
        }
        return sb.toString();
    }

    /** Klausa ledger SEBELUM periode: dipakai menghitung saldo awal Kas/Bank pada Arus Kas. */
    private static String klausaLedgerSebelum(org.hibernate.Session session, String tglMulai, Map<String, Object> p) {
        long satker = kantinSatkerId(session);
        p.put("satker", Long.valueOf(satker));
        StringBuilder sb = new StringBuilder();
        sb.append(" where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) ");
        if (ada(tglMulai)) {
            sb.append(" and cast(a.tanggal_transaksi as date) < cast(:tglMulai as date) ");
            p.put("tglMulai", tglMulai.trim());
        } else {
            // Tanpa Tgl Mulai tidak ada "sebelum periode" — saldo awal dianggap 0.
            sb.append(" and 1 = 0 ");
        }
        return sb.toString();
    }

    /**
     * Periode SEBELUMNYA yang sama panjang dengan {@code [tglMulai..tglSampai]}.
     * Dipakai laporan komparatif supaya pemakai tidak perlu mengisi dua rentang tanggal.
     */
    private static String[] periodeSebelumnya(String tglMulai, String tglSampai) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Date m = f.parse(ada(tglMulai) ? tglMulai.trim() : "1900-01-01");
            java.util.Date s2 = f.parse(ada(tglSampai) ? tglSampai.trim() : "2999-12-31");
            long panjang = s2.getTime() - m.getTime() + 86400000L;
            java.util.Date sblmSampai = new java.util.Date(m.getTime() - 86400000L);
            java.util.Date sblmMulai = new java.util.Date(sblmSampai.getTime() - panjang + 86400000L);
            return new String[] { f.format(sblmMulai), f.format(sblmSampai) };
        } catch (Exception e) {
            return new String[] { "1900-01-01", "1900-01-01" };
        }
    }

    /** Dua belas label bulan (YYYY-MM) yang berakhir pada bulan {@code tglSampai}. */
    private static String[] duaBelasBulan(String tglSampai) {
        String[] hasil = new String[12];
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(ada(tglSampai) ? f.parse(tglSampai.trim()) : ais.ui.util.WaktuUtil.getDate());
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            c.add(java.util.Calendar.MONTH, -11);
            java.text.SimpleDateFormat fb = new java.text.SimpleDateFormat("yyyy-MM");
            for (int i = 0; i < 12; i++) {
                hasil[i] = fb.format(c.getTime());
                c.add(java.util.Calendar.MONTH, 1);
            }
        } catch (Exception e) {
            for (int i = 0; i < 12; i++) {
                hasil[i] = "";
            }
        }
        return hasil;
    }

    /** Jalankan SQL agregat 1 baris 1 kolom angka; 0 bila kosong/gagal. */
    private static double angkaTunggal(org.hibernate.Session session, String sql, Map<String, Object> prm) {
        try {
            SQLQuery q = session.createSQLQuery(sql);
            for (Map.Entry<String, Object> e : prm.entrySet()) { q.setParameter(e.getKey(), e.getValue()); }
            Object v = q.uniqueResult();
            return (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit LaporanKantinUtil.angkaTunggal");
            return 0.0;
        }
    }

    /** FROM standar Buku Besar (transaksi + grup_transaksi + akun). */
    private static final String FROM_LEDGER =
        " from akunting.transaksi a "
      + " join akunting.grup_transaksi a1 on a1.id = a.grup_transaksi "
      + " join akunting.akun d on d.id = a.akun ";

    /** Ekspresi pengenal akun Kas/Bank (heuristik: flag bank/no rekening, atau nama mengandung kas/bank).
     *  Catatan penamaan kolom: getBank()=@JoinColumn "bank_id"; getNoRek() TANPA @Column → terlipat "norek". */
    private static final String EKSPRESI_KASBANK =
        " d.bank_id is not null or d.norek is not null "
      + " or lower(coalesce(d.nama,'')) like '%kas%' or lower(coalesce(d.nama,'')) like '%bank%' ";
    private static final String FILTER_KASBANK = " and (" + EKSPRESI_KASBANK + ") ";
    /** Kebalikannya: akun LAWAN (bukan kas/bank) — dipakai Arus Kas utk menguraikan sumber & penggunaan kas. */
    private static final String FILTER_BUKAN_KASBANK = " and not (" + EKSPRESI_KASBANK + ") ";

    /** JOIN klasifikasi akun -> Kelompok Laporan -> Jenis Laporan (utk laba rugi / beban). Alias b/c/f/m.
     *  master_grup_laporan (alias m) WAJIB ikut: pada data nyata kolom kelompok_laporan.keterangan sering
     *  KOSONG dan nama kelompok sesungguhnya ("Aktiva Tetap", "Beban Gaji", ...) ada di master_grup_laporan. */
    private static final String JOIN_KLAS =
        " join akunting.kelompok_laporan_punya_akun b on b.akun = d.id "
      + " join akunting.kelompok_laporan c on c.id = b.kelompok_laporan "
      + " join akunting.jenis_laporan f on f.id = c.jenis_laporan "
      + " left join akunting.master_grup_laporan m on m.id = c.master_grup_laporan ";

    /** Label kelompok yang ditampilkan; jatuh ke master_grup_laporan bila keterangan kelompok kosong. */
    private static final String LABEL_KLAS =
        " coalesce(nullif(trim(coalesce(c.keterangan,'')),''), nullif(trim(coalesce(m.keterangan,'')),''), '(Tanpa Kelompok)') ";

    /** Ekspresi tag klasifikasi (kelompok + master grup + jenis) huruf kecil. */
    private static final String TAG_KLAS =
        " lower(coalesce(c.keterangan,'') || ' ' || coalesce(m.keterangan,'') || ' ' || coalesce(f.keterangan,'')) ";

    /** Urutan baku seksi Arus Kas: Operasional, Investasi, Pendanaan, lain-lain, lalu yang belum dipetakan. */
    private static final String URUT_SEKSI_ARUS =
        " case when lower(l.seksi) like '%operasi%' then 1 "
      + "      when lower(l.seksi) like '%investasi%' then 2 "
      + "      when lower(l.seksi) like '%pendanaan%' or lower(l.seksi) like '%pembiayaan%' then 3 "
      + "      when lower(l.seksi) like '%belum dipetakan%' then 9 else 4 end ";

    /** Filter akun BEBAN/BIAYA (termasuk HPP) berdasar klasifikasi + hanya kelompok aktif. */
    private static final String FILTER_BEBAN =
        " and (c.aktif is null or c.aktif) and ( " + TAG_KLAS + " like '%beban%' or " + TAG_KLAS + " like '%biaya%' "
      + "      or " + TAG_KLAS + " like '%hpp%' or " + TAG_KLAS + " like '%harga pokok%' ) ";

    /** Filter akun PPN (heuristik nama akun, karena kantin tak punya modul pajak khusus). */
    private static final String FILTER_PPN_ANY =
        " and ( lower(coalesce(d.nama,'')) like '%ppn%' or lower(coalesce(d.nama,'')) like '%pajak pertambahan%' ) ";
    private static final String FILTER_PPN_KELUARAN =
        " and ( lower(coalesce(d.nama,'')) like '%ppn keluaran%' or lower(coalesce(d.nama,'')) like '%ppn-keluaran%' "
      + "   or (lower(coalesce(d.nama,'')) like '%ppn%' and lower(coalesce(d.nama,'')) like '%keluar%') "
      + "   or lower(coalesce(d.nama,'')) like '%pajak keluaran%' ) ";
    private static final String FILTER_PPN_MASUKAN =
        " and ( lower(coalesce(d.nama,'')) like '%ppn masukan%' or lower(coalesce(d.nama,'')) like '%ppn-masukan%' "
      + "   or (lower(coalesce(d.nama,'')) like '%ppn%' and lower(coalesce(d.nama,'')) like '%masuk%') "
      + "   or lower(coalesce(d.nama,'')) like '%pajak masukan%' ) ";

    /**
     * Bangun satu laporan dari parameter request:
     *   r, tokoId, tglMulai, tglSampai, qProduk, qPelanggan.
     */
    @SuppressWarnings("unchecked")
    public static Hasil build(HttpServletRequest request) {
        Hasil H = new Hasil();
        try {
            Tbmuser tbmuser = Common.getCurrentUser(request);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                H.status = "01"; H.message = "Sesi Anda telah berakhir. Silakan masuk kembali."; return H;
            }

            String r = request.getParameter("r");
            String tglMulai = request.getParameter("tglMulai");
            String tglSampai = request.getParameter("tglSampai");
            String qProduk = request.getParameter("qProduk");
            String qPelanggan = request.getParameter("qPelanggan");
            // Filter kasir: laporan penjualan sering perlu dipersempit ke satu kasir
            // (mis. saat menelusuri selisih setoran shift).
            String qKasir = request.getParameter("qKasir");

            Long tokoId = null;
            boolean lockToko = false;
            if (tbmuser.getPedagang() != null && tbmuser.getPedagang().getToko() != null) {
                tokoId = tbmuser.getPedagang().getToko().getId();
                lockToko = true;
            } else {
                String tp = request.getParameter("tokoId");
                if (ada(tp)) { try { tokoId = Long.valueOf(tp.trim()); } catch (Exception e) { tokoId = null; } }
            }
            H.lockToko = lockToko;

            // "Per toko / total": bila dicentang DAN sedang melihat SEMUA toko (tak ada toko spesifik &
            // bukan pedagang), laporan yang mendukung dikelompokkan per toko. Branch yg mendukung meng-set
            // tokoIdCol = kolom id toko pada tabel utamanya (mis. "p.toko"/"h.toko"/"pr.toko").
            boolean perToko = "true".equalsIgnoreCase(request.getParameter("perToko")) && tokoId == null;

            Session session = HibernateUtil.currentSession();
            LINTAS_SATKER.set(Boolean.valueOf("true".equalsIgnoreCase(request.getParameter("lintasSatker"))));
            String satkerParam = request.getParameter("satkerId");
            if (ada(satkerParam)) {
                try { SATKER_PILIHAN.set(Long.valueOf(satkerParam.trim())); }
                catch (Exception e) { SATKER_PILIHAN.remove(); }
            } else {
                SATKER_PILIHAN.remove();
            }
            Map<String, Object> prm = new LinkedHashMap<String, Object>();
            String sql = null;
            String[] tipe = null;
            List<Kolom> kolom = H.kolom;
            String judul = "Laporan";
            String catatan = "";
            int grupIdx = -1;
            String tokoIdCol = null;

            String qp = ada(qProduk) ? ("%" + qProduk.trim().toLowerCase() + "%") : null;
            String qc = ada(qPelanggan) ? ("%" + qPelanggan.trim().toLowerCase() + "%") : null;
            String qk = ada(qKasir) ? ("%" + qKasir.trim().toLowerCase() + "%") : null;
            // Filter tambahan laporan stok (dipakai "stok_per_tanggal"): kategori/jenis barang,
            // grup produk, hanya barang aktif, dan sembunyikan stok nol. Semua opsional --
            // laporan lain mengabaikannya sehingga tidak mengubah perilaku yang sudah ada.
            Long jenisProdukId = null;
            String jpParam = request.getParameter("jenisProdukId");
            if (ada(jpParam)) { try { jenisProdukId = Long.valueOf(jpParam.trim()); } catch (Exception e) { jenisProdukId = null; } }
            Long grupProdukId = null;
            String gpParam = request.getParameter("grupProdukId");
            if (ada(gpParam)) { try { grupProdukId = Long.valueOf(gpParam.trim()); } catch (Exception e) { grupProdukId = null; } }
            boolean hanyaAktif = "true".equalsIgnoreCase(request.getParameter("hanyaAktif"));
            boolean hanyaStokTidakNol = "true".equalsIgnoreCase(request.getParameter("hanyaStokTidakNol"));

            if ("pnj_faktur".equals(r)) {
                judul = "Daftar Faktur Penjualan";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                // Kembali dihitung (dibayar - total, minimal 0) agar tidak bergantung pada kolom
                // 'kembalian' yang bisa belum ada di DB (getter tanpa @Column -> rawan drift skema).
                sql = "select h.tanggal_pembayaran, h.kode, t.nama, coalesce(a.nama,'Umum'), "
                    + " coalesce(h.total_biaya,0), (coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)), "
                    + " greatest((coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)) - coalesce(h.total_biaya,0), 0) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + " left join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + w + " order by h.tanggal_pembayaran desc ";
                tipe = new String[]{"tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Pelanggan","text"));
                kolom.add(new Kolom("Total","num")); kolom.add(new Kolom("Dibayar","num")); kolom.add(new Kolom("Kembali","num"));

            } else if ("pnj_per_barang".equals(r)) { tokoIdCol = "p.toko";
                judul = "Penjualan per Barang";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                if (qk != null) { w.append(" and lower(coalesce(h.kasir_login_nama,'')) like :qk "); prm.put("qk", qk); }
                sql = "select " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", sum(coalesce(p.qty,0)), sum(" + OMZET + ") "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.pembelian_anggota_koperasi h on h.id=p.pembelian_anggota_koperasi "
                    + w + " group by 1, 2 order by 4 desc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Produk","text")); kolom.add(new Kolom("Qty Terjual","num")); kolom.add(new Kolom("Total Penjualan","num"));

            } else if ("pnj_barang_laku".equals(r)) { tokoIdCol = "p.toko";
                judul = "Barang Paling Laku";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                if (qk != null) { w.append(" and lower(coalesce(h.kasir_login_nama,'')) like :qk "); prm.put("qk", qk); }
                sql = "select " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", sum(coalesce(p.qty,0)), sum(" + OMZET + ") "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.pembelian_anggota_koperasi h on h.id=p.pembelian_anggota_koperasi "
                    + w + " group by 1, 2 order by 3 desc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Produk","text")); kolom.add(new Kolom("Qty Terjual","num")); kolom.add(new Kolom("Total Penjualan","num"));

            } else if ("pnj_rincian_barang".equals(r)) {
                judul = "Rincian Penjualan per Barang"; grupIdx = 0;
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,p.kode,'')) like :qp"
                        + " or lower(coalesce(pr.nama,p.nama,'')) like :qp) "); prm.put("qp", qp); }
                if (qk != null) { w.append(" and lower(coalesce(h.kasir_login_nama,'')) like :qk "); prm.put("qk", qk); }
                sql = "select " + LABEL_PRODUK_ITEM + " as produk, p.waktu, h.kode, " + KASIR_NOTA
                    + ", coalesce(p.qty,0), coalesce(nullif(trim(sd.nama),''),'') , " + LABEL_SATUAN_JUAL
                    + ", coalesce(p.hargasatuan, pr.hargajual, 0), " + OMZET
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.pembelian_anggota_koperasi h on h.id=p.pembelian_anggota_koperasi "
                    + JOIN_SATUAN_ITEM
                    + w + " order by 1 asc, p.waktu desc ";
                tipe = new String[]{"text","tgl","text","text","num","text","text","num","num"};
                kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text"));
                kolom.add(new Kolom("Kasir","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Satuan","text")); kolom.add(new Kolom("Satuan Jual","text"));
                kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Total","num"));

            } else if ("pnj_per_pelanggan".equals(r)) { tokoIdCol = "h.toko";
                judul = "Penjualan Barang per Pelanggan";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(a.kode, a.kode_identitas, '-'), coalesce(a.nama,'Umum / Non-Anggota'), "
                    + " count(distinct h.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + " left join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + w + " group by a.kode, a.kode_identitas, a.nama order by 4 desc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Pelanggan","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Total Belanja","num"));

            } else if ("pnj_per_kategori_pelanggan".equals(r)) { tokoIdCol = "h.toko";
                judul = "Penjualan per Kategori Pelanggan";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select coalesce(j.nama,'Umum / Tanpa Kategori'), count(distinct h.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + " left join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + " left join koperasi.jenis_anggota_koperasi j on j.id=a.jenis_anggota_koperasi "
                    + w + " group by j.nama order by 3 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Kategori Pelanggan","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Total Belanja","num"));

            } else if ("pnj_per_cabang".equals(r)) {
                judul = "Penjualan per Cabang (Toko/Merchant)";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select t.nama, count(distinct h.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + w + " group by t.nama order by 3 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Toko / Merchant","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Total Penjualan","num"));

            } else if ("pos_per_kasir".equals(r)) { tokoIdCol = "h.toko";
                judul = "Laporan Penerimaan Per Kasir";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select " + KASIR_NOTA + ", count(distinct h.id), sum(coalesce(h.bayar_tunai,0)), "
                    + " sum(coalesce(h.bayar_non_tunai,0)), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + w + " group by " + KASIR_NOTA + " order by 5 desc ";
                tipe = new String[]{"text","num","num","num","num"};
                kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Tunai","num")); kolom.add(new Kolom("Non Tunai","num")); kolom.add(new Kolom("Total","num"));

            } else if ("transaksi_per_kasir".equals(r)) { tokoIdCol = "h.toko";
                judul = "Transaksi Per Kasir";
                catatan = "Rekonsiliasi transaksi dan sesi: modal awal + tunai dibandingkan dengan uang fisik saat closing.";
                String filterH = " where 1=1 " + kondToko("h.toko", tokoId, prm)
                        + klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm);
                String filterK = " where 1=1 " + kondToko("k.toko", tokoId, prm)
                        + klausaTanggal("k.waktubuka", tglMulai, tglSampai, prm);
                sql = "with trx as (select coalesce(nullif(trim(h.kasir_login_nama),''),nullif(trim(h.oleh),''),'-') kasir,"
                    + " count(distinct h.id) jml,sum(coalesce(h.bayar_tunai,0)) tunai,sum(coalesce(h.bayar_non_tunai,0)) nontunai,sum(coalesce(h.total_biaya,0)) total"
                    + " from koperasi.pembelian_anggota_koperasi h " + filterH + " group by 1),"
                    + " sesi as (select coalesce(nullif(trim(k.kasir_nama),''),'-') kasir,sum(coalesce(k.modalawal,0)) modal,"
                    + " sum(case when coalesce(k.status,'BUKA')='TUTUP' then coalesce(k.uangfisik,0) else coalesce(k.modalawal,0)+coalesce(k.totaltunai,0) end) closing"
                    + " from koperasi.sesi_kas_kasir k " + filterK + " group by 1)"
                    + " select coalesce(t.kasir,s.kasir),coalesce(t.jml,0),coalesce(s.modal,0),coalesce(t.tunai,0),coalesce(t.nontunai,0),coalesce(t.total,0),"
                    + " coalesce(s.modal,0)+coalesce(t.tunai,0),coalesce(s.closing,0),coalesce(s.closing,0)-(coalesce(s.modal,0)+coalesce(t.tunai,0))"
                    + " from trx t full outer join sesi s on lower(trim(s.kasir))=lower(trim(t.kasir)) order by 6 desc";
                tipe = new String[]{"text","num","num","num","num","num","num","num","num"};
                kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Jml Transaksi","num"));
                kolom.add(new Kolom("Modal Awal","num")); kolom.add(new Kolom("Tunai","num"));
                kolom.add(new Kolom("Non Tunai","num")); kolom.add(new Kolom("Total Transaksi","num"));
                kolom.add(new Kolom("Kas Seharusnya","num")); kolom.add(new Kolom("Kas Closing","num"));
                kolom.add(new Kolom("Selisih","num"));

            } else if ("pos_per_akun_bank".equals(r)) { tokoIdCol = "h.toko";
                judul = "Penerimaan Penjualan Per Metode / Akun Bank";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select coalesce(cp.nama,'Tunai / Cash'), count(distinct h.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + " left join koperasi.cara_pembayaran_koperasi cp on cp.id=h.cara_pembayaran_koperasi "
                    + w + " group by cp.nama order by 3 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Metode / Akun Bank","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Total Diterima","num"));

            } else if ("pnj_per_pemasok".equals(r)) { tokoIdCol = "p.toko";
                judul = "Rincian Penjualan Barang Per Pemasok"; grupIdx = 0;
                catatan = "Setiap pemasok dirinci sampai produk yang terjual. Pemasok ditentukan dari pengadaan terakhir tiap produk.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("p.toko", tokoId, prm));
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(" + KODE_PRODUK_ITEM + ") like :qp or lower(" + NAMA_PRODUK_ITEM + ") like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(ps.pemasok,'(Tanpa Pemasok)'), " + KODE_PRODUK_ITEM + ", "
                    + NAMA_PRODUK_ITEM + ", " + NAMA_SATUAN_TRANSAKSI + ", "
                    + "sum(" + QTY_UOM_ITEM + "), sum(coalesce(p.qty,0)), sum(" + OMZET + ") "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + JOIN_SATUAN_ITEM
                    + " left join (select distinct on (produk) produk, coalesce(nullif(namasupplier,''),'(Tanpa Pemasok)') as pemasok "
                    + "            from koperasi.pengadaan_produk order by produk, waktupengadaan desc) ps on ps.produk=p.produk "
                    + w + " group by ps.pemasok, " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", "
                    + NAMA_SATUAN_TRANSAKSI + " order by 1, 7 desc, 3 ";
                tipe = new String[]{"text","text","text","text","num","num","num"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Kode Produk","text"));
                kolom.add(new Kolom("Produk Terjual","text")); kolom.add(new Kolom("Satuan Terjual","text"));
                kolom.add(new Kolom("Qty UOM","num")); kolom.add(new Kolom("Qty Dasar","num"));
                kolom.add(new Kolom("Total Penjualan","num"));

            } else if ("pnj_uang_muka".equals(r)) {
                judul = "Uang Muka Penjualan (Pembayaran Sebagian)";
                catatan = "Faktur dengan pembayaran sebagian (uang muka), belum lunas.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("h.toko", tokoId, prm));
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                w.append(" and (coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)) > 0 "
                       + " and (coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)) < coalesce(h.total_biaya,0) ");
                sql = "select h.tanggal_pembayaran, h.kode, t.nama, coalesce(a.nama,'Umum'), coalesce(h.total_biaya,0), "
                    + " (coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)), "
                    + " (coalesce(h.total_biaya,0)-(coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0))) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + " left join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + w + " order by h.tanggal_pembayaran desc ";
                tipe = new String[]{"tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Pelanggan","text"));
                kolom.add(new Kolom("Total","num")); kolom.add(new Kolom("Uang Muka","num")); kolom.add(new Kolom("Sisa","num"));

            } else if ("beli_penerimaan".equals(r)) {
                judul = "Daftar Penerimaan Barang";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("a.toko", tokoId, prm));
                w.append(klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm));
                sql = "select coalesce(nullif(a.nomorfaktur,''),'(Tanpa No. Faktur)'), max(a.waktupengadaan), coalesce(a.namasupplier,'-'), "
                    + " count(*), sum(coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0)) "
                    + " from koperasi.pengadaan_produk a " + w
                    + " group by a.nomorfaktur, a.namasupplier order by 2 desc ";
                tipe = new String[]{"text","tgl","text","num","num"};
                kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Jml Item","num")); kolom.add(new Kolom("Total Pembelian","num"));

            } else if ("beli_rincian_penerimaan".equals(r)) {
                judul = "Rincian Penerimaan Barang"; grupIdx = 0;
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("a.toko", tokoId, prm));
                w.append(klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(nullif(a.namasupplier,''),'(Tanpa Pemasok)') as pemasok, a.waktupengadaan, coalesce(nullif(a.nomorfaktur,''),'-'), pr.kode, pr.nama, "
                    + " coalesce(a.qty,0), coalesce(a.hargabelisatuan,0), coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0) "
                    + " from koperasi.pengadaan_produk a join koperasi.produk pr on pr.id=a.produk " + w
                    + " order by a.namasupplier asc, a.waktupengadaan desc ";
                tipe = new String[]{"text","tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Harga Beli","num")); kolom.add(new Kolom("Total","num"));

            } else if ("beli_pesanan".equals(r)) {
                judul = "Daftar Pesanan/Pembelian per Pemasok";
                catatan = "Kantin mencatat pembelian sebagai pengadaan barang; diringkas per pemasok.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("a.toko", tokoId, prm));
                w.append(klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm));
                sql = "select coalesce(nullif(a.namasupplier,''),'(Tanpa Pemasok)'), count(*), sum(coalesce(a.qty,0)), "
                    + " sum(coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0)) "
                    + " from koperasi.pengadaan_produk a " + w
                    + " group by a.namasupplier order by 4 desc ";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Jml Item","num")); kolom.add(new Kolom("Total Qty","num")); kolom.add(new Kolom("Total Pembelian","num"));

            } else if ("beli_rincian_pesanan".equals(r)) {
                judul = "Rincian Pesanan/Pembelian per Pemasok"; grupIdx = 0;
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("a.toko", tokoId, prm));
                w.append(klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(nullif(a.namasupplier,''),'(Tanpa Pemasok)'), a.waktupengadaan, coalesce(nullif(a.nomorfaktur,''),'-'), pr.kode, pr.nama, "
                    + " coalesce(a.qty,0), coalesce(a.hargabelisatuan,0), coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0) "
                    + " from koperasi.pengadaan_produk a join koperasi.produk pr on pr.id=a.produk " + w
                    + " order by a.namasupplier asc, a.waktupengadaan desc ";
                tipe = new String[]{"text","tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Harga Beli","num")); kolom.add(new Kolom("Total","num"));

            } else if ("sed_barang_jasa".equals(r)) {
                judul = "Daftar Barang dan Jasa";
                // Gap-closure rekonsiliasi Accurate vs e-campus (Toko Al-Bahjah): laporan ini SEBELUMNYA
                // tak menyertakan Barcode/PLU, Satuan, Pemasok, maupun nilai HPP total -- padahal semua
                // field itu SUDAH ada di entitas Produk (diisi via impor Excel Katalog Barang), cuma
                // tak pernah di-SELECT di sini. Ditambahkan supaya laporan ini bisa dipakai langsung utk
                // mencocokkan data dgn ekspor "Daftar Barang dan Jasa" Accurate (format kolomnya sengaja
                // diseragamkan dgn KOLOM_EXCEL_PRODUK di KantinHelper -- lihat JavaDoc kelas itu).
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
				sql = "select pr.kode, coalesce(pr.barcode,'-'), pr.nama, coalesce(k.nama,'Umum'), coalesce(t.nama,'-'), "
					+ " coalesce(nullif(trim(s.nama),''),'(Belum diatur)'), coalesce(nullif(trim(pm.nama),''),'(Belum diatur)'), coalesce(pr.hargabeli,0), coalesce(pr.hargajual,0), coalesce(pr.stok,0), "
                    + " (coalesce(pr.hargabeli,0) * coalesce(pr.stok,0)) "
                    + " from koperasi.produk pr left join koperasi.jenis_produk k on k.id=pr.jenis_produk "
                    + " left join koperasi.toko t on t.id=pr.toko "
                    + " left join koperasi.satuan_produk s on s.id=pr.satuan "
                    + " left join koperasi.pemasok_produk pm on pm.id=pr.pemasok " + w + " order by pr.nama asc ";
                tipe = new String[]{"text","text","text","text","text","text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Barcode/PLU","text")); kolom.add(new Kolom("Nama","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Satuan","text")); kolom.add(new Kolom("Pemasok","text"));
                kolom.add(new Kolom("Harga Beli","num")); kolom.add(new Kolom("Harga Jual","num")); kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Nilai HPP (Beli x Stok)","num"));

            } else if ("sed_penyesuaian".equals(r)) {
                judul = "Daftar Penyesuaian Stok Barang";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, count(*), sum(coalesce(o.selisih,0)) "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk " + w
                    + " group by pr.kode, pr.nama order by 4 asc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Jml Penyesuaian","num")); kolom.add(new Kolom("Total Selisih","num"));

            } else if ("sed_penyesuaian_rinci".equals(r)) {
                judul = "Penyesuaian Stok Barang (Rincian)"; grupIdx = 0;
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select (coalesce(pr.kode,'') || ' - ' || coalesce(pr.nama,'')) as produk, o.waktuopname, coalesce(o.stoksistem,0), coalesce(o.stokfisik,0), coalesce(o.selisih,0), coalesce(o.keterangan,'') "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk " + w
                    + " order by pr.nama asc, o.waktuopname desc ";
                tipe = new String[]{"text","tgl","num","num","num","text"};
                kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Stok Sistem","num"));
                kolom.add(new Kolom("Stok Fisik","num")); kolom.add(new Kolom("Selisih","num")); kolom.add(new Kolom("Keterangan","text"));

            } else if ("gud_kuantitas".equals(r)) {
                judul = "Kuantitas Barang per Gudang";
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(t.nama,'-'), pr.kode, pr.nama, coalesce(k.nama,'Umum'), coalesce(pr.stok,0) "
                    + " from koperasi.produk pr left join koperasi.toko t on t.id=pr.toko "
                    + " left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w
                    + " order by t.nama asc, pr.nama asc ";
                tipe = new String[]{"text","text","text","text","num"};
                kolom.add(new Kolom("Gudang / Toko","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Stok","num"));

            } else if ("gud_penghitungan".equals(r)) {
                judul = "Lembar Penghitungan Stok";
                catatan = "Lembar kerja: cetak, lalu isi kolom Hitungan Fisik & Selisih secara manual.";
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(t.nama,'-'), coalesce(pr.stok,0), '' , '' "
                    + " from koperasi.produk pr left join koperasi.toko t on t.id=pr.toko " + w
                    + " order by pr.nama asc ";
                tipe = new String[]{"text","text","text","num","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Stok Sistem","num"));
                kolom.add(new Kolom("Hitungan Fisik","text")); kolom.add(new Kolom("Selisih","text"));

            } else if ("gud_mutasi_item".equals(r)) {
                judul = "Ringkasan Mutasi Gudang per Item";
                String tkMasuk = kondToko("toko", tokoId, prm) + klausaTanggal("waktupengadaan", tglMulai, tglSampai, prm);
                String tkKeluar = kondToko("toko", tokoId, prm) + klausaTanggal("waktu", tglMulai, tglSampai, prm);
                String tkOpname = kondToko("toko", tokoId, prm) + klausaTanggal("waktuopname", tglMulai, tglSampai, prm);
                boolean adaPakai = tabelAda(session, "koperasi.pemakaian_bahan_baku");
                String pkSel = "0"; String pkJoin = "";
                if (adaPakai) {
                    String tkPakai = kondToko("toko", tokoId, prm) + klausaTanggal("waktu", tglMulai, tglSampai, prm);
                    pkSel = "coalesce(pk.pakai,0)";
                    pkJoin = " left join (select produk, sum(coalesce(qty,0)) pakai from koperasi.pemakaian_bahan_baku where 1=1 " + tkPakai + " group by produk) pk on pk.produk=pr.id ";
                }
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(m.masuk,0), coalesce(k.keluar,0), coalesce(o.adj,0), " + pkSel + ", coalesce(pr.stok,0) "
                    + " from koperasi.produk pr "
                    + " left join (select produk, sum(coalesce(qty,0)) masuk from koperasi.pengadaan_produk where 1=1 " + tkMasuk + " group by produk) m on m.produk=pr.id "
                    + " left join (select produk, sum(coalesce(qty,0)) keluar from koperasi.pembelian where 1=1 " + tkKeluar + " group by produk) k on k.produk=pr.id "
                    + " left join (select produk, sum(coalesce(selisih,0)) adj from koperasi.stok_opname where 1=1 " + tkOpname + " group by produk) o on o.produk=pr.id "
                    + pkJoin
                    + w + " order by pr.nama asc ";
                tipe = new String[]{"text","text","num","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Masuk","num")); kolom.add(new Kolom("Keluar","num"));
                kolom.add(new Kolom("Penyesuaian","num")); kolom.add(new Kolom("Pakai Bahan","num")); kolom.add(new Kolom("Stok Akhir","num"));

            } else if ("gud_mutasi_kategori".equals(r)) {
                judul = "Ringkasan Mutasi Gudang per Kategori";
                String tkMasuk = kondToko("toko", tokoId, prm) + klausaTanggal("waktupengadaan", tglMulai, tglSampai, prm);
                String tkKeluar = kondToko("toko", tokoId, prm) + klausaTanggal("waktu", tglMulai, tglSampai, prm);
                String tkOpname = kondToko("toko", tokoId, prm) + klausaTanggal("waktuopname", tglMulai, tglSampai, prm);
                boolean adaPakai = tabelAda(session, "koperasi.pemakaian_bahan_baku");
                String pkSel = "0"; String pkJoin = "";
                if (adaPakai) {
                    String tkPakai = kondToko("toko", tokoId, prm) + klausaTanggal("waktu", tglMulai, tglSampai, prm);
                    pkSel = "coalesce(sum(pk.pakai),0)";
                    pkJoin = " left join (select produk, sum(coalesce(qty,0)) pakai from koperasi.pemakaian_bahan_baku where 1=1 " + tkPakai + " group by produk) pk on pk.produk=pr.id ";
                }
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                sql = "select coalesce(k.nama,'Umum'), coalesce(sum(m.masuk),0), coalesce(sum(kl.keluar),0), coalesce(sum(o.adj),0), " + pkSel + ", coalesce(sum(pr.stok),0) "
                    + " from koperasi.produk pr left join koperasi.jenis_produk k on k.id=pr.jenis_produk "
                    + " left join (select produk, sum(coalesce(qty,0)) masuk from koperasi.pengadaan_produk where 1=1 " + tkMasuk + " group by produk) m on m.produk=pr.id "
                    + " left join (select produk, sum(coalesce(qty,0)) keluar from koperasi.pembelian where 1=1 " + tkKeluar + " group by produk) kl on kl.produk=pr.id "
                    + " left join (select produk, sum(coalesce(selisih,0)) adj from koperasi.stok_opname where 1=1 " + tkOpname + " group by produk) o on o.produk=pr.id "
                    + pkJoin
                    + w + " group by k.nama order by 2 desc ";
                tipe = new String[]{"text","num","num","num","num","num"};
                kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Masuk","num")); kolom.add(new Kolom("Keluar","num")); kolom.add(new Kolom("Penyesuaian","num"));
                kolom.add(new Kolom("Pakai Bahan","num")); kolom.add(new Kolom("Stok Akhir","num"));

            } else if ("ar_faktur_belum_lunas".equals(r)) { tokoIdCol = "u.toko";
                judul = "Faktur Belum Lunas";
                catatan = "Hanya slot pembayaran Kasbon yang menjadi piutang. Voucher, QRIS, Tunai, dan Transfer tidak termasuk.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("u.toko", tokoId, prm));
                w.append(klausaTanggal("u.tanggal", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select u.tanggal, u.kode_nota, t.nama, coalesce(a.nama,'Umum'), max(u.total_nota), "
                    + " string_agg(distinct u.jenis_piutang, ', '), sum(u.nilai_piutang) "
                    + " from (" + sqlSumberKasbon() + ") u join koperasi.toko t on t.id=u.toko "
                    + " join koperasi.anggota_koperasi a on a.id=u.anggota_koperasi " + w
                    + " group by u.id_nota,u.tanggal,u.kode_nota,u.toko,t.nama,a.nama order by u.tanggal desc ";
                tipe = new String[]{"tgl","text","text","text","num","text","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Pelanggan","text"));
                kolom.add(new Kolom("Total Nota","num")); kolom.add(new Kolom("Jenis Piutang","text")); kolom.add(new Kolom("Nilai Piutang","num"));

            } else if ("ar_saldo".equals(r)) { tokoIdCol = "u.toko";
                judul = "Daftar Saldo Piutang Customer";
                catatan = "Piutang dihitung dari nominal setiap slot Kasbon (Divisi/Pejuang/Operasional). Voucher, QRIS, Tunai, dan Transfer tidak masuk. Klik jumlah faktur atau nilai piutang untuk melihat nota, jenis Kasbon, dan produk penyusunnya.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("u.toko", tokoId, prm));
                w.append(klausaTanggal("u.tanggal", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(a.kode, a.kode_identitas, '-'), coalesce(a.nama,'Umum / Non-Anggota'), count(distinct u.id_nota), "
                    + " string_agg(distinct u.jenis_piutang, ', '), sum(u.nilai_piutang) "
                    + " from (" + sqlSumberKasbon() + ") u join koperasi.anggota_koperasi a on a.id=u.anggota_koperasi " + w
                    + " group by a.kode, a.kode_identitas, a.nama order by 5 desc ";
                tipe = new String[]{"text","text","num","text","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Pelanggan","text")); kolom.add(new Kolom("Jml Faktur","num"));
                kolom.add(new Kolom("Jenis Piutang","text")); kolom.add(new Kolom("Saldo Piutang","num"));

            } else if ("akn_bb_pembantu_piutang".equals(r)) {
                judul = "Buku Besar Pembantu Piutang (per Pelanggan)"; grupIdx = 0;
                catatan = "Mutasi piutang tiap pelanggan/anggota: DEBET = belanja via metode 'Masuk sebagai Hutang', "
                    + "KREDIT = pembayaran hutang, dengan saldo berjalan per pelanggan.";
                sql = sqlMutasiPiutang(kondTanggalInline("h.tanggal_pembayaran", tglMulai, tglSampai),
                    kondTanggalInline("ph.waktu", tglMulai, tglSampai),
                    "select nama_anggota, cast(waktu as date), jenis_mutasi, keterangan, bertambah, berkurang, "
                    + " sum(bertambah - berkurang) over (partition by id_anggota order by waktu asc, baris_id asc "
                    + "   rows between unbounded preceding and current row) as saldo "
                    + " from mutasi order by nama_anggota asc, waktu asc, baris_id asc ");
                tipe = new String[]{"text","tgl","text","text","num","num","num"};
                kolom.add(new Kolom("Pelanggan","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Jenis","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet (Hutang)","num"));
                kolom.add(new Kolom("Kredit (Bayar)","num")); kolom.add(new Kolom("Saldo","num"));

            } else if ("akn_histori_piutang".equals(r)) {
                judul = "Histori Piutang (Kronologis)";
                catatan = "Seluruh mutasi piutang (belanja hutang & pembayaran hutang) urut waktu, dengan saldo total berjalan.";
                sql = sqlMutasiPiutang(kondTanggalInline("h.tanggal_pembayaran", tglMulai, tglSampai),
                    kondTanggalInline("ph.waktu", tglMulai, tglSampai),
                    "select cast(waktu as date), nama_anggota, jenis_mutasi, keterangan, bertambah, berkurang, "
                    + " sum(bertambah - berkurang) over (order by waktu asc, baris_id asc "
                    + "   rows between unbounded preceding and current row) as saldo "
                    + " from mutasi order by waktu asc, baris_id asc ");
                tipe = new String[]{"tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Pelanggan","text")); kolom.add(new Kolom("Jenis","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet (Hutang)","num"));
                kolom.add(new Kolom("Kredit (Bayar)","num")); kolom.add(new Kolom("Saldo","num"));

            } else if ("akn_bb_pembantu_utang".equals(r)) {
                judul = "Buku Besar Pembantu Utang (per Pemasok)"; grupIdx = 0;
                catatan = "Mutasi utang tiap pemasok: DEBET = pembayaran ke pemasok, KREDIT = kulakan masuk, "
                    + "dengan saldo berjalan (sisa utang). Pembayaran diambil dari modul Pembayaran Hutang Supplier "
                    + "sehingga saldo di sini adalah utang BERSIH, bukan lagi akumulasi kulakan saja.";
                String wU = kondToko("pp.toko", tokoId, prm) + klausaTanggal("pp.waktupengadaan", tglMulai, tglSampai, prm);
                // Dua sumber digabung: kulakan MENAMBAH utang, pembayaran MENGURANGI. Sebelum perbaikan
                // 2026-08-20 laporan ini hanya membaca kulakan sehingga saldonya selalu kelebihan.
                sql = "select pemasok, tgl, faktur, produk, tambah, kurang, "
                    + " sum(tambah - kurang) over (partition by pemasok order by tgl asc, urut asc "
                    + "   rows between unbounded preceding and current row) as saldo "
                    + " from ( select coalesce(nullif(trim(pp.namasupplier),''), coalesce(sp.nama,'Tanpa Nama Pemasok')) as pemasok, "
                    + "   cast(pp.waktupengadaan as date) as tgl, coalesce(pp.nomorfaktur,'-') as faktur, coalesce(pr.nama,'-') as produk, "
                    + "   coalesce(pp.totalharga, coalesce(pp.qty,0)*coalesce(pp.hargabelisatuan,0), 0) as tambah, 0.0 as kurang, pp.id as urut "
                    + "   from koperasi.pengadaan_produk pp "
                    + "   left join koperasi.produk pr on pr.id = pp.produk "
                    + "   left join koperasi.pengadaan_faktur pf on pf.id = pp.faktur_pengadaan "
                    + "   left join library.penyedia sp on sp.id = pf.supplier " + wU
                    + "   union all "
                    + "   select coalesce(s2.nama,'Tanpa Nama Pemasok'), cast(pb.tanggal as date), "
                    + "     coalesce(pb.kode_unik,'-'), 'Pembayaran ' || coalesce(pb.metode,''), 0.0, "
                    + "     coalesce(pb.nominal,0), pb.id "
                    + "   from koperasi.pembayaran_hutang_supplier pb "
                    + "   left join library.penyedia s2 on s2.id = pb.supplier "
                    + "   where coalesce(upper(pb.status_dok),'') not like '%BATAL%' "
                    + kondTanggalInline("pb.tanggal", tglMulai, tglSampai)
                    + "   union all "
                    // Dibayar di muka melekat pada fakturnya (bukan dokumen pembayaran tersendiri),
                    // tetapi tetap mengurangi utang -- tanpa baris ini saldo di sini akan berbeda
                    // dengan laporan Saldo Hutang per Supplier.
                    + "   select coalesce(s3.nama,'Tanpa Nama Pemasok'), cast(pf2.tanggal_faktur as date), "
                    + "     coalesce(pf2.nomor_faktur,'-'), 'Dibayar di muka', 0.0, coalesce(i2.dibayar_awal,0), pf2.id "
                    + "   from koperasi.payable_faktur_info i2 "
                    + "   join koperasi.pengadaan_faktur pf2 on pf2.id = i2.pengadaan_faktur "
                    + "   left join library.penyedia s3 on s3.id = pf2.supplier "
                    + "   where coalesce(i2.dibayar_awal,0) > 0 "
                    + kondTanggalInline("pf2.tanggal_faktur", tglMulai, tglSampai)
                    + " ) x order by pemasok asc, tgl asc, urut asc ";
                tipe = new String[]{"text","tgl","text","text","num","num","num"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Referensi","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Kulakan (Kredit)","num"));
                kolom.add(new Kolom("Pembayaran (Debet)","num")); kolom.add(new Kolom("Sisa Utang","num"));

            } else if ("ap_saldo_supplier".equals(r)) {
                judul = "Saldo Hutang per Supplier (Toko)";
                catatan = "Sisa hutang tiap pemasok = nilai faktur kulakan \u2212 dibayar di muka \u2212 pembayaran yang "
                    + "sudah dialokasikan. Sumber: faktur kulakan (koperasi.pengadaan_faktur) dan modul pembayaran "
                    + "hutang supplier. Hanya pemasok bersaldo yang ditampilkan.";
                sql = "select coalesce(s.nama,'(Tanpa Nama)') as pemasok, count(distinct f.id), "
                    + " sum(coalesce(f.total_faktur_manual, coalesce(f.total_hitung_saat_simpan,0))), "
                    + " sum(coalesce(i.dibayar_awal,0)) + coalesce(sum(al.terbayar),0), "
                    + " sum(coalesce(f.total_faktur_manual, coalesce(f.total_hitung_saat_simpan,0))) "
                    + "   - sum(coalesce(i.dibayar_awal,0)) - coalesce(sum(al.terbayar),0) "
                    + " from koperasi.pengadaan_faktur f "
                    + " left join library.penyedia s on s.id = f.supplier "
                    + " left join koperasi.payable_faktur_info i on i.pengadaan_faktur = f.id "
                    + " left join ( select a.pengadaan_faktur as fid, sum(coalesce(a.nominal,0)) as terbayar "
                    + "   from koperasi.alokasi_pembayaran_hutang_supplier a "
                    + "   join koperasi.pembayaran_hutang_supplier p on p.id = a.pembayaran "
                    + "   where coalesce(upper(p.status_dok),'') not like '%BATAL%' group by a.pengadaan_faktur ) al "
                    + "   on al.fid = f.id "
                    + " where 1=1 " + kondToko("f.toko", tokoId, prm)
                    + klausaTanggal("f.tanggal_faktur", tglMulai, tglSampai, prm)
                    + " group by s.nama "
                    + " having sum(coalesce(f.total_faktur_manual, coalesce(f.total_hitung_saat_simpan,0))) "
                    + "   - sum(coalesce(i.dibayar_awal,0)) - coalesce(sum(al.terbayar),0) <> 0 "
                    + " order by 5 desc ";
                tipe = new String[]{"text","num","num","num","num"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Jml Faktur","num"));
                kolom.add(new Kolom("Nilai Faktur","num")); kolom.add(new Kolom("Sudah Dibayar","num"));
                kolom.add(new Kolom("Sisa Hutang","num"));

            } else if ("ap_umur_utang".equals(r)) {
                judul = "Umur Hutang Supplier (Aging)"; grupIdx = 0;
                catatan = "Faktur kulakan yang belum lunas, dikelompokkan per pemasok dan dipilah menurut umur "
                    + "sejak jatuh tempo (faktur tanpa termin dihitung dari tanggal fakturnya).";
                sql = "select pemasok, no_faktur, tgl, jatuh_tempo, sisa, "
                    + " case when hari <= 0 then 'Belum jatuh tempo' when hari <= 30 then '1-30 hari' "
                    + "      when hari <= 60 then '31-60 hari' when hari <= 90 then '61-90 hari' "
                    + "      else '> 90 hari' end as umur "
                    + " from ( select coalesce(s.nama,'(Tanpa Nama)') as pemasok, coalesce(f.nomor_faktur,'-') as no_faktur, "
                    + "   cast(f.tanggal_faktur as date) as tgl, cast(coalesce(i.jatuh_tempo, f.tanggal_faktur) as date) as jatuh_tempo, "
                    + "   coalesce(f.total_faktur_manual, coalesce(f.total_hitung_saat_simpan,0)) - coalesce(i.dibayar_awal,0) "
                    + "     - coalesce(( select sum(coalesce(a.nominal,0)) from koperasi.alokasi_pembayaran_hutang_supplier a "
                    + "        join koperasi.pembayaran_hutang_supplier p on p.id = a.pembayaran "
                    + "        where a.pengadaan_faktur = f.id and coalesce(upper(p.status_dok),'') not like '%BATAL%' ),0) as sisa, "
                    + "   (current_date - cast(coalesce(i.jatuh_tempo, f.tanggal_faktur) as date)) as hari "
                    + "   from koperasi.pengadaan_faktur f "
                    + "   left join library.penyedia s on s.id = f.supplier "
                    + "   left join koperasi.payable_faktur_info i on i.pengadaan_faktur = f.id "
                    + "   where 1=1 " + kondToko("f.toko", tokoId, prm)
                    + klausaTanggal("f.tanggal_faktur", tglMulai, tglSampai, prm)
                    + " ) x where sisa > 0.005 order by pemasok, jatuh_tempo ";
                tipe = new String[]{"text","text","tgl","tgl","num","text"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("No. Faktur","text"));
                kolom.add(new Kolom("Tgl Faktur","tgl")); kolom.add(new Kolom("Jatuh Tempo","tgl"));
                kolom.add(new Kolom("Sisa Hutang","num")); kolom.add(new Kolom("Umur","text"));

            } else if ("ap_pembayaran".equals(r)) {
                judul = "Riwayat Pembayaran Hutang Supplier";
                catatan = "Pembayaran ke pemasok beserta faktur yang dilunasinya, termasuk status posting jurnal "
                    + "(kolom terakhir) sehingga terlihat mana yang belum masuk buku besar.";
                sql = "select cast(p.tanggal as date), coalesce(p.kode_unik,'-'), coalesce(s.nama,'(Tanpa Nama)'), "
                    + " coalesce(p.metode,'-'), coalesce(p.nominal,0), "
                    + " coalesce(( select string_agg(coalesce(f2.nomor_faktur,'#'||f2.id), ', ') "
                    + "    from koperasi.alokasi_pembayaran_hutang_supplier a2 "
                    + "    join koperasi.pengadaan_faktur f2 on f2.id = a2.pengadaan_faktur "
                    + "    where a2.pembayaran = p.id ), '-'), "
                    + " case when p.posting_history is null then 'BELUM diposting' else 'Sudah diposting' end "
                    + " from koperasi.pembayaran_hutang_supplier p "
                    + " left join library.penyedia s on s.id = p.supplier "
                    + " where coalesce(upper(p.status_dok),'') not like '%BATAL%' "
                    + klausaTanggal("p.tanggal", tglMulai, tglSampai, prm)
                    + " order by p.tanggal desc, p.id desc ";
                tipe = new String[]{"tgl","text","text","text","num","text","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Kode","text"));
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Metode","text"));
                kolom.add(new Kolom("Nominal","num")); kolom.add(new Kolom("Faktur Dilunasi","text"));
                kolom.add(new Kolom("Status Jurnal","text"));

            } else if ("ar_penerimaan_customer".equals(r)) {
                judul = "Riwayat Penerimaan Piutang Customer";
                catatan = "Penerimaan pelunasan piutang dari pelanggan (modul Inventory & Sales), termasuk status "
                    + "posting jurnalnya.";
                sql = "select cast(p.tanggal as date), coalesce(p.nomor, coalesce(p.kode_unik,'-')), "
                    + " coalesce(a.nama,'(Tanpa Nama)'), coalesce(p.metode,'-'), coalesce(p.nominal,0), "
                    + " case when p.posting_history is null then 'BELUM diposting' else 'Sudah diposting' end "
                    + " from koperasi.penerimaan_piutang_customer p "
                    + " left join koperasi.anggota_koperasi a on a.id = p.customer "
                    + " where coalesce(upper(p.status_dok),'') not like '%BATAL%' "
                    + klausaTanggal("p.tanggal", tglMulai, tglSampai, prm)
                    + " order by p.tanggal desc, p.id desc ";
                tipe = new String[]{"tgl","text","text","text","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Nomor","text"));
                kolom.add(new Kolom("Pelanggan","text")); kolom.add(new Kolom("Metode","text"));
                kolom.add(new Kolom("Nominal","num")); kolom.add(new Kolom("Status Jurnal","text"));

            } else if ("akn_diagnosa_jurnal_toko".equals(r)) {
                judul = "Diagnosa Jurnal Toko (Dokumen Belum Diposting)";
                catatan = "Jumlah dokumen toko yang belum masuk buku besar per jenis. Selama masih ada yang belum "
                    + "diposting, Neraca dan Laba Rugi berbasis jurnal belum menggambarkan keadaan sebenarnya "
                    + "(mis. Persediaan bisa minus bila HPP terposting tetapi kulakan belum).";
                sql = "select 'Kulakan (pembelian persediaan)', count(*), coalesce(sum(coalesce(pp.totalharga,"
                    + "   coalesce(pp.qty,0)*coalesce(pp.hargabelisatuan,0),0)),0) from koperasi.pengadaan_produk pp"
                    + "   where pp.posting_pembelian is null "
                    + " union all select 'Pembayaran hutang supplier', count(*), coalesce(sum(coalesce(nominal,0)),0)"
                    + "   from koperasi.pembayaran_hutang_supplier where posting_history is null "
                    + " union all select 'Penerimaan piutang customer', count(*), coalesce(sum(coalesce(nominal,0)),0)"
                    + "   from koperasi.penerimaan_piutang_customer where posting_history is null "
                    + " union all select 'Retur pembelian', count(*), coalesce(sum(coalesce(totalnilai,0)),0)"
                    + "   from koperasi.retur_pembelian where posting_history is null "
                    + " union all select 'Retur penjualan', count(*), coalesce(sum(coalesce(totalnilai,0)),0)"
                    + "   from koperasi.retur_penjualan where posting_history is null "
                    + " union all select 'Selisih stok opname', count(*), 0"
                    + "   from koperasi.stok_opname where posting_history is null and coalesce(selisih,0) <> 0 "
                    + " union all select 'Mutasi antar outlet', count(*), 0"
                    + "   from koperasi.mutasi_stok_toko where posting_history is null ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Jenis Dokumen","text")); kolom.add(new Kolom("Belum Diposting","num"));
                kolom.add(new Kolom("Nilai","num"));

            } else if ("psaldo_rincian".equals(r)) {
                judul = "Rincian Penyesuaian Saldo (Opname Voucher)";
                catatan = "Hasil opname saldo voucher/deposit anggota: saldo menurut sistem, saldo yang "
                    + "seharusnya menurut petugas, selisih, dan alasannya. Selisih positif berarti saldo "
                    + "anggota ditambah, negatif berarti dikurangi. Tiap baris punya pasangan mutasi "
                    + "deposit senilai selisihnya, sehingga saldo hasil hitungan selalu cocok.";
                sql = "select cast(p.waktu as date), coalesce(a.kode,''), coalesce(a.nama,'(Tanpa Nama)'), "
                    + " coalesce(p.saldo_sistem,0), coalesce(p.saldo_fisik,0), coalesce(p.selisih,0), "
                    + " coalesce(p.keterangan,''), coalesce(p.oleh,'') "
                    + " from koperasi.penyesuaian_saldo_anggota p "
                    + " left join koperasi.anggota_koperasi a on a.id = p.anggota_koperasi "
                    + " where 1=1 " + klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm)
                    + " order by p.waktu desc, p.id desc ";
                tipe = new String[]{"tgl","text","text","num","num","num","text","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Kode Anggota","text"));
                kolom.add(new Kolom("Anggota","text")); kolom.add(new Kolom("Saldo Sistem","num"));
                kolom.add(new Kolom("Saldo Seharusnya","num")); kolom.add(new Kolom("Selisih","num"));
                kolom.add(new Kolom("Alasan","text")); kolom.add(new Kolom("Oleh","text"));

            } else if ("psaldo_rekap".equals(r)) {
                judul = "Rekap Penyesuaian Saldo per Anggota";
                catatan = "Berapa kali saldo tiap anggota disesuaikan beserta total selisihnya. Anggota "
                    + "yang sering muncul di sini layak ditelusuri: bisa jadi ada pola kesalahan input "
                    + "topup atau pemakaian yang berulang.";
                sql = "select coalesce(a.kode,''), coalesce(a.nama,'(Tanpa Nama)'), count(*), "
                    + " coalesce(sum(case when coalesce(p.selisih,0) > 0 then p.selisih else 0 end),0), "
                    + " coalesce(sum(case when coalesce(p.selisih,0) < 0 then -p.selisih else 0 end),0), "
                    + " coalesce(sum(p.selisih),0) "
                    + " from koperasi.penyesuaian_saldo_anggota p "
                    + " left join koperasi.anggota_koperasi a on a.id = p.anggota_koperasi "
                    + " where 1=1 " + klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm)
                    + " group by a.kode, a.nama "
                    + " order by abs(coalesce(sum(p.selisih),0)) desc ";
                tipe = new String[]{"text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode Anggota","text")); kolom.add(new Kolom("Anggota","text"));
                kolom.add(new Kolom("Jml Penyesuaian","num")); kolom.add(new Kolom("Total Ditambah","num"));
                kolom.add(new Kolom("Total Dikurangi","num")); kolom.add(new Kolom("Selisih Bersih","num"));

            } else if ("akn_aset_tetap".equals(r)) {
                judul = "Daftar Aset Tetap (Nilai Buku)";
                catatan = "Aktiva tetap yang masih aktif (belum dihapus): nilai perolehan, akumulasi penyusutan, "
                    + "dan nilai buku (= perolehan - akumulasi penyusutan).";
                sql = "select ad.nama, cast(ad.tanggalbeli as date), coalesce(ad.hargabeli,0), "
                    + " coalesce((select sum(coalesce(ps.nilaipenyusutan,0)) from asset.penyusutan_asset ps where ps.asset_detail = ad.id),0), "
                    + " coalesce(ad.hargabeli,0) - coalesce((select sum(coalesce(ps.nilaipenyusutan,0)) from asset.penyusutan_asset ps where ps.asset_detail = ad.id),0) "
                    + " from asset.asset_detail ad where ad.penghapusan_master_asset_detail is null "
                    + " order by ad.tanggalbeli asc nulls last, ad.nama asc ";
                tipe = new String[]{"text","tgl","num","num","num"};
                kolom.add(new Kolom("Nama Aset","text")); kolom.add(new Kolom("Tgl Perolehan","tgl"));
                kolom.add(new Kolom("Nilai Perolehan","num")); kolom.add(new Kolom("Akumulasi Penyusutan","num")); kolom.add(new Kolom("Nilai Buku","num"));

            } else if ("gl_rincian".equals(r)) {
                judul = "Rincian Buku Besar (Kas Kantin)";
                catatan = "Buku besar kas sederhana: penjualan = uang masuk, pengadaan = uang keluar.";
                String tkJual = kondToko("h.toko", tokoId, prm) + klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm);
                String tkBeli = kondToko("a.toko", tokoId, prm) + klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm);
                sql = "select x.tgl, x.ket, x.ref, x.masuk, x.keluar, "
                    + " sum(x.masuk - x.keluar) over (order by x.tgl asc, x.ref asc rows between unbounded preceding and current row) as saldo from ( "
                    + "   select h.tanggal_pembayaran as tgl, 'Penjualan' as ket, h.kode as ref, coalesce(h.total_biaya,0) as masuk, 0.0 as keluar "
                    + "   from koperasi.pembelian_anggota_koperasi h where 1=1 " + tkJual
                    + "   union all "
                    + "   select a.waktupengadaan, 'Pembelian/Pengadaan', coalesce(nullif(a.nomorfaktur,''),'-'), 0.0, coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0) "
                    + "   from koperasi.pengadaan_produk a where 1=1 " + tkBeli
                    + " ) x order by x.tgl asc, x.ref asc ";
                tipe = new String[]{"tgl","text","text","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Referensi","text"));
                kolom.add(new Kolom("Masuk (Debit)","num")); kolom.add(new Kolom("Keluar (Kredit)","num")); kolom.add(new Kolom("Saldo","num"));

            } else if ("pos_kasir_harian".equals(r)) { tokoIdCol = "h.toko";
                judul = "Penjualan per Kasir per Hari"; grupIdx = 0;
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select " + KASIR_NOTA + " as kasir, cast(h.tanggal_pembayaran as date), count(distinct h.id), "
                    + " sum(coalesce(h.bayar_tunai,0)), sum(coalesce(h.bayar_non_tunai,0)), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + w + " group by " + KASIR_NOTA + ", cast(h.tanggal_pembayaran as date) order by 1 asc, 2 asc ";
                tipe = new String[]{"text","tgl","num","num","num","num"};
                kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Jml Transaksi","num"));
                kolom.add(new Kolom("Tunai","num")); kolom.add(new Kolom("Non Tunai","num")); kolom.add(new Kolom("Total","num"));

            } else if ("pos_harian".equals(r)) { tokoIdCol = "h.toko";
                judul = "Penjualan Harian (Semua Kasir)";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select cast(h.tanggal_pembayaran as date), count(distinct h.id), sum(coalesce(h.bayar_tunai,0)), "
                    + " sum(coalesce(h.bayar_non_tunai,0)), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h join koperasi.toko t on t.id=h.toko "
                    + w + " group by cast(h.tanggal_pembayaran as date) order by 1 asc ";
                tipe = new String[]{"tgl","num","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Tunai","num"));
                kolom.add(new Kolom("Non Tunai","num")); kolom.add(new Kolom("Total","num"));

            } else if ("beli_faktur".equals(r)) {
                judul = "Faktur Pembelian";
                catatan = "Daftar faktur pembelian (dari pengadaan) per nomor faktur pemasok.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("a.toko", tokoId, prm));
                w.append(klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm));
                sql = "select coalesce(nullif(a.nomorfaktur,''),'(Tanpa No. Faktur)'), max(a.waktupengadaan), coalesce(a.namasupplier,'-'), "
                    + " count(*), sum(coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0)) "
                    + " from koperasi.pengadaan_produk a " + w
                    + " group by a.nomorfaktur, a.namasupplier order by 2 desc ";
                tipe = new String[]{"text","tgl","text","num","num"};
                kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Jml Item","num")); kolom.add(new Kolom("Total Faktur","num"));

            } else if ("mst_pemasok".equals(r)) {
                judul = "Daftar Pemasok / Supplier";
                catatan = "Pemasok yang pernah memasok (dari data pengadaan). Tambah pemasok baru via menu Pengaturan > Penyedia.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("a.toko", tokoId, prm));
                w.append(klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, prm));
                sql = "select coalesce(nullif(a.namasupplier,''),'(Tanpa Pemasok)'), count(*), "
                    + " sum(coalesce(a.totalharga, a.qty*a.hargabelisatuan, 0)), max(a.waktupengadaan) "
                    + " from koperasi.pengadaan_produk a " + w
                    + " group by a.namasupplier order by 3 desc ";
                tipe = new String[]{"text","num","num","tgl"};
                kolom.add(new Kolom("Pemasok","text")); kolom.add(new Kolom("Jml Pengadaan","num")); kolom.add(new Kolom("Total Nilai","num")); kolom.add(new Kolom("Pengadaan Terakhir","tgl"));

            } else if ("mst_pelanggan".equals(r)) {
                judul = "Daftar Pelanggan / Member";
                catatan = "Anggota/pelanggan koperasi. Tambah pelanggan baru via menu e-Kantin > Pengaturan > Anggota.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(a.kode, a.kode_identitas, '-'), coalesce(a.nama,'-'), coalesce(a.kode_identitas,'-'), "
                    + " coalesce(j.nama,'Umum'), coalesce(nullif(a.hp,''), coalesce(a.telp,'-')), case when a.aktif then 'Aktif' else 'Nonaktif' end "
                    + " from koperasi.anggota_koperasi a left join koperasi.jenis_anggota_koperasi j on j.id=a.jenis_anggota_koperasi "
                    + w + " order by a.nama asc ";
                tipe = new String[]{"text","text","text","text","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama","text")); kolom.add(new Kolom("No. Identitas","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Telepon","text")); kolom.add(new Kolom("Status","text"));

            } else if ("sed_stok_harian".equals(r)) {
                judul = "Stok Harian (Nilai Persediaan)";
                catatan = "Posisi stok terkini per produk beserta nilai persediaan (stok x harga modal).";
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(k.nama,'Umum'), coalesce(t.nama,'-'), coalesce(pr.stok,0), coalesce(pr.hargabeli,0), "
                    + " (coalesce(pr.stok,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.produk pr left join koperasi.jenis_produk k on k.id=pr.jenis_produk "
                    + " left join koperasi.toko t on t.id=pr.toko " + w + " order by pr.nama asc ";
                tipe = new String[]{"text","text","text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Harga Modal","num")); kolom.add(new Kolom("Nilai Persediaan","num"));

            } else if ("retur_barang".equals(r)) {
                judul = "Retur Barang";
                tipe = new String[]{"tgl","text","text","num","num","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Total","num")); kolom.add(new Kolom("Keterangan","text"));
                if (!tabelAda(session, "koperasi.retur_barang")) {
                    H.judul = judul; H.catatan = "Belum ada transaksi retur (input via Kulakan > Retur Barang)."; H.grup = -1; H.tipe = tipe;
                    return H; // baris kosong, status 00
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("x.toko", tokoId, prm));
                w.append(klausaTanggal("x.waktu", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select x.waktu, (coalesce(pr.kode,'') || ' - ' || coalesce(pr.nama,'')) as produk, coalesce(t.nama,'-'), "
                    + " coalesce(x.qty,0), coalesce(x.hargasatuan,0), coalesce(x.total, x.qty*x.hargasatuan, 0), coalesce(x.keterangan,'') "
                    + " from koperasi.retur_barang x left join koperasi.produk pr on pr.id=x.produk "
                    + " left join koperasi.toko t on t.id=x.toko "
                    + w + " order by x.waktu desc ";

            } else if ("pakai_bahan".equals(r)) {
                judul = "Pemakaian Bahan Baku"; grupIdx = 0;
                tipe = new String[]{"text","tgl","num","text"};
                kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Keterangan","text"));
                if (!tabelAda(session, "koperasi.pemakaian_bahan_baku")) {
                    H.judul = judul; H.catatan = "Belum ada pemakaian (input via Kulakan > Pemakaian Bahan Baku)."; H.grup = 0; H.tipe = tipe;
                    return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("x.toko", tokoId, prm));
                w.append(klausaTanggal("x.waktu", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select (coalesce(pr.kode,'') || ' - ' || coalesce(pr.nama,'')) as produk, x.waktu, coalesce(x.qty,0), coalesce(x.keterangan,'') "
                    + " from koperasi.pemakaian_bahan_baku x left join koperasi.produk pr on pr.id=x.produk "
                    + w + " order by pr.nama asc, x.waktu desc ";

            } else if (r != null && r.startsWith("wh_") && !tabelAda(session, "asset.mutasi_lokasi")) {
                // Gerbang bersama: semua laporan pergudangan per-lokasi butuh tabel ledger asset.mutasi_lokasi.
                H.judul = "Laporan Pergudangan (Stok per Lokasi)";
                H.catatan = "Fitur stok per-lokasi belum aktif. Restart aplikasi setelah deploy agar tabel mutasi_lokasi terbentuk, lalu catat lewat menu Mutasi Gudang.";
                H.grup = -1; H.tipe = new String[]{"text"}; H.kolom.add(new Kolom("Info","text"));
                return H;

            } else if ("wh_stok_kini".equals(r)) {
                judul = "Stok Saat Ini (per Lokasi)"; grupIdx = 0;
                catatan = "Posisi stok tiap lokasi/gudang dari catatan Mutasi Gudang, beserta nilai (stok x harga modal).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaSampai("m.tanggal", tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select l.nama, coalesce(jl.nama,'-'), pr.kode, pr.nama, sum(coalesce(m.qty,0)), coalesce(pr.hargabeli,0), sum(coalesce(m.qty,0))*coalesce(pr.hargabeli,0) "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi left join asset.jenis_lokasi jl on jl.id=l.jenis_lokasi "
                    + " join koperasi.produk pr on pr.id=m.produk " + w
                    + " group by l.nama, jl.nama, pr.kode, pr.nama, pr.hargabeli having sum(coalesce(m.qty,0))<>0 order by l.nama asc, pr.nama asc ";
                tipe = new String[]{"text","text","text","text","num","num","num"};
                kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Jenis","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Harga Modal","num")); kolom.add(new Kolom("Nilai","num"));

            } else if ("wh_valuasi".equals(r)) {
                judul = "Valuasi Persediaan per Lokasi";
                catatan = "Nilai persediaan (stok x harga modal) diringkas per lokasi/gudang.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaSampai("m.tanggal", tglSampai, prm));
                sql = "select l.nama, coalesce(jl.nama,'-'), count(distinct pr.id), sum(coalesce(m.qty,0)), sum(coalesce(m.qty,0)*coalesce(pr.hargabeli,0)) "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi left join asset.jenis_lokasi jl on jl.id=l.jenis_lokasi "
                    + " join koperasi.produk pr on pr.id=m.produk " + w
                    + " group by l.nama, jl.nama having sum(coalesce(m.qty,0))<>0 order by 5 desc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Jenis","text")); kolom.add(new Kolom("Jml Item","num")); kolom.add(new Kolom("Total Stok","num")); kolom.add(new Kolom("Nilai Persediaan","num"));

            } else if ("wh_stok_minus".equals(r)) {
                judul = "Stok Minus (per Lokasi)";
                catatan = "Barang berstok negatif — indikasi salah input atau penjualan tanpa stok.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select l.nama, pr.kode, pr.nama, sum(coalesce(m.qty,0)) "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi join koperasi.produk pr on pr.id=m.produk " + w
                    + " group by l.nama, pr.kode, pr.nama having sum(coalesce(m.qty,0)) < 0 order by 4 asc ";
                tipe = new String[]{"text","text","text","num"};
                kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Stok","num"));

            } else if ("wh_stok_kosong".equals(r)) {
                judul = "Stok Kosong (per Lokasi)";
                catatan = "Barang yang pernah bergerak namun stoknya kini 0 di lokasi tersebut.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select l.nama, pr.kode, pr.nama, max(m.tanggal) "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi join koperasi.produk pr on pr.id=m.produk " + w
                    + " group by l.nama, pr.kode, pr.nama having sum(coalesce(m.qty,0)) = 0 order by 4 desc ";
                tipe = new String[]{"text","text","text","tgl"};
                kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Terakhir Bergerak","tgl"));

            } else if ("stok_per_tanggal".equals(r)) {
                // "Stok Barang per Tanggal" -- saldo stok SEPERTI PADA tanggal acuan (parameter
                // tglSampai). Rumusnya MENCERMINKAN PERSIS StokKantinUtil.formulaStokSql (7 suku,
                // sumber kebenaran stok berjalan), hanya ditambah batas tanggal per tabel sumber
                // (kolom tanggalnya berbeda-beda: waktupengadaan / waktuopname / waktu). Karena
                // itu, memilih tanggal HARI INI menghasilkan angka yang sama dengan kolom stok di
                // layar Produk -- laporan ini rekonsiliasi, bukan rumus tandingan.
                judul = "Stok Barang per Tanggal"; grupIdx = -1;
                catatan = "Saldo stok tiap barang pada tanggal acuan (kolom 'Per Tanggal'). "
                        + "Kosongkan tanggal untuk memakai stok terkini.";
                String cut = ada(tglSampai) ? " and cast(%s as date) <= cast(:tglSampai as date) " : " and 1=1 ";
                if (ada(tglSampai)) { prm.put("tglSampai", tglSampai.trim()); }
                String fStok =
                      "  coalesce((select sum(x.qty) from koperasi.pengadaan_produk x where x.produk=pr.id"
                    + String.format(cut, "x.waktupengadaan") + "),0)"
                    + " + coalesce((select sum(x.selisih) from koperasi.stok_opname x where x.produk=pr.id"
                    + String.format(cut, "x.waktuopname") + "),0)"
                    + " - coalesce((select sum(x.qty) from koperasi.pembelian x where x.produk=pr.id"
                    + String.format(cut, "x.waktu") + "),0)"
                    + " - coalesce((select sum(x.qty) from koperasi.pemakaian_bahan_baku x where x.produk=pr.id"
                    + String.format(cut, "x.waktu") + "),0)"
                    + " + coalesce((select sum(x.qty) from koperasi.retur_penjualan x where x.produk=pr.id"
                    + " and x.kembalikan_ke_stok = true" + String.format(cut, "x.waktu") + "),0)"
                    + " + coalesce((select sum(x.qty) from koperasi.mutasi_stok_toko x where x.produk_tujuan=pr.id"
                    + String.format(cut, "x.waktu") + "),0)"
                    + " - coalesce((select sum(x.qty) from koperasi.mutasi_stok_toko x where x.produk_asal=pr.id"
                    + String.format(cut, "x.waktu") + "),0)"
                    + " - coalesce((select sum(x.qty) from koperasi.retur_pembelian x where x.produk=pr.id"
                    + String.format(cut, "x.waktu") + "),0)"
                    // Produksi (Fase 0 dok. 49): ikut dipotong tanggal spt suku lain.
                    + " + coalesce((select sum(coalesce(x.qty_masuk,0)-coalesce(x.qty_keluar,0)) from koperasi.mutasi_stok_produksi x where x.produk=pr.id"
                    + String.format(cut, "x.waktu") + "),0)";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp or lower(coalesce(pr.barcode,'')) like :qp) "); prm.put("qp", qp); }
                if (jenisProdukId != null) { w.append(" and pr.jenis_produk = :jenisProdukId "); prm.put("jenisProdukId", jenisProdukId); }
                if (grupProdukId != null) { w.append(" and pr.grup_produk = :grupProdukId "); prm.put("grupProdukId", grupProdukId); }
                if (hanyaAktif) { w.append(" and coalesce(pr.aktif,true) = true "); }
                if (hanyaStokTidakNol) { w.append(" "); }
                sql = "select s.* from ( select coalesce(pr.kode,'') as kode, coalesce(pr.barcode,'') as barcode,"
                    + " pr.nama as nama, coalesce(jp.nama,'-') as kategori, coalesce(t.nama,'-') as toko,"
                    // produk.satuan adalah FK bigint. Jangan COALESCE FK tersebut dengan
                    // string kosong; ambil nama satuan dari tabel referensinya.
                    + " coalesce(nullif(trim(sp.nama),''),'(Belum diatur)') as satuan, (" + fStok + ") as stok,"
                    + " coalesce(pr.hargabeli,0) as hargabeli, coalesce(pr.hargajual,0) as hargajual"
                    + " from koperasi.produk pr"
                    + " left join koperasi.satuan_produk sp on sp.id = pr.satuan"
                    + " left join koperasi.jenis_produk jp on jp.id = pr.jenis_produk"
                    + " left join koperasi.toko t on t.id = pr.toko" + w + " ) s"
                    + (hanyaStokTidakNol ? " where s.stok <> 0 " : "")
                    + " order by s.toko asc, s.nama asc";
                sql = sql.replace("select s.* from", "select s.kode, s.barcode, s.nama, s.kategori, s.toko, s.satuan,"
                    + " s.stok, s.hargabeli, s.hargajual, (s.stok * s.hargabeli) as nilai from");
                tipe = new String[]{"text","text","text","text","text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Barcode","text"));
                kolom.add(new Kolom("Nama Barang","text")); kolom.add(new Kolom("Kategori","text"));
                kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Satuan","text"));
                kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Harga Beli","num"));
                kolom.add(new Kolom("Harga Jual","num")); kolom.add(new Kolom("Nilai Stok","num"));
            } else if ("wh_kartu_stok".equals(r)) {
                judul = "Kartu Stok Barang"; grupIdx = 0;
                catatan = "Riwayat pergerakan tiap barang (masuk, keluar, transfer, koreksi) beserta saldo berjalan.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select (coalesce(pr.kode,'') || ' - ' || coalesce(pr.nama,'')) as produk, m.tanggal, l.nama, m.jenis, "
                    + " (case when m.qty>0 then m.qty else 0 end), (case when m.qty<0 then -m.qty else 0 end), "
                    + " sum(coalesce(m.qty,0)) over (partition by m.produk order by m.tanggal asc, m.id asc rows between unbounded preceding and current row), "
                    + " coalesce(m.referensi,''), coalesce(m.oleh,'') "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi join koperasi.produk pr on pr.id=m.produk " + w
                    + " order by pr.nama asc, m.tanggal asc, m.id asc ";
                tipe = new String[]{"text","tgl","text","text","num","num","num","text","text"};
                kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Jenis","text"));
                kolom.add(new Kolom("Masuk","num")); kolom.add(new Kolom("Keluar","num")); kolom.add(new Kolom("Saldo","num")); kolom.add(new Kolom("Referensi","text")); kolom.add(new Kolom("Petugas","text"));

            } else if ("wh_mutasi_harian".equals(r)) {
                judul = "Mutasi Stok Harian";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                sql = "select cast(m.tanggal as date), sum(case when m.qty>0 then m.qty else 0 end), sum(case when m.qty<0 then -m.qty else 0 end), sum(coalesce(m.qty,0)) "
                    + " from asset.mutasi_lokasi m " + w + " group by cast(m.tanggal as date) order by 1 desc ";
                tipe = new String[]{"tgl","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Total Masuk","num")); kolom.add(new Kolom("Total Keluar","num")); kolom.add(new Kolom("Perubahan Bersih","num"));

            } else if ("wh_mutasi_barang".equals(r)) {
                judul = "Mutasi Stok per Barang";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, sum(case when m.qty>0 then m.qty else 0 end), sum(case when m.qty<0 then -m.qty else 0 end), sum(coalesce(m.qty,0)) "
                    + " from asset.mutasi_lokasi m join koperasi.produk pr on pr.id=m.produk " + w
                    + " group by pr.kode, pr.nama order by pr.nama asc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Masuk","num")); kolom.add(new Kolom("Keluar","num")); kolom.add(new Kolom("Perubahan Bersih","num"));

            } else if ("wh_mutasi_petugas".equals(r)) {
                judul = "Mutasi Stok per Petugas";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                sql = "select coalesce(nullif(m.oleh,''),'-'), count(*), sum(case when m.qty>0 then m.qty else 0 end), sum(case when m.qty<0 then -m.qty else 0 end) "
                    + " from asset.mutasi_lokasi m " + w + " group by m.oleh order by 2 desc ";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Petugas","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Total Masuk","num")); kolom.add(new Kolom("Total Keluar","num"));

            } else if ("wh_mutasi_dokumen".equals(r)) {
                judul = "Mutasi Stok per Dokumen (Referensi)";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                sql = "select coalesce(nullif(m.referensi,''),'(tanpa referensi)'), max(m.jenis), count(*), max(m.tanggal), sum(coalesce(m.qty,0)) "
                    + " from asset.mutasi_lokasi m " + w + " group by m.referensi order by 4 desc ";
                tipe = new String[]{"text","text","num","tgl","num"};
                kolom.add(new Kolom("Referensi","text")); kolom.add(new Kolom("Jenis","text")); kolom.add(new Kolom("Jml Baris","num")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Perubahan Bersih","num"));

            } else if ("wh_barang_masuk".equals(r)) {
                judul = "Barang Masuk (per Lokasi)";
                StringBuilder w = new StringBuilder(" where m.qty > 0 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select m.tanggal, l.nama, m.jenis, pr.kode, pr.nama, coalesce(m.qty,0), coalesce(m.harga_satuan,0), coalesce(m.qty,0)*coalesce(m.harga_satuan,0), coalesce(m.oleh,'') "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi join koperasi.produk pr on pr.id=m.produk " + w
                    + " order by m.tanggal desc, m.id desc ";
                tipe = new String[]{"tgl","text","text","text","text","num","num","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Jenis","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Petugas","text"));

            } else if ("wh_barang_keluar".equals(r)) {
                judul = "Barang Keluar (per Lokasi)";
                StringBuilder w = new StringBuilder(" where m.qty < 0 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select m.tanggal, l.nama, m.jenis, pr.kode, pr.nama, -coalesce(m.qty,0), coalesce(m.harga_satuan,0), (-coalesce(m.qty,0))*coalesce(m.harga_satuan,0), coalesce(m.oleh,'') "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi join koperasi.produk pr on pr.id=m.produk " + w
                    + " order by m.tanggal desc, m.id desc ";
                tipe = new String[]{"tgl","text","text","text","text","num","num","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Lokasi","text")); kolom.add(new Kolom("Jenis","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Petugas","text"));

            } else if ("wh_transfer".equals(r)) {
                judul = "Transfer Stok Antar Lokasi";
                catatan = "Perpindahan barang dari satu lokasi ke lokasi lain (baris sisi keluar/asal).";
                StringBuilder w = new StringBuilder(" where m.jenis = 'TRANSFER' and m.qty < 0 ");
                w.append(klausaTanggal("m.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select m.tanggal, l.nama, coalesce(lp.nama,'-'), pr.kode, pr.nama, abs(coalesce(m.qty,0)), coalesce(nullif(m.referensi,''),'-'), coalesce(m.oleh,'') "
                    + " from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi left join asset.lokasi lp on lp.id=m.lokasi_pasangan "
                    + " join koperasi.produk pr on pr.id=m.produk " + w
                    + " order by m.tanggal desc, m.id desc ";
                tipe = new String[]{"tgl","text","text","text","text","num","text","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Dari Lokasi","text")); kolom.add(new Kolom("Ke Lokasi","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Referensi","text")); kolom.add(new Kolom("Petugas","text"));

            } else if ("pnj_detail_transaksi".equals(r)) {
                judul = "Detail Transaksi Penjualan"; grupIdx = 0;
                catatan = "Rincian item tiap transaksi POS (dikelompokkan per nomor faktur).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,p.kode,'')) like :qp"
                        + " or lower(coalesce(pr.nama,p.nama,'')) like :qp) "); prm.put("qp", qp); }
                if (qk != null) { w.append(" and lower(coalesce(h.kasir_login_nama,'')) like :qk "); prm.put("qk", qk); }
                sql = "select coalesce(h.kode,'-') as faktur, p.waktu, " + KASIR_NOTA + ", coalesce(a.nama,'Umum'), "
                    + " " + LABEL_PRODUK_ITEM + ", coalesce(p.qty,0), " + LABEL_SATUAN_JUAL
                    + ", coalesce(p.hargasatuan, pr.hargajual, 0), coalesce(p.diskon,0), " + OMZET
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.pembelian_anggota_koperasi h on h.id=p.pembelian_anggota_koperasi "
                    + " left join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + JOIN_SATUAN_ITEM
                    + w + " order by p.waktu desc, faktur asc ";
                tipe = new String[]{"text","tgl","text","text","text","num","text","num","num","num"};
                kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Waktu","tgl")); kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Pembeli","text"));
                kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Satuan Jual","text"));
                kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Diskon","num")); kolom.add(new Kolom("Total","num"));

            } else if ("pnj_per_jam".equals(r)) { tokoIdCol = "h.toko";
                judul = "Penjualan per Jam Operasional";
                catatan = "Omzet & jumlah transaksi tiap jam — untuk menentukan jam ramai dan penjadwalan kasir.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select (to_char(h.tanggal_pembayaran,'HH24') || ':00'), count(distinct h.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h " + w
                    + " group by to_char(h.tanggal_pembayaran,'HH24') order by 1 asc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Jam","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Omzet","num"));

            } else if ("pnj_per_kategori".equals(r)) { tokoIdCol = "p.toko";
                judul = "Penjualan per Kategori Produk";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                sql = "select coalesce(k.nama,'Umum'), sum(coalesce(p.qty,0)), sum(" + OMZET + ") "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w
                    + " group by k.nama order by 3 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Qty Terjual","num")); kolom.add(new Kolom("Total Penjualan","num"));

            } else if ("margin_produk".equals(r)) { tokoIdCol = "p.toko";
                judul = "Margin per Produk/Menu";
                catatan = "Laba per produk = penjualan - HPP (qty x harga modal).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", sum(coalesce(p.qty,0)), sum(" + OMZET + "), sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0)), "
                    + " (sum(" + OMZET + ") - sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0))) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w
                    + " group by 1, 2 order by 6 desc ";
                tipe = new String[]{"text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Qty Terjual","num")); kolom.add(new Kolom("Penjualan","num")); kolom.add(new Kolom("HPP","num")); kolom.add(new Kolom("Laba","num"));

            } else if ("margin_kategori".equals(r)) { tokoIdCol = "p.toko";
                judul = "Margin per Kategori";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                sql = "select coalesce(k.nama,'Umum'), sum(" + OMZET + "), sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0)), "
                    + " (sum(" + OMZET + ") - sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0))) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w
                    + " group by k.nama order by 4 desc ";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Penjualan","num")); kolom.add(new Kolom("HPP","num")); kolom.add(new Kolom("Laba Kotor","num"));

            } else if ("laba_kotor_harian".equals(r)) { tokoIdCol = "p.toko";
                judul = "Laba Kotor Harian";
                catatan = "Penjualan dikurangi HPP per hari (berbasis harga modal produk).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                sql = "select cast(p.waktu as date), sum(" + OMZET + "), sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0)), "
                    + " (sum(" + OMZET + ") - sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0))) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w
                    + " group by cast(p.waktu as date) order by 1 desc ";
                tipe = new String[]{"tgl","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Penjualan","num")); kolom.add(new Kolom("HPP","num")); kolom.add(new Kolom("Laba Kotor","num"));

            } else if ("produk_bawah_modal".equals(r)) { tokoIdCol = "p.toko";
                judul = "Produk Dijual di Bawah Modal";
                catatan = "Transaksi dengan harga jual lebih rendah dari harga modal (indikasi rugi / diskon berlebih).";
                StringBuilder w = new StringBuilder(" where coalesce(p.hargasatuan, pr.hargajual, 0) < coalesce(pr.hargabeli,0) ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select p.waktu, coalesce(h.kode,'-'), pr.kode, pr.nama, coalesce(p.hargasatuan, pr.hargajual, 0), coalesce(pr.hargabeli,0), "
                    + " (coalesce(p.hargasatuan, pr.hargajual, 0) - coalesce(pr.hargabeli,0)) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk "
                    + " left join koperasi.pembelian_anggota_koperasi h on h.id=p.pembelian_anggota_koperasi " + w
                    + " order by p.waktu desc ";
                tipe = new String[]{"tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Faktur","text")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text"));
                kolom.add(new Kolom("Harga Jual","num")); kolom.add(new Kolom("Harga Modal","num")); kolom.add(new Kolom("Selisih","num"));

            } else if ("slow_moving".equals(r)) { tokoIdCol = "p.toko";
                judul = "Barang Slow Moving";
                catatan = "Produk yang tetap terjual namun paling lambat pergerakannya (qty terkecil) pada periode.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and p.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", sum(coalesce(p.qty,0)), sum(" + OMZET + ") "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w
                    + " group by 1, 2 having sum(coalesce(p.qty,0)) > 0 order by 3 asc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Qty Terjual","num")); kolom.add(new Kolom("Total Penjualan","num"));

            } else if ("harga_beli_terakhir".equals(r)) {
                judul = "Harga Beli Terakhir per Produk";
                catatan = "Harga modal terakhir tiap produk dari data pengadaan (untuk menjaga akurasi HPP).";
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(pr.hargabeli,0), coalesce(lb.harga,0), lb.tgl "
                    + " from koperasi.produk pr left join ( "
                    + "   select distinct on (produk) produk, coalesce(hargabelisatuan,0) as harga, waktupengadaan as tgl "
                    + "   from koperasi.pengadaan_produk order by produk, waktupengadaan desc "
                    + " ) lb on lb.produk = pr.id " + w + " order by pr.nama asc ";
                tipe = new String[]{"text","text","num","num","tgl"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Harga Modal (Master)","num")); kolom.add(new Kolom("Harga Beli Terakhir","num")); kolom.add(new Kolom("Tgl Pengadaan","tgl"));

            } else if ("stok_per_kategori".equals(r)) { tokoIdCol = "pr.toko";
                judul = "Stok per Kategori";
                StringBuilder w = new StringBuilder(" where pr.aktif = true ");
                w.append(kondToko("pr.toko", tokoId, prm));
                sql = "select coalesce(k.nama,'Umum'), count(*), sum(coalesce(pr.stok,0)), sum(coalesce(pr.stok,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.produk pr left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w
                    + " group by k.nama order by 4 desc ";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Jml Item","num")); kolom.add(new Kolom("Total Stok","num")); kolom.add(new Kolom("Nilai Persediaan","num"));

            } else if ("mgr_pertumbuhan".equals(r)) { tokoIdCol = "h.toko";
                judul = "Pertumbuhan Penjualan (Bulanan)";
                catatan = "Tren omzet & jumlah transaksi per bulan untuk membaca perkembangan usaha.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select to_char(h.tanggal_pembayaran,'YYYY-MM'), count(distinct h.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h " + w
                    + " group by to_char(h.tanggal_pembayaran,'YYYY-MM') order by 1 asc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Bulan","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Omzet","num"));

            } else if ("beli_pr".equals(r)) {
                judul = "Permintaan Pengadaan (PR)";
                catatan = "Daftar Permintaan Pengadaan (Purchase Requisition) dari modul Pengadaan Aset.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("p.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select p.kode, p.tanggal_pembuatan, p.tanggal_persetujuan, coalesce(p.keterangan,''), coalesce(p.nilai,0), "
                    + " (case when p.tanggal_ditolak is not null then 'Ditolak' when p.tanggal_persetujuan is not null then 'Disetujui' else 'Diajukan' end) "
                    + " from asset.permintaan_pengadaan_master_asset p " + w + " order by p.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","tgl","text","num","text"};
                kolom.add(new Kolom("Kode PR","text")); kolom.add(new Kolom("Tgl Dibuat","tgl")); kolom.add(new Kolom("Tgl Setuju","tgl")); kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Status","text"));

            } else if ("beli_po".equals(r)) {
                judul = "Pesanan Pembelian (PO)";
                catatan = "Purchase Order ke vendor/penyedia beserta status pembayaran.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("po.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select po.kode, po.tanggal_pembuatan, coalesce(v.nama,'-'), coalesce(po.nilai,0), coalesce(po.dibayar,0), (coalesce(po.nilai,0)-coalesce(po.dibayar,0)), "
                    + " (case when po.lunas then 'Lunas' when po.tanggal_persetujuan is not null then 'Disetujui' else 'Draft' end) "
                    + " from asset.pemesanan_pengadaan_master_asset po left join asset.penyedia_asset v on v.id=po.penyedia " + w + " order by po.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","text","num","num","num","text"};
                kolom.add(new Kolom("Kode PO","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Vendor","text")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Dibayar","num")); kolom.add(new Kolom("Sisa","num")); kolom.add(new Kolom("Status","text"));

            } else if ("beli_bast".equals(r)) {
                judul = "Penerimaan Barang (BAST)";
                catatan = "Berita Acara Serah Terima / penerimaan barang dari vendor.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("b.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select b.kode, b.tanggal_pembuatan, coalesce(v.nama,'-'), coalesce(b.kodetagihan,'-'), b.tanggaltagihan, coalesce(b.nilai,0), "
                    + " (case when b.tanggal_persetujuan is not null then 'Disetujui' else 'Draft' end) "
                    + " from asset.penerimaan_pengadaan_master_asset b left join asset.penyedia_asset v on v.id=b.penyedia " + w + " order by b.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","text","text","tgl","num","text"};
                kolom.add(new Kolom("Kode BAST","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Vendor","text")); kolom.add(new Kolom("No. Tagihan","text")); kolom.add(new Kolom("Tgl Tagihan","tgl")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Status","text"));

            } else if ("beli_tagihan".equals(r)) {
                judul = "Terima Tagihan / Pembayaran Vendor";
                catatan = "Tagihan vendor beserta nilai dibayar, pajak, dan sisa.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("t.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select t.kode, t.tanggal_pembuatan, coalesce(v.nama,'-'), coalesce(t.nilaitagihan,0), coalesce(t.nilaidibayar,0), coalesce(t.totalpajak,0), (coalesce(t.nilaitagihan,0)-coalesce(t.nilaidibayar,0)) "
                    + " from asset.pembayaran_pengadaan_master_asset t left join asset.penyedia_asset v on v.id=t.penyedia " + w + " order by t.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Vendor","text")); kolom.add(new Kolom("Nilai Tagihan","num")); kolom.add(new Kolom("Dibayar","num")); kolom.add(new Kolom("Pajak","num")); kolom.add(new Kolom("Sisa","num"));

            } else if ("beli_utang_vendor".equals(r)) {
                judul = "Utang Vendor (Tagihan Belum Lunas)";
                catatan = "Sisa kewajiban ke tiap vendor dari tagihan yang belum dibayar penuh.";
                StringBuilder w = new StringBuilder(" where coalesce(t.nilaidibayar,0) < coalesce(t.nilaitagihan,0) ");
                w.append(klausaTanggal("t.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select coalesce(v.nama,'-'), count(*), sum(coalesce(t.nilaitagihan,0)), sum(coalesce(t.nilaidibayar,0)), sum(coalesce(t.nilaitagihan,0)-coalesce(t.nilaidibayar,0)) "
                    + " from asset.pembayaran_pengadaan_master_asset t left join asset.penyedia_asset v on v.id=t.penyedia " + w + " group by v.nama order by 5 desc ";
                tipe = new String[]{"text","num","num","num","num"};
                kolom.add(new Kolom("Vendor","text")); kolom.add(new Kolom("Jml Tagihan","num")); kolom.add(new Kolom("Total Tagihan","num")); kolom.add(new Kolom("Dibayar","num")); kolom.add(new Kolom("Sisa Utang","num"));

            } else if ("beli_per_vendor".equals(r)) {
                judul = "Pembelian per Vendor (PO)";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("po.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select coalesce(v.nama,'-'), count(*), sum(coalesce(po.nilai,0)), sum(coalesce(po.dibayar,0)), max(po.tanggal_pembuatan) "
                    + " from asset.pemesanan_pengadaan_master_asset po left join asset.penyedia_asset v on v.id=po.penyedia " + w + " group by v.nama order by 3 desc ";
                tipe = new String[]{"text","num","num","num","tgl"};
                kolom.add(new Kolom("Vendor","text")); kolom.add(new Kolom("Jml PO","num")); kolom.add(new Kolom("Total Nilai","num")); kolom.add(new Kolom("Dibayar","num")); kolom.add(new Kolom("PO Terakhir","tgl"));

            } else if ("beli_retur_pengadaan".equals(r)) {
                judul = "Retur Pengadaan (ke Vendor)";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("r.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select r.kode, r.tanggal_pembuatan, coalesce(v.nama,'-'), coalesce(r.keterangan,''), "
                    + " (case when r.tanggal_persetujuan is not null then 'Disetujui' else 'Draft' end) "
                    + " from asset.retur_pengadaan_master_asset r left join asset.penyedia_asset v on v.id=r.penyedia " + w + " order by r.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","text","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Vendor","text")); kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Status","text"));

            } else if ("mst_vendor".equals(r)) {
                judul = "Daftar Vendor / Penyedia";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (qc != null) { w.append(" and (lower(coalesce(v.nama,'')) like :qc or lower(coalesce(v.kode,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select v.kode, v.nama, coalesce(v.pemilik,'-'), coalesce(v.telp,'-'), coalesce(v.email,'-'), (case when v.aktif then 'Aktif' else 'Nonaktif' end) "
                    + " from asset.penyedia_asset v " + w + " order by v.nama asc ";
                tipe = new String[]{"text","text","text","text","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Vendor","text")); kolom.add(new Kolom("Pemilik","text")); kolom.add(new Kolom("Telepon","text")); kolom.add(new Kolom("Email","text")); kolom.add(new Kolom("Status","text"));

            } else if ("saldo_deposit".equals(r)) {
                judul = "Deposit / Top Up Saldo";
                catatan = "Daftar setoran/top up saldo (dari modul Deposit).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("d.waktu", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and lower(coalesce(d.nama,'')) like :qc "); prm.put("qc", qc); }
                sql = "select d.waktu, coalesce(d.nama,'-'), coalesce(d.nominal,0), coalesce(d.keterangan,'') "
                    + " from public.deposit d " + w + " order by d.waktu desc ";
                tipe = new String[]{"tgl","text","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Nama","text")); kolom.add(new Kolom("Nominal","num")); kolom.add(new Kolom("Keterangan","text"));

            } else if ("saldo_deposit_rekap".equals(r)) {
                judul = "Rekap Deposit per Nama";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("d.waktu", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and lower(coalesce(d.nama,'')) like :qc "); prm.put("qc", qc); }
                sql = "select coalesce(d.nama,'-'), count(*), sum(coalesce(d.nominal,0)), max(d.waktu) "
                    + " from public.deposit d " + w + " group by d.nama order by 3 desc ";
                tipe = new String[]{"text","num","num","tgl"};
                kolom.add(new Kolom("Nama","text")); kolom.add(new Kolom("Jml Top Up","num")); kolom.add(new Kolom("Total Nominal","num")); kolom.add(new Kolom("Terakhir","tgl"));

            } else if ("beli_pr_rinci".equals(r)) {
                judul = "Rincian Permintaan Pengadaan (PR) per Item"; grupIdx = 0;
                catatan = "Item barang tiap dokumen PR (dikelompokkan per nomor PR).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("p.tanggal_pembuatan", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(ma.kode,'')) like :qp or lower(coalesce(ma.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(p.kode,'-') as pr, coalesce(ma.kode,'-'), coalesce(ma.nama,'-'), coalesce(d.jumlah,0), coalesce(d.hargabeli,0), coalesce(d.hargatotal, d.jumlah*d.hargabeli, 0), coalesce(d.jumlahdatang,0) "
                    + " from asset.permintaan_pengadaan_master_asset_detail d "
                    + " join asset.permintaan_pengadaan_master_asset p on p.id = d.permintaan_pengadaan_master_asset "
                    + " left join asset.master_asset ma on ma.id = d.masterasset " + w
                    + " order by p.tanggal_pembuatan desc, pr asc ";
                tipe = new String[]{"text","text","text","num","num","num","num"};
                kolom.add(new Kolom("No. PR","text")); kolom.add(new Kolom("Kode Item","text")); kolom.add(new Kolom("Nama Item","text")); kolom.add(new Kolom("Jumlah","num"));
                kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Total","num")); kolom.add(new Kolom("Sudah Datang","num"));

            } else if ("beli_po_rinci".equals(r)) {
                judul = "Rincian Pesanan Pembelian (PO) per Item"; grupIdx = 0;
                catatan = "Item barang tiap dokumen PO ke vendor (dikelompokkan per nomor PO).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("po.tanggal_pembuatan", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(ma.kode,'')) like :qp or lower(coalesce(ma.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(po.kode,'-') as po, coalesce(ma.kode,'-'), coalesce(ma.nama,'-'), coalesce(d.jumlah,0), coalesce(d.hargabeli,0), coalesce(d.hargapotongan,0), coalesce(d.hargatotal, d.jumlah*d.hargabeli, 0) "
                    + " from asset.pemesanan_pengadaan_master_asset_detail d "
                    + " join asset.pemesanan_pengadaan_master_asset po on po.id = d.pemesanan_pengadaan_master_asset "
                    + " left join asset.master_asset ma on ma.id = d.masterasset " + w
                    + " order by po.tanggal_pembuatan desc, po asc ";
                tipe = new String[]{"text","text","text","num","num","num","num"};
                kolom.add(new Kolom("No. PO","text")); kolom.add(new Kolom("Kode Item","text")); kolom.add(new Kolom("Nama Item","text")); kolom.add(new Kolom("Jumlah","num"));
                kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Potongan","num")); kolom.add(new Kolom("Total","num"));

            } else if ("beli_bast_rinci".equals(r)) {
                judul = "Rincian Penerimaan (BAST) per Item"; grupIdx = 0;
                catatan = "Item barang tiap dokumen penerimaan/BAST (dikelompokkan per nomor BAST).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("b.tanggal_pembuatan", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(ma.kode,'')) like :qp or lower(coalesce(ma.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(b.kode,'-') as bast, coalesce(ma.kode,'-'), coalesce(ma.nama,'-'), coalesce(d.hargabeli,0), coalesce(d.hargapotongan,0), coalesce(d.hargatotal,0) "
                    + " from asset.penerimaan_pengadaan_master_asset_detail d "
                    + " join asset.penerimaan_pengadaan_master_asset b on b.id = d.penerimaan_pengadaan_master_asset "
                    + " left join asset.master_asset ma on ma.id = d.masterasset " + w
                    + " order by b.tanggal_pembuatan desc, bast asc ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("No. BAST","text")); kolom.add(new Kolom("Kode Item","text")); kolom.add(new Kolom("Nama Item","text")); kolom.add(new Kolom("Harga Beli","num")); kolom.add(new Kolom("Potongan","num")); kolom.add(new Kolom("Total","num"));

            } else if ("resep_bom".equals(r)) {
                judul = "Resep / BOM per Menu"; grupIdx = 0;
                catatan = "Komposisi bahan baku tiap menu (dari data Bahan Baku produk) + subtotal biaya per bahan.";
                StringBuilder w = new StringBuilder(" where p.bahanbaku is not null and trim(p.bahanbaku) not in ('','[]') and trim(p.bahanbaku) like '[%' ");
                w.append(kondToko("p.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(coalesce(p.kode,'')) like :qp or lower(coalesce(p.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select p.nama, coalesce(b->>'nama','-'), "
                    + " cast(coalesce(nullif(b->>'qty',''),'0') as numeric), cast(coalesce(nullif(b->>'harga',''),'0') as numeric), "
                    + " (cast(coalesce(nullif(b->>'qty',''),'0') as numeric) * cast(coalesce(nullif(b->>'harga',''),'0') as numeric)) "
                    + " from koperasi.produk p, json_array_elements(cast(p.bahanbaku as json)) as b " + w
                    + " order by p.nama asc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Menu","text")); kolom.add(new Kolom("Bahan Baku","text")); kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Harga","num")); kolom.add(new Kolom("Subtotal (HPP)","num"));

            } else if ("resep_hpp_menu".equals(r)) {
                judul = "HPP & Margin per Menu (dari Resep)";
                catatan = "Modal (HPP) tiap menu dihitung dari komposisi bahan baku, dibandingkan harga jual.";
                StringBuilder w = new StringBuilder(" where p.bahanbaku is not null and trim(p.bahanbaku) not in ('','[]') and trim(p.bahanbaku) like '[%' ");
                w.append(kondToko("p.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(coalesce(p.kode,'')) like :qp or lower(coalesce(p.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select x.menu, x.tenant, x.hpp, x.jual, (x.jual - x.hpp) from ( "
                    + "  select p.nama as menu, coalesce(t.nama,'-') as tenant, "
                    + "   coalesce((select sum(cast(coalesce(nullif(b->>'qty',''),'0') as numeric)*cast(coalesce(nullif(b->>'harga',''),'0') as numeric)) "
                    + "             from json_array_elements(cast(p.bahanbaku as json)) b),0) as hpp, "
                    + "   coalesce(p.hargajual,0) as jual "
                    + "  from koperasi.produk p left join koperasi.toko t on t.id=p.toko " + w
                    + " ) x order by x.menu asc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Menu","text")); kolom.add(new Kolom("Tenant","text")); kolom.add(new Kolom("HPP (Resep)","num")); kolom.add(new Kolom("Harga Jual","num")); kolom.add(new Kolom("Untung","num"));

            } else if ("opname_selisih_nilai".equals(r)) {
                judul = "Selisih Stok Opname (Nilai Terbesar)";
                catatan = "Barang dengan selisih hasil opname terbesar (qty & nilai) — prioritas investigasi.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(t.nama,'-'), sum(coalesce(o.selisih,0)), sum(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk left join koperasi.toko t on t.id=o.toko " + w
                    + " group by pr.kode, pr.nama, t.nama order by abs(sum(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0))) desc ";
                tipe = new String[]{"text","text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Selisih Qty","num")); kolom.add(new Kolom("Nilai Selisih","num"));

            } else if ("opname_per_toko".equals(r)) {
                judul = "Riwayat Stock Opname per Toko";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                sql = "select coalesce(t.nama,'-'), count(*), sum(case when coalesce(o.selisih,0) < 0 then 1 else 0 end), sum(coalesce(o.selisih,0)), sum(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk left join koperasi.toko t on t.id=o.toko " + w
                    + " group by t.nama order by 5 asc ";
                tipe = new String[]{"text","num","num","num","num"};
                kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Jml Opname","num")); kolom.add(new Kolom("Jml Minus","num")); kolom.add(new Kolom("Total Selisih Qty","num")); kolom.add(new Kolom("Nilai Selisih","num"));

            } else if ("mst_tenant".equals(r)) {
                judul = "Daftar Tenant / Stan (Toko)";
                catatan = "Daftar tenant/stan/outlet (Toko) beserta jumlah produknya.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (qc != null) { w.append(" and lower(coalesce(t.nama,'')) like :qc "); prm.put("qc", qc); }
                sql = "select t.nama, coalesce(t.keterangan,'-'), coalesce(cnt.jml,0) "
                    + " from koperasi.toko t "
                    + " left join (select toko, count(*) as jml from koperasi.produk group by toko) cnt on cnt.toko=t.id " + w
                    + " order by t.nama asc ";
                tipe = new String[]{"text","text","num"};
                kolom.add(new Kolom("Tenant / Stan","text")); kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Jml Produk","num"));

            } else if ("kas_buka_tutup".equals(r)) {
                judul = "Buka Tutup Kas Kasir";
                catatan = "Sesi kas tiap kasir: modal awal, penjualan tunai & non-tunai, uang fisik, dan selisih.";
                tipe = new String[]{"text","tgl","tgl","num","num","num","num","num","text"};
                kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Buka","tgl")); kolom.add(new Kolom("Tutup","tgl"));
                kolom.add(new Kolom("Modal Awal","num")); kolom.add(new Kolom("Tunai","num")); kolom.add(new Kolom("Non Tunai","num"));
                kolom.add(new Kolom("Uang Fisik","num")); kolom.add(new Kolom("Selisih","num")); kolom.add(new Kolom("Status","text"));
                if (!tabelAda(session, "koperasi.sesi_kas_kasir")) {
                    H.judul = judul; H.catatan = "Fitur Buka/Tutup Kas belum aktif (restart aplikasi setelah deploy, lalu buka kas di menu Kas Kasir)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("k.toko", tokoId, prm));
                w.append(klausaTanggal("k.waktubuka", tglMulai, tglSampai, prm));
                sql = "select coalesce(k.kasir_nama,'-'), k.waktubuka, k.waktututup, coalesce(k.modalawal,0), coalesce(k.totaltunai,0), coalesce(k.totalnontunai,0), coalesce(k.uangfisik,0), coalesce(k.selisih,0), coalesce(k.status,'BUKA') "
                    + " from koperasi.sesi_kas_kasir k " + w + " order by k.waktubuka desc ";

            } else if ("kas_selisih".equals(r)) {
                judul = "Selisih Kas Kasir";
                catatan = "Sesi kas yang selisih (uang fisik tidak sama dengan kas seharusnya) — untuk pengawasan.";
                tipe = new String[]{"text","tgl","text","num","num","num"};
                kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Kas Seharusnya","num")); kolom.add(new Kolom("Uang Fisik","num")); kolom.add(new Kolom("Selisih","num"));
                if (!tabelAda(session, "koperasi.sesi_kas_kasir")) {
                    H.judul = judul; H.catatan = "Fitur Buka/Tutup Kas belum aktif (restart aplikasi setelah deploy)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where coalesce(k.status,'BUKA') = 'TUTUP' and coalesce(k.selisih,0) <> 0 ");
                w.append(kondToko("k.toko", tokoId, prm));
                w.append(klausaTanggal("k.waktubuka", tglMulai, tglSampai, prm));
                sql = "select coalesce(k.kasir_nama,'-'), k.waktubuka, coalesce(t.nama,'-'), (coalesce(k.modalawal,0)+coalesce(k.totaltunai,0)), coalesce(k.uangfisik,0), coalesce(k.selisih,0) "
                    + " from koperasi.sesi_kas_kasir k left join koperasi.toko t on t.id=k.toko " + w + " order by abs(coalesce(k.selisih,0)) desc ";

            } else if ("tenant_setoran".equals(r)) {
                judul = "Setoran & Bagi Hasil Tenant";
                catatan = "Kewajiban (bagi hasil + sewa + biaya) dan setoran tiap tenant/stan per periode.";
                tipe = new String[]{"text","text","tgl","num","num","num","num","num","num","text"};
                kolom.add(new Kolom("Tenant","text")); kolom.add(new Kolom("Periode","text")); kolom.add(new Kolom("Tanggal","tgl"));
                kolom.add(new Kolom("Omzet","num")); kolom.add(new Kolom("Bagi Hasil","num")); kolom.add(new Kolom("Sewa","num"));
                kolom.add(new Kolom("Biaya","num")); kolom.add(new Kolom("Kewajiban","num")); kolom.add(new Kolom("Setoran","num")); kolom.add(new Kolom("Status","text"));
                if (!tabelAda(session, "koperasi.setoran_tenant")) {
                    H.judul = judul; H.catatan = "Fitur Setoran Tenant belum aktif (restart aplikasi setelah deploy, lalu input di menu Setoran Tenant)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("k.toko", tokoId, prm));
                w.append(klausaTanggal("k.tanggal", tglMulai, tglSampai, prm));
                sql = "select coalesce(t.nama,'-'), coalesce(k.periode,'-'), k.tanggal, coalesce(k.omzet,0), coalesce(k.nilaibagihasil,0), coalesce(k.sewa,0), coalesce(k.biayalayanan,0), "
                    + " (coalesce(k.nilaibagihasil,0)+coalesce(k.sewa,0)+coalesce(k.biayalayanan,0)), coalesce(k.setoran,0), coalesce(k.status,'KURANG') "
                    + " from koperasi.setoran_tenant k left join koperasi.toko t on t.id=k.toko " + w + " order by k.tanggal desc ";

            } else if ("tenant_bagi_hasil".equals(r)) {
                judul = "Rekap Bagi Hasil per Tenant";
                tipe = new String[]{"text","num","num","num","num","num"};
                kolom.add(new Kolom("Tenant","text")); kolom.add(new Kolom("Jml Periode","num")); kolom.add(new Kolom("Total Omzet","num"));
                kolom.add(new Kolom("Total Kewajiban","num")); kolom.add(new Kolom("Total Setoran","num")); kolom.add(new Kolom("Sisa","num"));
                if (!tabelAda(session, "koperasi.setoran_tenant")) {
                    H.judul = judul; H.catatan = "Fitur Setoran Tenant belum aktif (restart aplikasi setelah deploy)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("k.toko", tokoId, prm));
                w.append(klausaTanggal("k.tanggal", tglMulai, tglSampai, prm));
                sql = "select coalesce(t.nama,'-'), count(*), sum(coalesce(k.omzet,0)), "
                    + " sum(coalesce(k.nilaibagihasil,0)+coalesce(k.sewa,0)+coalesce(k.biayalayanan,0)), sum(coalesce(k.setoran,0)), "
                    + " sum((coalesce(k.nilaibagihasil,0)+coalesce(k.sewa,0)+coalesce(k.biayalayanan,0))-coalesce(k.setoran,0)) "
                    + " from koperasi.setoran_tenant k left join koperasi.toko t on t.id=k.toko " + w + " group by t.nama order by 4 desc ";

            } else if ("tenant_tunggakan".equals(r)) {
                judul = "Tunggakan Tenant";
                catatan = "Tenant dengan setoran kurang dari kewajiban (bagi hasil + sewa + biaya).";
                tipe = new String[]{"text","text","tgl","num","num","num"};
                kolom.add(new Kolom("Tenant","text")); kolom.add(new Kolom("Periode","text")); kolom.add(new Kolom("Tanggal","tgl"));
                kolom.add(new Kolom("Kewajiban","num")); kolom.add(new Kolom("Setoran","num")); kolom.add(new Kolom("Tunggakan","num"));
                if (!tabelAda(session, "koperasi.setoran_tenant")) {
                    H.judul = judul; H.catatan = "Fitur Setoran Tenant belum aktif (restart aplikasi setelah deploy)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where (coalesce(k.nilaibagihasil,0)+coalesce(k.sewa,0)+coalesce(k.biayalayanan,0)) > coalesce(k.setoran,0) ");
                w.append(kondToko("k.toko", tokoId, prm));
                w.append(klausaTanggal("k.tanggal", tglMulai, tglSampai, prm));
                sql = "select coalesce(t.nama,'-'), coalesce(k.periode,'-'), k.tanggal, "
                    + " (coalesce(k.nilaibagihasil,0)+coalesce(k.sewa,0)+coalesce(k.biayalayanan,0)), coalesce(k.setoran,0), "
                    + " ((coalesce(k.nilaibagihasil,0)+coalesce(k.sewa,0)+coalesce(k.biayalayanan,0))-coalesce(k.setoran,0)) "
                    + " from koperasi.setoran_tenant k left join koperasi.toko t on t.id=k.toko " + w + " order by 6 desc ";

            } else if ("dead_stock".equals(r)) { tokoIdCol = "pr.toko";
                judul = "Barang Dead Stock / Tidak Laku";
                catatan = "Produk berstok namun TIDAK ada penjualan pada periode — kandidat promo/penghapusan.";
                StringBuilder w = new StringBuilder(" where pr.aktif = true and coalesce(pr.stok,0) > 0 ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                String sub = " and not exists (select 1 from koperasi.pembelian p where p.produk=pr.id "
                    + kondToko("p.toko", tokoId, prm) + klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm) + ") ";
                w.append(sub);
                sql = "select pr.kode, pr.nama, coalesce(k.nama,'Umum'), coalesce(pr.stok,0), coalesce(pr.hargabeli,0), (coalesce(pr.stok,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.produk pr left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w + " order by 6 desc ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Harga Modal","num")); kolom.add(new Kolom("Nilai Stok Diam","num"));

            } else if ("analisa_diskon".equals(r)) { tokoIdCol = "p.toko";
                judul = "Analisa Diskon per Produk";
                catatan = "Total diskon yang diberikan tiap produk (dari kolom diskon transaksi POS).";
                StringBuilder w = new StringBuilder(" where coalesce(p.diskon,0) > 0 ");
                w.append(kondToko("p.toko", tokoId, prm));
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", sum(coalesce(p.qty,0)), sum(coalesce(p.diskon,0)), sum(coalesce(p.hargasatuan, pr.hargajual, 0)*coalesce(p.qty,0)) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w + " group by 1, 2 order by 4 desc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Qty","num")); kolom.add(new Kolom("Total Diskon","num")); kolom.add(new Kolom("Nilai Kotor","num"));

            } else if ("ar_umur_piutang".equals(r)) { tokoIdCol = "u.toko";
                judul = "Umur Piutang (Aging)";
                catatan = "Nilai Kasbon dikelompokkan berdasarkan umur transaksi. Voucher, QRIS, Tunai, dan Transfer tidak termasuk.";
                String umur = "(current_date - cast(u.tanggal as date))";
                StringBuilder w = new StringBuilder(" where u.nilai_piutang > 0 ");
                w.append(kondToko("u.toko", tokoId, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(a.nama,'Umum / Non-Anggota'), "
                    + " sum(case when " + umur + " <= 30 then u.nilai_piutang else 0 end), "
                    + " sum(case when " + umur + " between 31 and 60 then u.nilai_piutang else 0 end), "
                    + " sum(case when " + umur + " between 61 and 90 then u.nilai_piutang else 0 end), "
                    + " sum(case when " + umur + " > 90 then u.nilai_piutang else 0 end), sum(u.nilai_piutang) "
                    + " from (" + sqlSumberKasbon() + ") u join koperasi.anggota_koperasi a on a.id=u.anggota_koperasi "
                    + w + " group by a.nama order by 6 desc ";
                tipe = new String[]{"text","num","num","num","num","num"};
                kolom.add(new Kolom("Pelanggan","text")); kolom.add(new Kolom("0-30 hr","num")); kolom.add(new Kolom("31-60 hr","num")); kolom.add(new Kolom("61-90 hr","num")); kolom.add(new Kolom(">90 hr","num")); kolom.add(new Kolom("Total Piutang","num"));

            } else if ("perputaran_stok".equals(r)) { tokoIdCol = "p.toko";
                judul = "Perputaran Stok (Turnover)";
                catatan = "Perputaran = qty terjual dibagi stok saat ini (semakin besar = semakin cepat berputar).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("p.toko", tokoId, prm));
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, sum(coalesce(p.qty,0)), coalesce(pr.stok,0), "
                    + " (case when coalesce(pr.stok,0) > 0 then sum(coalesce(p.qty,0))/coalesce(pr.stok,0) else 0 end) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w + " group by pr.kode, pr.nama, pr.stok order by 5 desc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Qty Terjual","num")); kolom.add(new Kolom("Stok Kini","num")); kolom.add(new Kolom("Perputaran (x)","num"));

            } else if ("kontribusi_produk".equals(r)) { tokoIdCol = "p.toko";
                judul = "Kontribusi Produk terhadap Omzet";
                catatan = "Persentase sumbangan tiap produk terhadap total omzet pada periode.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("p.toko", tokoId, prm));
                w.append(klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select " + KODE_PRODUK_ITEM + ", " + NAMA_PRODUK_ITEM + ", sum(" + OMZET + "), "
                    + " cast(sum(" + OMZET + ") / nullif(sum(sum(" + OMZET + ")) over (), 0) * 100 as numeric(10,2)) "
                    + " from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w + " group by 1, 2 order by 3 desc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Omzet","num")); kolom.add(new Kolom("Kontribusi (%)","num"));

            } else if ("kasir_sering_selisih".equals(r)) {
                judul = "Kasir Sering Selisih";
                catatan = "Peringkat kasir berdasarkan frekuensi & besar selisih kas (dari sesi Buka/Tutup Kas).";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Kasir","text")); kolom.add(new Kolom("Jml Sesi","num")); kolom.add(new Kolom("Jml Selisih","num")); kolom.add(new Kolom("Total Selisih (abs)","num"));
                if (!tabelAda(session, "koperasi.sesi_kas_kasir")) {
                    H.judul = judul; H.catatan = "Fitur Buka/Tutup Kas belum aktif (restart aplikasi setelah deploy)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where coalesce(k.status,'BUKA') = 'TUTUP' ");
                w.append(kondToko("k.toko", tokoId, prm));
                w.append(klausaTanggal("k.waktubuka", tglMulai, tglSampai, prm));
                sql = "select coalesce(k.kasir_nama,'-'), count(*), sum(case when coalesce(k.selisih,0) <> 0 then 1 else 0 end), sum(abs(coalesce(k.selisih,0))) "
                    + " from koperasi.sesi_kas_kasir k " + w + " group by k.kasir_nama order by 4 desc ";

            } else if ("produk_sering_dikoreksi".equals(r)) {
                judul = "Produk Sering Dikoreksi (Opname)";
                catatan = "Produk yang paling sering mengalami koreksi stok (opname) — potensi masalah data/proses.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, count(*), sum(coalesce(o.selisih,0)), sum(abs(coalesce(o.selisih,0))) "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk " + w + " group by pr.kode, pr.nama order by 3 desc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Jml Koreksi","num")); kolom.add(new Kolom("Total Selisih","num")); kolom.add(new Kolom("Selisih (abs)","num"));

            } else if ("beli_belum_datang".equals(r)) {
                judul = "Barang Belum Diterima (PR)";
                catatan = "Permintaan pengadaan yang jumlah barangnya belum seluruhnya datang.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("p.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select p.kode, p.tanggal_pembuatan, sum(coalesce(d.jumlah,0)), sum(coalesce(d.jumlahdatang,0)), (sum(coalesce(d.jumlah,0))-sum(coalesce(d.jumlahdatang,0))) "
                    + " from asset.permintaan_pengadaan_master_asset_detail d "
                    + " join asset.permintaan_pengadaan_master_asset p on p.id=d.permintaan_pengadaan_master_asset " + w
                    + " group by p.kode, p.tanggal_pembuatan having sum(coalesce(d.jumlah,0)) > sum(coalesce(d.jumlahdatang,0)) order by p.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","num","num","num"};
                kolom.add(new Kolom("No. PR","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Diminta","num")); kolom.add(new Kolom("Sudah Datang","num")); kolom.add(new Kolom("Belum Datang","num"));

            } else if ("potong_gaji".equals(r)) {
                judul = "Potong Gaji Pegawai (Transaksi Pegawai)";
                catatan = "Transaksi/potongan gaji pegawai (mis. belanja kantin yang dipotong dari gaji). Gunakan kotak pencarian untuk menyaring, mis. ketik \"kantin\".";
                StringBuilder w = new StringBuilder(" where t.thn >= (cast(extract(year from current_date) as integer) - 1) ");
                if (qc != null) { w.append(" and (lower(coalesce(t.keterangan,'')) like :qc or lower(coalesce(peg.nama,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(peg.nama,'-'), coalesce(jt.nama,'-'), (cast(t.thn as text)||'-'||cast(t.bln as text)), coalesce(t.tgl,0), coalesce(t.nilai,0), coalesce(t.keterangan,'') "
                    + " from payroll.transaksi_pegawai t left join public.pegawai peg on peg.id=t.pegawai "
                    + " left join payroll.jenis_transaksi_pegawai jt on jt.id=t.jenis_transaksi_pegawai " + w
                    + " order by t.thn desc, t.bln desc, t.tgl desc ";
                tipe = new String[]{"text","text","text","num","num","text"};
                kolom.add(new Kolom("Pegawai","text")); kolom.add(new Kolom("Jenis","text")); kolom.add(new Kolom("Periode","text")); kolom.add(new Kolom("Tgl","num")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Keterangan","text"));

            } else if ("prediksi_habis".equals(r)) {
                judul = "Prediksi Kehabisan Stok";
                catatan = "Perkiraan berapa hari lagi stok habis berdasarkan rata-rata penjualan 30 hari terakhir. Urut paling mendesak dahulu.";
                String tk = kondToko("p.toko", tokoId, prm);
                StringBuilder w = new StringBuilder(" where pr.aktif = true and coalesce(pr.stok,0) > 0 ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(pr.stok,0), coalesce(s.terjual,0), "
                    + " cast(coalesce(s.terjual,0)/30.0 as numeric(10,2)), "
                    + " (case when coalesce(s.terjual,0) > 0 then cast(coalesce(pr.stok,0)/(coalesce(s.terjual,0)/30.0) as numeric(10,1)) else null end) "
                    + " from koperasi.produk pr "
                    + " left join (select p.produk, sum(coalesce(p.qty,0)) as terjual from koperasi.pembelian p where p.waktu >= current_date - interval '30 days' " + tk + " group by p.produk) s on s.produk=pr.id "
                    + w + " order by 6 asc nulls last ";
                tipe = new String[]{"text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Terjual 30 Hari","num")); kolom.add(new Kolom("Rata/Hari","num")); kolom.add(new Kolom("Perkiraan Habis (hari)","num"));

            } else if ("rekomendasi_beli".equals(r)) {
                judul = "Rekomendasi Pembelian Ulang";
                catatan = "Produk yang diperkirakan habis dalam 14 hari, beserta saran jumlah beli untuk cukup 30 hari.";
                String tk = kondToko("p.toko", tokoId, prm);
                StringBuilder w = new StringBuilder(" where pr.aktif = true and coalesce(pr.stok,0) >= 0 ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select x.kode, x.nama, x.stok, x.rata, x.hari, cast(ceil(x.rata*30 - x.stok) as numeric(12,0)) from ( "
                    + "  select pr.kode, pr.nama, coalesce(pr.stok,0) as stok, cast(coalesce(s.terjual,0)/30.0 as numeric(10,2)) as rata, "
                    + "   (case when coalesce(s.terjual,0) > 0 then cast(coalesce(pr.stok,0)/(coalesce(s.terjual,0)/30.0) as numeric(10,1)) else null end) as hari "
                    + "  from koperasi.produk pr "
                    + "  left join (select p.produk, sum(coalesce(p.qty,0)) as terjual from koperasi.pembelian p where p.waktu >= current_date - interval '30 days' " + tk + " group by p.produk) s on s.produk=pr.id "
                    + w + " ) x where x.hari is not null and x.hari <= 14 order by x.hari asc ";
                tipe = new String[]{"text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Rata/Hari","num")); kolom.add(new Kolom("Perkiraan Habis (hari)","num")); kolom.add(new Kolom("Saran Beli (30 hr)","num"));

            } else if ("wh_umur_stok".equals(r)) {
                judul = "Analisa Umur Stok";
                catatan = "Lama sejak barang terakhir masuk (indikasi barang lama menumpuk). Diurut dari yang paling lama.";
                StringBuilder w = new StringBuilder(" where m.qty > 0 ");
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, max(m.tanggal), (current_date - cast(max(m.tanggal) as date)) "
                    + " from asset.mutasi_lokasi m join koperasi.produk pr on pr.id=m.produk " + w
                    + " group by pr.kode, pr.nama order by 4 desc ";
                tipe = new String[]{"text","text","tgl","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Masuk Terakhir","tgl")); kolom.add(new Kolom("Umur (hari)","num"));

            } else if ("dompet_saldo".equals(r)) {
                judul = "Saldo Tabungan / Dompet per Anggota";
                catatan = "Saldo tabungan tiap anggota/siswa/mahasiswa (dari modul Deposit) — jumlah seluruh setoran.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("d.waktu", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and lower(coalesce(d.nama,'')) like :qc "); prm.put("qc", qc); }
                sql = "select coalesce(d.nama,'-'), coalesce(jt.nama,'Umum'), count(*), sum(coalesce(d.nominal,0)) "
                    + " from public.deposit d left join public.jenis_tabungan jt on jt.id=d.jenis_tabungan " + w
                    + " group by d.nama, jt.nama order by 4 desc ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Nama","text")); kolom.add(new Kolom("Jenis Tabungan","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Saldo","num"));

            } else if ("dompet_per_jenis".equals(r)) {
                judul = "Rekap Tabungan per Jenis";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("d.waktu", tglMulai, tglSampai, prm));
                sql = "select coalesce(jt.nama,'Umum'), count(*), sum(coalesce(d.nominal,0)) "
                    + " from public.deposit d left join public.jenis_tabungan jt on jt.id=d.jenis_tabungan " + w
                    + " group by jt.nama order by 3 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Jenis Tabungan","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Total Saldo","num"));

            } else if ("pencairan_saldo".equals(r)) {
                judul = "Pencairan / Penarikan Saldo";
                catatan = "Pencairan saldo anggota (tunai/transfer/potong belanja) beserta status.";
                tipe = new String[]{"text","tgl","text","text","text","num","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Waktu","tgl")); kolom.add(new Kolom("Member","text")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Cara","text")); kolom.add(new Kolom("Nominal","num")); kolom.add(new Kolom("Status","text"));
                if (!tabelAda(session, "koperasi.pencairan_diskon")) {
                    H.judul = judul; H.catatan = "Fitur Pencairan Saldo belum aktif pada database ini."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("pc.toko", tokoId, prm));
                w.append(klausaTanggal("pc.waktu_pencairan", tglMulai, tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(pc.kode_pencairan,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(pc.kode_pencairan,'-'), pc.waktu_pencairan, coalesce(a.nama,'-'), coalesce(t.nama,'-'), coalesce(cp.nama,'-'), coalesce(pc.nominal_cair,0), coalesce(pc.status,'-') "
                    + " from koperasi.pencairan_diskon pc "
                    + " left join koperasi.anggota_koperasi a on a.id=pc.anggota_koperasi "
                    + " left join koperasi.toko t on t.id=pc.toko "
                    + " left join koperasi.cara_pembayaran_koperasi cp on cp.id=pc.cara_pembayaran "
                    + w + " order by pc.waktu_pencairan desc ";

            } else if ("rekap_pencairan".equals(r)) {
                judul = "Rekap Pencairan/Refund per Cara Bayar";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Cara Pencairan","text")); kolom.add(new Kolom("Jml Pencairan","num")); kolom.add(new Kolom("Total Nominal","num"));
                if (!tabelAda(session, "koperasi.pencairan_diskon")) {
                    H.judul = judul; H.catatan = "Fitur Pencairan Saldo belum aktif pada database ini."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where coalesce(pc.status,'') = 'BERHASIL' ");
                w.append(kondToko("pc.toko", tokoId, prm));
                w.append(klausaTanggal("pc.waktu_pencairan", tglMulai, tglSampai, prm));
                sql = "select coalesce(cp.nama,'-'), count(*), sum(coalesce(pc.nominal_cair,0)) "
                    + " from koperasi.pencairan_diskon pc left join koperasi.cara_pembayaran_koperasi cp on cp.id=pc.cara_pembayaran "
                    + w + " group by cp.nama order by 3 desc ";

            } else if ("voucher_kantin".equals(r)) {
                judul = "Voucher Kantin (Topup & Kadaluarsa)";
                catatan = "Pencairan berupa topup/voucher beserta tanggal kadaluarsa dan status aktif/kadaluarsa.";
                tipe = new String[]{"text","text","num","tgl","text","text"};
                kolom.add(new Kolom("Kode Voucher","text")); kolom.add(new Kolom("Member","text")); kolom.add(new Kolom("Nominal","num"));
                kolom.add(new Kolom("Kadaluarsa","tgl")); kolom.add(new Kolom("Status","text")); kolom.add(new Kolom("Keadaan","text"));
                if (!tabelAda(session, "koperasi.pencairan_diskon")) {
                    H.judul = judul; H.catatan = "Fitur Pencairan/Voucher belum aktif pada database ini."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where pc.tanggal_expired_jika_berupa_topup is not null ");
                w.append(kondToko("pc.toko", tokoId, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(pc.kode_pencairan,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(pc.kode_pencairan,'-'), coalesce(a.nama,'-'), coalesce(pc.nominal_cair,0), pc.tanggal_expired_jika_berupa_topup, coalesce(pc.status,'-'), "
                    + " (case when pc.tanggal_expired_jika_berupa_topup < current_date then 'Kadaluarsa' else 'Aktif' end) "
                    + " from koperasi.pencairan_diskon pc left join koperasi.anggota_koperasi a on a.id=pc.anggota_koperasi "
                    + w + " order by pc.tanggal_expired_jika_berupa_topup asc ";

            } else if ("pemusnahan".equals(r)) {
                judul = "Pemusnahan / Penghapusan Barang";
                catatan = "Daftar penghapusan/pemusnahan barang (dari modul Penghapusan Aset) beserta status persetujuan.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("ph.tanggal_pembuatan", tglMulai, tglSampai, prm));
                sql = "select ph.kode, ph.tanggal_pembuatan, coalesce(jp.nama,'-'), coalesce(ph.keterangan,''), coalesce(ph.nilai,0), "
                    + " (case when ph.tanggal_persetujuan is not null then 'Disetujui' else 'Draft' end) "
                    + " from asset.penghapusan_master_asset ph left join asset.jenis_penghapusan_barang jp on jp.id=ph.jenis_pengapusan_barang " + w
                    + " order by ph.tanggal_pembuatan desc ";
                tipe = new String[]{"text","tgl","text","text","num","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Jenis Penghapusan","text")); kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Nilai","num")); kolom.add(new Kolom("Status","text"));

            } else if ("pemusnahan_rinci".equals(r)) {
                judul = "Rincian Pemusnahan per Item"; grupIdx = 0;
                catatan = "Barang yang dimusnahkan/dihapus per dokumen (dikelompokkan per nomor penghapusan).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(klausaTanggal("ph.tanggal_pembuatan", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(ma.kode,'')) like :qp or lower(coalesce(ma.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(ph.kode,'-'), coalesce(ma.kode,'-'), coalesce(ma.nama,'-'), coalesce(d.hargabeli,0), coalesce(d.keterangan,'') "
                    + " from asset.penghapusan_master_asset_detail d "
                    + " join asset.penghapusan_master_asset ph on ph.id=d.penghapusan_master_asset "
                    + " left join asset.master_asset ma on ma.id=d.masterasset " + w
                    + " order by ph.tanggal_pembuatan desc, ph.kode asc ";
                tipe = new String[]{"text","text","text","num","text"};
                kolom.add(new Kolom("No. Penghapusan","text")); kolom.add(new Kolom("Kode Item","text")); kolom.add(new Kolom("Nama Item","text")); kolom.add(new Kolom("Harga Beli","num")); kolom.add(new Kolom("Keterangan","text"));

            } else if ("jadwal_opname".equals(r)) {
                judul = "Jadwal Stock Opname";
                catatan = "Daftar rencana/kegiatan stock opname beserta status pelaksanaan.";
                tipe = new String[]{"text","text","tgl","text","text","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Tgl Rencana","tgl")); kolom.add(new Kolom("Kategori","text"));
                kolom.add(new Kolom("Petugas","text")); kolom.add(new Kolom("Status","text")); kolom.add(new Kolom("Keterangan","text"));
                if (!tabelAda(session, "koperasi.sesi_stok_opname")) {
                    H.judul = judul; H.catatan = "Fitur Jadwal Opname belum aktif (restart aplikasi setelah deploy, lalu buat jadwal di menu Jadwal Opname)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("s.toko", tokoId, prm));
                w.append(klausaTanggal("s.tanggalrencana", tglMulai, tglSampai, prm));
                sql = "select coalesce(s.kode,'-'), coalesce(t.nama,'-'), s.tanggalrencana, coalesce(s.kategori,'-'), coalesce(s.petugas,'-'), coalesce(s.status,'RENCANA'), coalesce(s.keterangan,'') "
                    + " from koperasi.sesi_stok_opname s left join koperasi.toko t on t.id=s.toko " + w + " order by s.tanggalrencana desc ";

            } else if ("berita_acara_opname".equals(r)) {
                judul = "Berita Acara Stock Opname (Ringkasan)";
                catatan = "Ringkasan hasil opname per toko pada periode terpilih: jumlah item, item lebih/kurang, dan nilai selisih.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                sql = "select coalesce(t.nama,'-'), count(*), sum(case when coalesce(o.selisih,0) > 0 then 1 else 0 end), "
                    + " sum(case when coalesce(o.selisih,0) < 0 then 1 else 0 end), sum(coalesce(o.selisih,0)), sum(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk left join koperasi.toko t on t.id=o.toko " + w
                    + " group by t.nama order by t.nama asc ";
                tipe = new String[]{"text","num","num","num","num","num"};
                kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Jml Item Dihitung","num")); kolom.add(new Kolom("Item Lebih","num")); kolom.add(new Kolom("Item Kurang","num")); kolom.add(new Kolom("Total Selisih Qty","num")); kolom.add(new Kolom("Nilai Selisih","num"));

            } else if ("stok_minimum".equals(r)) { tokoIdCol = "pr.toko";
                judul = "Stok Minimum / Reorder";
                catatan = "Produk yang stoknya sudah mencapai/di bawah batas minimum — perlu dibeli ulang. Isi batas minimum di menu Stok Min & Expired.";
                StringBuilder w = new StringBuilder(" where pr.aktif = true and coalesce(pr.stok_minimum,0) > 0 and coalesce(pr.stok,0) <= coalesce(pr.stok_minimum,0) ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(k.nama,'Umum'), coalesce(pr.stok,0), coalesce(pr.stok_minimum,0), (coalesce(pr.stok_minimum,0)-coalesce(pr.stok,0)) "
                    + " from koperasi.produk pr left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w
                    + " order by (coalesce(pr.stok_minimum,0)-coalesce(pr.stok,0)) desc ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Stok Minimum","num")); kolom.add(new Kolom("Kurang","num"));

            } else if ("barang_mendekati_expired".equals(r)) { tokoIdCol = "pr.toko";
                judul = "Barang Mendekati Kadaluarsa";
                catatan = "Produk yang akan kedaluwarsa dalam 60 hari ke depan. Isi tanggal expired di menu Stok Min & Expired.";
                StringBuilder w = new StringBuilder(" where pr.aktif = true and pr.tanggal_expired is not null and pr.tanggal_expired >= current_date and pr.tanggal_expired <= current_date + 60 ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(pr.batch,'-'), pr.tanggal_expired, (pr.tanggal_expired - current_date), coalesce(pr.stok,0) "
                    + " from koperasi.produk pr " + w + " order by pr.tanggal_expired asc ";
                tipe = new String[]{"text","text","text","tgl","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Batch","text")); kolom.add(new Kolom("Kadaluarsa","tgl")); kolom.add(new Kolom("Sisa (hari)","num")); kolom.add(new Kolom("Stok","num"));

            } else if ("barang_expired".equals(r)) { tokoIdCol = "pr.toko";
                judul = "Barang Kadaluarsa (Expired)";
                catatan = "Produk yang sudah melewati tanggal kedaluwarsa — sebaiknya ditarik dari penjualan.";
                StringBuilder w = new StringBuilder(" where pr.aktif = true and pr.tanggal_expired is not null and pr.tanggal_expired < current_date ");
                w.append(kondToko("pr.toko", tokoId, prm));
                if (qp != null) { w.append(" and (lower(pr.kode) like :qp or lower(pr.nama) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(pr.batch,'-'), pr.tanggal_expired, (current_date - pr.tanggal_expired), coalesce(pr.stok,0), (coalesce(pr.stok,0)*coalesce(pr.hargabeli,0)) "
                    + " from koperasi.produk pr " + w + " order by pr.tanggal_expired asc ";
                tipe = new String[]{"text","text","text","tgl","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Batch","text")); kolom.add(new Kolom("Kadaluarsa","tgl")); kolom.add(new Kolom("Lewat (hari)","num")); kolom.add(new Kolom("Stok","num")); kolom.add(new Kolom("Nilai Kerugian","num"));

            } else if ("barang_rusak".equals(r)) {
                judul = "Barang Rusak";
                catatan = "Barang berkurang saat opname dengan alasan rusak/basi/pecah/cacat (dari catatan opname).";
                StringBuilder w = new StringBuilder(" where coalesce(o.selisih,0) < 0 and ("
                    + " lower(coalesce(o.keterangan,'')) like '%rusak%' or lower(coalesce(o.keterangan,'')) like '%basi%' "
                    + " or lower(coalesce(o.keterangan,'')) like '%pecah%' or lower(coalesce(o.keterangan,'')) like '%cacat%' "
                    + " or lower(coalesce(o.keterangan,'')) like '%kadaluarsa%' or lower(coalesce(o.keterangan,'')) like '%expired%' or lower(coalesce(o.keterangan,'')) like '%busuk%') ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select o.waktuopname, pr.kode, pr.nama, coalesce(t.nama,'-'), coalesce(o.selisih,0), abs(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0)), coalesce(o.keterangan,'') "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk left join koperasi.toko t on t.id=o.toko " + w
                    + " order by o.waktuopname desc ";
                tipe = new String[]{"tgl","text","text","text","num","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Selisih Qty","num")); kolom.add(new Kolom("Nilai Kerugian","num")); kolom.add(new Kolom("Alasan","text"));

            } else if ("barang_hilang".equals(r)) {
                judul = "Barang Hilang";
                catatan = "Barang berkurang saat opname dengan alasan hilang/dicuri (dari catatan opname).";
                StringBuilder w = new StringBuilder(" where coalesce(o.selisih,0) < 0 and ("
                    + " lower(coalesce(o.keterangan,'')) like '%hilang%' or lower(coalesce(o.keterangan,'')) like '%curi%' or lower(coalesce(o.keterangan,'')) like '%raib%') ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select o.waktuopname, pr.kode, pr.nama, coalesce(t.nama,'-'), coalesce(o.selisih,0), abs(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0)), coalesce(o.keterangan,'') "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk left join koperasi.toko t on t.id=o.toko " + w
                    + " order by o.waktuopname desc ";
                tipe = new String[]{"tgl","text","text","text","num","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Toko","text"));
                kolom.add(new Kolom("Selisih Qty","num")); kolom.add(new Kolom("Nilai Kerugian","num")); kolom.add(new Kolom("Alasan","text"));

            } else if ("nilai_kerugian".equals(r)) {
                judul = "Nilai Kerugian Barang (Rekap)";
                catatan = "Akumulasi kerugian akibat kekurangan stok saat opname (rusak/hilang/basi/koreksi negatif) per produk.";
                StringBuilder w = new StringBuilder(" where coalesce(o.selisih,0) < 0 ");
                w.append(kondToko("o.toko", tokoId, prm));
                w.append(klausaTanggal("o.waktuopname", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select pr.kode, pr.nama, coalesce(k.nama,'Umum'), count(*), sum(coalesce(o.selisih,0)), abs(sum(coalesce(o.selisih,0)*coalesce(pr.hargabeli,0))) "
                    + " from koperasi.stok_opname o join koperasi.produk pr on pr.id=o.produk left join koperasi.jenis_produk k on k.id=pr.jenis_produk " + w
                    + " group by pr.kode, pr.nama, k.nama order by 6 desc ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Kategori","text")); kolom.add(new Kolom("Jml Kejadian","num")); kolom.add(new Kolom("Total Kurang","num")); kolom.add(new Kolom("Nilai Kerugian","num"));

            } else if ("kebutuhan_bahan".equals(r)) {
                judul = "Kebutuhan Bahan Baku (Resep x Penjualan)";
                catatan = "Perkiraan pemakaian bahan baku yang SEHARUSNYA: komposisi resep dikalikan jumlah menu terjual pada periode.";
                String tk = kondToko("p.toko", tokoId, prm) + klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm);
                sql = "select coalesce(b->>'nama','-'), "
                    + " sum(sold.qty * cast(coalesce(nullif(b->>'qty',''),'0') as numeric)), "
                    + " sum(sold.qty * cast(coalesce(nullif(b->>'qty',''),'0') as numeric) * cast(coalesce(nullif(b->>'harga',''),'0') as numeric)) "
                    + " from (select p.produk as menu_id, sum(coalesce(p.qty,0)) as qty from koperasi.pembelian p where 1=1 " + tk + " group by p.produk) sold "
                    + " join koperasi.produk pm on pm.id=sold.menu_id, json_array_elements(cast(pm.bahanbaku as json)) b "
                    + " where pm.bahanbaku is not null and trim(pm.bahanbaku) like '[%' group by 1 order by 2 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Bahan Baku","text")); kolom.add(new Kolom("Kebutuhan Qty","num")); kolom.add(new Kolom("Nilai Kebutuhan","num"));

            } else if ("bahan_selisih_resep".equals(r)) {
                judul = "Selisih Bahan Aktual vs Resep";
                catatan = "Perbandingan pemakaian bahan baku AKTUAL (Pemakaian Bahan Baku) dengan yang SEHARUSNYA (resep x menu terjual). Selisih positif = pemborosan.";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Bahan Baku","text")); kolom.add(new Kolom("Seharusnya","num")); kolom.add(new Kolom("Aktual","num")); kolom.add(new Kolom("Selisih","num"));
                if (!tabelAda(session, "koperasi.pemakaian_bahan_baku")) {
                    H.judul = judul; H.catatan = "Belum ada data Pemakaian Bahan Baku (input via Kulakan > Pemakaian Bahan Baku)."; H.grup = -1; H.tipe = tipe; return H;
                }
                String tkJual = kondToko("p.toko", tokoId, prm) + klausaPeriodeItemPenjualan(tglMulai, tglSampai, prm);
                String tkPakai = kondToko("x.toko", tokoId, prm) + klausaTanggal("x.waktu", tglMulai, tglSampai, prm);
				sql = "select coalesce(pr.kode,'-'), coalesce(pr.nama,'-'), coalesce(e.expected,0), coalesce(a.actual,0), (coalesce(a.actual,0)-coalesce(e.expected,0)) "
					+ " from ( select case when trim(coalesce(b->>'produk','')) ~ '^[0-9]+$' "
					+ " then cast(trim(b->>'produk') as bigint) else 0 end as bahan_id, "
                    + "          sum(sold.qty * cast(coalesce(nullif(b->>'qty',''),'0') as numeric)) as expected "
                    + "        from (select p.produk as menu_id, sum(coalesce(p.qty,0)) as qty from koperasi.pembelian p where 1=1 " + tkJual + " group by p.produk) sold "
                    + "        join koperasi.produk pm on pm.id=sold.menu_id, json_array_elements(cast(pm.bahanbaku as json)) b "
                    + "        where pm.bahanbaku is not null and trim(pm.bahanbaku) like '[%' group by 1 ) e "
                    + " full outer join ( select x.produk, sum(coalesce(x.qty,0)) as actual from koperasi.pemakaian_bahan_baku x where 1=1 " + tkPakai + " group by x.produk ) a on a.produk = e.bahan_id "
                    + " left join koperasi.produk pr on pr.id = coalesce(e.bahan_id, a.produk) "
                    + " order by abs(coalesce(a.actual,0)-coalesce(e.expected,0)) desc ";

            } else if ("realisasi_produksi".equals(r)) {
                judul = "Realisasi Produksi Harian";
                catatan = "Catatan produksi menu per hari: porsi direncanakan, dibuat, terjual, sisa, dan waste.";
                tipe = new String[]{"tgl","text","text","num","num","num","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Menu","text")); kolom.add(new Kolom("Toko","text")); kolom.add(new Kolom("Rencana","num"));
                kolom.add(new Kolom("Dibuat","num")); kolom.add(new Kolom("Terjual","num")); kolom.add(new Kolom("Sisa","num")); kolom.add(new Kolom("Waste","num"));
                if (!tabelAda(session, "koperasi.produksi_kantin")) {
                    H.judul = judul; H.catatan = "Fitur Produksi Kantin belum aktif (restart aplikasi setelah deploy, lalu input di menu Produksi Kantin)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("pk.toko", tokoId, prm));
                w.append(klausaTanggal("pk.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select pk.tanggal, coalesce(pr.nama,'-'), coalesce(t.nama,'-'), coalesce(pk.porsirencana,0), coalesce(pk.porsidibuat,0), coalesce(pk.porsiterjual,0), coalesce(pk.porsisisa,0), coalesce(pk.porsiwaste,0) "
                    + " from koperasi.produksi_kantin pk left join koperasi.produk pr on pr.id=pk.produk left join koperasi.toko t on t.id=pk.toko " + w
                    + " order by pk.tanggal desc ";

            } else if ("rekap_produksi".equals(r)) {
                judul = "Rekap Produksi per Menu";
                tipe = new String[]{"text","text","num","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Menu","text")); kolom.add(new Kolom("Rencana","num")); kolom.add(new Kolom("Dibuat","num"));
                kolom.add(new Kolom("Terjual","num")); kolom.add(new Kolom("Sisa","num")); kolom.add(new Kolom("Waste","num"));
                if (!tabelAda(session, "koperasi.produksi_kantin")) {
                    H.judul = judul; H.catatan = "Fitur Produksi Kantin belum aktif (restart aplikasi setelah deploy)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("pk.toko", tokoId, prm));
                w.append(klausaTanggal("pk.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select coalesce(pr.kode,'-'), coalesce(pr.nama,'-'), sum(coalesce(pk.porsirencana,0)), sum(coalesce(pk.porsidibuat,0)), sum(coalesce(pk.porsiterjual,0)), sum(coalesce(pk.porsisisa,0)), sum(coalesce(pk.porsiwaste,0)) "
                    + " from koperasi.produksi_kantin pk left join koperasi.produk pr on pr.id=pk.produk " + w
                    + " group by pr.kode, pr.nama order by 4 desc ";

            } else if ("waste_produksi".equals(r)) {
                judul = "Sisa Makanan / Waste Produksi";
                catatan = "Porsi terbuang/rusak/basi dari produksi beserta nilai kerugiannya (porsi waste x harga modal).";
                tipe = new String[]{"tgl","text","text","num","num","text"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Menu","text")); kolom.add(new Kolom("Waste (porsi)","num")); kolom.add(new Kolom("Nilai Kerugian","num")); kolom.add(new Kolom("Keterangan","text"));
                if (!tabelAda(session, "koperasi.produksi_kantin")) {
                    H.judul = judul; H.catatan = "Fitur Produksi Kantin belum aktif (restart aplikasi setelah deploy)."; H.grup = -1; H.tipe = tipe; return H;
                }
                StringBuilder w = new StringBuilder(" where coalesce(pk.porsiwaste,0) > 0 ");
                w.append(kondToko("pk.toko", tokoId, prm));
                w.append(klausaTanggal("pk.tanggal", tglMulai, tglSampai, prm));
                if (qp != null) { w.append(" and (lower(coalesce(pr.kode,'')) like :qp or lower(coalesce(pr.nama,'')) like :qp) "); prm.put("qp", qp); }
                sql = "select pk.tanggal, coalesce(pr.kode,'-'), coalesce(pr.nama,'-'), coalesce(pk.porsiwaste,0), (coalesce(pk.porsiwaste,0)*coalesce(pr.hargabeli,0)), coalesce(pk.keterangan,'') "
                    + " from koperasi.produksi_kantin pk left join koperasi.produk pr on pr.id=pk.produk " + w
                    + " order by pk.tanggal desc ";

            } else if ("konsumsi_kelas".equals(r)) {
                judul = "Rekap Belanja per Kelas";
                catatan = "Total belanja kantin dikelompokkan per kelas siswa (via keanggotaan koperasi).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("h.toko", tokoId, prm));
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select coalesce(kl.nama,'(Tanpa Kelas)'), count(distinct h.id), count(distinct a.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h "
                    + " join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + " join sekolah.siswa s on s.id=a.siswa "
                    + " left join public.kelas kl on kl.id=s.current_kelas_id " + w
                    + " group by kl.nama order by 4 desc ";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Kelas","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Jml Siswa","num")); kolom.add(new Kolom("Total Belanja","num"));

            } else if ("konsumsi_prodi".equals(r)) {
                judul = "Rekap Belanja per Prodi / Jurusan";
                catatan = "Total belanja kantin dikelompokkan per program studi/jurusan mahasiswa (via keanggotaan koperasi).";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                w.append(kondToko("h.toko", tokoId, prm));
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                sql = "select coalesce(j.nama,'(Tanpa Prodi)'), count(distinct h.id), count(distinct a.id), sum(coalesce(h.total_biaya,0)) "
                    + " from koperasi.pembelian_anggota_koperasi h "
                    + " join koperasi.anggota_koperasi a on a.id=h.anggota_koperasi "
                    + " join public.mahasiswa m on m.id=a.mahasiswa "
                    + " left join public.jurusan j on j.id=m.jurusan " + w
                    + " group by j.nama order by 4 desc ";
                tipe = new String[]{"text","num","num","num"};
                kolom.add(new Kolom("Prodi / Jurusan","text")); kolom.add(new Kolom("Jml Transaksi","num")); kolom.add(new Kolom("Jml Mahasiswa","num")); kolom.add(new Kolom("Total Belanja","num"));

            } else if ("audit_harga".equals(r)) {
                judul = "Audit Perubahan Harga Barang";
                catatan = "Riwayat perubahan harga modal produk (dari jejak audit Envers). Perlu audit Envers aktif.";
                tipe = new String[]{"text","text","text","tgl","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Produk","text")); kolom.add(new Kolom("Oleh","text")); kolom.add(new Kolom("Waktu","tgl")); kolom.add(new Kolom("Harga Lama","num")); kolom.add(new Kolom("Harga Baru","num"));
                if (!tabelAda(session, "new_audit.produk__audit")) {
                    H.judul = judul; H.catatan = "Jejak audit produk (Envers) tidak ditemukan pada database ini."; H.grup = -1; H.tipe = tipe; return H;
                }
                sql = "select x.kode, x.nama, x.oleh, x.waktu, x.hlama, x.hbaru from ( "
                    + "  select a.kode, a.nama, coalesce(a.oleh,'-') as oleh, "
                    + "    to_timestamp((select r.revtstmp from new_audit.revinfo r where r.rev=a.rev)/1000) as waktu, "
                    + "    lag(coalesce(a.hargabeli,0)) over (partition by a.id order by a.rev) as hlama, coalesce(a.hargabeli,0) as hbaru "
                    + "  from new_audit.produk__audit a "
                    + " ) x where coalesce(x.hlama,0) <> coalesce(x.hbaru,0) order by x.waktu desc ";

            } else if ("audit_master".equals(r)) {
                judul = "Audit Perubahan Master Barang";
                catatan = "Riwayat perubahan nama produk (dari jejak audit Envers). Perlu audit Envers aktif.";
                tipe = new String[]{"text","text","tgl","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Oleh","text")); kolom.add(new Kolom("Waktu","tgl")); kolom.add(new Kolom("Nama Lama","text")); kolom.add(new Kolom("Nama Baru","text"));
                if (!tabelAda(session, "new_audit.produk__audit")) {
                    H.judul = judul; H.catatan = "Jejak audit produk (Envers) tidak ditemukan pada database ini."; H.grup = -1; H.tipe = tipe; return H;
                }
                sql = "select x.kode, x.oleh, x.waktu, x.nlama, x.nbaru from ( "
                    + "  select a.kode, coalesce(a.oleh,'-') as oleh, "
                    + "    to_timestamp((select r.revtstmp from new_audit.revinfo r where r.rev=a.rev)/1000) as waktu, "
                    + "    lag(a.nama) over (partition by a.id order by a.rev) as nlama, a.nama as nbaru "
                    + "  from new_audit.produk__audit a "
                    + " ) x where coalesce(x.nlama,'') <> coalesce(x.nbaru,'') order by x.waktu desc ";

            } else if ("ar_sisa_kredit".equals(r)) {
                judul = "Limit & Sisa Kredit Pelanggan";
                catatan = "Plafon kredit anggota vs nominal transaksi Kasbon. Voucher, QRIS, Tunai, dan Transfer tidak memakai limit piutang. Sisa = Limit - Piutang; NEGATIF = melebihi limit.";
                StringBuilder w = new StringBuilder(" where a.aktif = true ");
                StringBuilder ws = new StringBuilder(" where u.nilai_piutang > 0 ");
                ws.append(kondToko("u.toko", tokoId, prm));
                ws.append(klausaSampai("u.tanggal", tglSampai, prm));
                if (qc != null) { w.append(" and (lower(coalesce(a.nama,'')) like :qc or lower(coalesce(a.kode,'')) like :qc or lower(coalesce(a.kode_identitas,'')) like :qc) "); prm.put("qc", qc); }
                sql = "select coalesce(a.kode, a.kode_identitas, '-'), coalesce(a.nama,'-'), "
                    + " coalesce(a.limit_kredit,0), coalesce(pi.piutang,0), "
                    + " coalesce(a.limit_kredit,0) - coalesce(pi.piutang,0) as sisa "
                    + " from koperasi.anggota_koperasi a "
                    + " left join ( select u.anggota_koperasi as aid, sum(u.nilai_piutang) as piutang "
                    + "   from (" + sqlSumberKasbon() + ") u " + ws + " group by u.anggota_koperasi ) pi on pi.aid = a.id "
                    + w
                    + " and ( coalesce(a.limit_kredit,0) > 0 or coalesce(pi.piutang,0) > 0 ) "
                    + " order by sisa asc ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Pelanggan","text"));
                kolom.add(new Kolom("Limit Kredit","num")); kolom.add(new Kolom("Piutang Terpakai","num")); kolom.add(new Kolom("Sisa Kredit","num"));

            } else if ("akn_ppn_rekap".equals(r)) {
                judul = "Rekapitulasi PPN (Keluaran & Masukan)";
                catatan = "Aktivitas akun PPN dari jurnal TERPOSTING Satuan Kerja kantin. Saldo = Kredit - Debet; total baris = PPN Kurang/(Lebih) Bayar. Akun PPN dikenali dari NAMA akun.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, "
                    + " case when lower(coalesce(d.nama,'')) like '%masuk%' then 'PPN Masukan' when lower(coalesce(d.nama,'')) like '%keluar%' then 'PPN Keluaran' else 'PPN' end as jenis, "
                    + " coalesce(sum(a.debet),0), coalesce(sum(a.kredit),0), coalesce(sum(a.kredit),0)-coalesce(sum(a.debet),0) as saldo "
                    + FROM_LEDGER + w + FILTER_PPN_ANY
                    + " group by d.kode, d.nama having coalesce(sum(a.debet),0)<>0 or coalesce(sum(a.kredit),0)<>0 order by jenis, d.kode ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text")); kolom.add(new Kolom("Jenis","text"));
                kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num")); kolom.add(new Kolom("Saldo (Net)","num"));

            } else if ("akn_ppn_keluaran".equals(r)) {
                judul = "Rincian PPN Keluaran (Penjualan)"; grupIdx = 0;
                catatan = "Rincian baris jurnal pada akun PPN Keluaran (PPN dipungut atas penjualan) dari jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select (d.kode || ' - ' || d.nama) as akun, cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0), coalesce(a.kredit,0) " + FROM_LEDGER + w + FILTER_PPN_KELUARAN
                    + " order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"text","tgl","text","text","num","num"};
                kolom.add(new Kolom("Akun","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit (PPN)","num"));

            } else if ("akn_ppn_masukan".equals(r)) {
                judul = "Rincian PPN Masukan (Pembelian)"; grupIdx = 0;
                catatan = "Rincian baris jurnal pada akun PPN Masukan (PPN dibayar atas pembelian) dari jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select (d.kode || ' - ' || d.nama) as akun, cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0), coalesce(a.kredit,0) " + FROM_LEDGER + w + FILTER_PPN_MASUKAN
                    + " order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"text","tgl","text","text","num","num"};
                kolom.add(new Kolom("Akun","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet (PPN)","num")); kolom.add(new Kolom("Kredit","num"));

            } else if ("akn_anggaran".equals(r)) {
                judul = "Anggaran vs Realisasi (RAB)";
                long satkerAng = kantinSatkerId(session);
                int thnAng = -1;
                try { if (tglMulai!=null && tglMulai.trim().length()>=4) thnAng = Integer.parseInt(tglMulai.trim().substring(0,4)); else if (tglSampai!=null && tglSampai.trim().length()>=4) thnAng = Integer.parseInt(tglSampai.trim().substring(0,4)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LaporanKantinUtil.java:1937");}
                catatan = "Serapan anggaran (RAB / Workspace) Satuan Kerja kantin. Tahun diambil dari filter Tgl Mulai/Sampai (kosong = semua tahun). Hanya baris rincian (leaf).";
                prm.put("satkerAng", Long.valueOf(satkerAng));
                prm.put("thnAng", Integer.valueOf(thnAng));
                sql = "select w.kode, w.nama, coalesce(w.harga_total,0), coalesce(w.realisasi_total,0), "
                    + " coalesce(w.harga_total,0)-coalesce(w.realisasi_total,0) as sisa, "
                    + " case when coalesce(w.harga_total,0)=0 then '-' else round(cast(coalesce(w.realisasi_total,0)*100.0/w.harga_total as numeric),1)||'%' end "
                    + " from rab.workspace w where coalesce(w.aktif,true)=true and coalesce(w.leaf,false)=true "
                    + " and ( cast(:satkerAng as bigint) = -1 or w.satuan_kerja = :satkerAng ) "
                    + " and ( :thnAng = -1 or w.tahun_workspace = :thnAng ) order by w.kode ";
                tipe = new String[]{"text","text","num","num","num","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Uraian","text")); kolom.add(new Kolom("Anggaran","num"));
                kolom.add(new Kolom("Realisasi","num")); kolom.add(new Kolom("Sisa","num")); kolom.add(new Kolom("% Serap","text"));

            } else if ("akn_gaji_rincian".equals(r)) {
                judul = "Rincian Gaji Karyawan"; grupIdx = 0;
                catatan = "Rincian komponen gaji per karyawan (modul Payroll, seluruh instansi — tidak terbatas kantin). Periode = tanggal bayar gaji.";
                StringBuilder w = new StringBuilder(" where pg.id is not null ");
                w.append(klausaTanggal("x.tanggal_bayar_gaji", tglMulai, tglSampai, prm));
                sql = "select (coalesce(pg.nama,'-') || ' (' || coalesce(pg.niplama,'-') || ')') as pegawai, "
                    + " coalesce(x.komponengaji,'-'), cast(x.tanggal_bayar_gaji as date), coalesce(x.nilaifinal, x.nilai, 0) "
                    + " from payroll.pembayaran_gaji_punya_pegawai x join public.pegawai pg on pg.id = x.pegawai " + w
                    + " order by pg.nama, x.tanggal_bayar_gaji, x.id ";
                tipe = new String[]{"text","text","tgl","num"};
                kolom.add(new Kolom("Pegawai","text")); kolom.add(new Kolom("Komponen","text")); kolom.add(new Kolom("Tgl Bayar","tgl")); kolom.add(new Kolom("Nilai","num"));

            } else if ("akn_gaji_komponen".equals(r)) {
                judul = "Porsi Gaji per Komponen";
                catatan = "Rekap total nilai gaji per komponen (Payroll seluruh instansi). Periode = tanggal bayar gaji.";
                StringBuilder w = new StringBuilder(" where true ");
                w.append(klausaTanggal("x.tanggal_bayar_gaji", tglMulai, tglSampai, prm));
                sql = "select coalesce(x.komponengaji,'-'), count(distinct x.pegawai), coalesce(sum(coalesce(x.nilaifinal,x.nilai,0)),0) "
                    + " from payroll.pembayaran_gaji_punya_pegawai x " + w
                    + " group by x.komponengaji order by 3 desc ";
                tipe = new String[]{"text","num","num"};
                kolom.add(new Kolom("Komponen Gaji","text")); kolom.add(new Kolom("Jml Pegawai","num")); kolom.add(new Kolom("Total","num"));

            } else if ("akn_gaji_pph".equals(r)) {
                judul = "Pajak Penghasilan (PPh) Karyawan";
                catatan = "Komponen gaji bertanda PPh/pajak per karyawan (Payroll seluruh instansi). Periode = tanggal bayar gaji.";
                StringBuilder w = new StringBuilder(" where ( lower(coalesce(x.komponengaji,'')) like '%pph%' or lower(coalesce(x.komponengaji,'')) like '%pajak%' ) ");
                w.append(klausaTanggal("x.tanggal_bayar_gaji", tglMulai, tglSampai, prm));
                sql = "select coalesce(pg.niplama,'-'), coalesce(pg.nama,'-'), coalesce(sum(coalesce(x.nilaifinal,x.nilai,0)),0) as pph "
                    + " from payroll.pembayaran_gaji_punya_pegawai x join public.pegawai pg on pg.id = x.pegawai " + w
                    + " group by pg.niplama, pg.nama having coalesce(sum(coalesce(x.nilaifinal,x.nilai,0)),0) <> 0 order by pph desc ";
                tipe = new String[]{"text","text","num"};
                kolom.add(new Kolom("NIP","text")); kolom.add(new Kolom("Pegawai","text")); kolom.add(new Kolom("Total PPh","num"));

            } else if ("akn_rekon_ikhtisar".equals(r)) {
                judul = "Ikhtisar Rekonsiliasi Bank";
                long satkerRk = kantinSatkerId(session);
                prm.put("satkerRk", Long.valueOf(satkerRk));
                catatan = "Saldo Buku (jurnal terposting) vs Saldo Rekening Koran (mutasi bank yang dientri) per akun bank, s/d tanggal 'Sampai'. Selisih != 0 = perlu ditelusuri (setoran dalam perjalanan / cek beredar / biaya bank belum dicatat).";
                String sdK = klausaSampai("m.tanggal", tglSampai, prm);
                String sdB = klausaSampai("a.tanggal_transaksi", tglSampai, prm);
                String tkK = kondToko("m.toko", tokoId, prm);
                sql = "select d.kode, d.nama, coalesce(bk.buku,0), coalesce(mk.koran,0), coalesce(bk.buku,0)-coalesce(mk.koran,0) as selisih "
                    + " from akunting.akun d "
                    + " join ( select m.akun_bank as aid, sum(coalesce(m.masuk,0)-coalesce(m.keluar,0)) as koran "
                    + "        from koperasi.mutasi_rekening_koran m where m.akun_bank is not null " + sdK + tkK + " group by m.akun_bank ) mk on mk.aid = d.id "
                    + " left join ( select a.akun as aid, sum(coalesce(a.debet,0)-coalesce(a.kredit,0)) as buku "
                    + "        from akunting.transaksi a join akunting.grup_transaksi a1 on a1.id = a.grup_transaksi "
                    + "        where a1.posting_history is not null and ( cast(:satkerRk as bigint) = -1 or a1.satuan_kerja = :satkerRk ) " + sdB
                    + "        group by a.akun ) bk on bk.aid = d.id "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Akun Bank","text"));
                kolom.add(new Kolom("Saldo Buku","num")); kolom.add(new Kolom("Saldo Rek. Koran","num")); kolom.add(new Kolom("Selisih","num"));

            } else if ("akn_rekon_belum".equals(r)) {
                judul = "Mutasi Rekening Koran Belum Cocok"; grupIdx = 0;
                catatan = "Baris rekening koran bank yang BELUM dicocokkan (belum direkonsiliasi) dengan buku, dikelompokkan per akun bank. Kelola & centang cocok di menu Pengaturan > Rekening Koran (Rekonsiliasi).";
                StringBuilder w = new StringBuilder(" where coalesce(m.sudahrekon,false) = false ");
                w.append(klausaSampai("m.tanggal", tglSampai, prm));
                w.append(kondToko("m.toko", tokoId, prm));
                sql = "select coalesce(m.namaakunbank, cast(m.akun_bank as text), '-') as akun, cast(m.tanggal as date), "
                    + " coalesce(m.keterangan,''), coalesce(m.referensi,''), coalesce(m.masuk,0), coalesce(m.keluar,0) "
                    + " from koperasi.mutasi_rekening_koran m " + w + " order by m.namaakunbank, m.tanggal, m.id ";
                tipe = new String[]{"text","tgl","text","text","num","num"};
                kolom.add(new Kolom("Akun Bank","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("Keterangan","text"));
                kolom.add(new Kolom("Referensi","text")); kolom.add(new Kolom("Masuk","num")); kolom.add(new Kolom("Keluar","num"));

            // ===================== AKUNTANSI (Buku Besar NYATA — meniru Accurate) =====================
            } else if ("akn_jurnal".equals(r) || "lk_jurnal".equals(r)) {
                judul = "Keseluruhan Jurnal (Jurnal Umum)";
                catatan = "Seluruh baris jurnal TERPOSTING pada Satuan Kerja kantin (konfigurasi 'satuan_kerja_kantin'). Sumber: akunting.transaksi. Isi Tgl Mulai/Sampai utk membatasi periode.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), d.kode, d.nama, coalesce(a.keterangan,''), "
                    + " coalesce(a.debet,0), coalesce(a.kredit,0) " + FROM_LEDGER + w
                    + " order by a.tanggal_transaksi, a.kode, d.kode ";
                tipe = new String[]{"tgl","text","text","text","text","num","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Kode Akun","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num"));

            } else if ("akn_buku_besar".equals(r) || "lk_bukubesar".equals(r)) {
                judul = "Buku Besar (per Akun)"; grupIdx = 0;
                catatan = "Mutasi tiap akun dari jurnal TERPOSTING Satuan Kerja kantin, dengan subtotal Debet/Kredit per akun.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select (d.kode || ' - ' || d.nama) as akun, cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0), coalesce(a.kredit,0) " + FROM_LEDGER + w
                    + " order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"text","tgl","text","text","num","num"};
                kolom.add(new Kolom("Akun","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num"));

            } else if ("lk_bukubesartgl".equals(r)) {
                judul = "Buku Besar per Tanggal"; grupIdx = 0;
                catatan = "Mutasi jurnal TERPOSTING dikelompokkan per TANGGAL (bukan per akun), dengan subtotal Debet/Kredit tiap tanggal. Sumber sama dengan Buku Besar.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select to_char(a.tanggal_transaksi, 'DD-MM-YYYY') as tgl, d.kode, d.nama, coalesce(a.kode,'-'), "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0), coalesce(a.kredit,0) " + FROM_LEDGER + w
                    + " order by a.tanggal_transaksi, d.kode, a.id ";
                tipe = new String[]{"text","text","text","text","text","num","num"};
                kolom.add(new Kolom("Tanggal","text")); kolom.add(new Kolom("Kode Akun","text"));
                kolom.add(new Kolom("Nama Akun","text")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Keterangan","text"));
                kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num"));

            } else if ("lk_neracalajur".equals(r)) {
                judul = "Neraca Lajur (Kertas Kerja)";
                catatan = "Kertas kerja dari jurnal TERPOSTING: Neraca Saldo, Penyesuaian, Neraca Saldo setelah Penyesuaian, "
                    + "lalu dipisah ke kolom Laba Rugi / Neraca menurut Kelompok Laporan. Akun yang BELUM dipetakan tetap "
                    + "muncul di kolom NSD tetapi TIDAK masuk kolom Laba Rugi maupun Neraca (lihat 'Diagnosa Pemetaan Akun').";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                // Klasifikasi sengaja dipakai sebagai subkueri EXISTS, BUKAN join seperti JOIN_KLAS:
                // satu akun boleh terdaftar di beberapa kelompok laporan, dan join akan MENGGANDAKAN
                // barisnya sehingga saldo tiap akun terhitung berkali-kali.
                String klasAwal = " exists (select 1 from akunting.kelompok_laporan_punya_akun b "
                    + " join akunting.kelompok_laporan c on c.id = b.kelompok_laporan "
                    + " join akunting.jenis_laporan f on f.id = c.jenis_laporan "
                    + " where b.akun = d.id and (c.aktif is null or c.aktif) and ";
                // Predikat huruf-per-huruf sama dengan akn_laba_rugi dan akn_neraca supaya satu akun
                // tidak pernah masuk kolom yang berbeda antara neraca lajur dan laporan pokoknya.
                String klasLR = klasAwal + " ( lower(coalesce(f.keterangan,'')) like '%laba%' "
                    + " or lower(coalesce(f.keterangan,'')) like '%rugi%' "
                    + " or lower(coalesce(f.keterangan,'')) like '%pendapatan%' "
                    + " or lower(coalesce(f.keterangan,'')) like '%beban%' "
                    + " or lower(coalesce(f.keterangan,'')) like '%biaya%' ) ) ";
                String klasNR = klasAwal + " lower(coalesce(f.keterangan,'')) like '%neraca%' ) ";
                // jenis_transaksi 7 = Jurnal Penyesuaian (angka yang sama dipakai DasboardAkuntansi).
                String ns = " (coalesce(sum(case when coalesce(a1.jenis_transaksi,0) <> 7 then a.debet - a.kredit else 0 end),0)) ";
                String nsd = " (coalesce(sum(a.debet - a.kredit),0)) ";
                sql = "select d.kode, d.nama, "
                    + " case when " + ns + " > 0 then " + ns + " else 0 end, "
                    + " case when " + ns + " < 0 then -" + ns + " else 0 end, "
                    + " coalesce(sum(case when coalesce(a1.jenis_transaksi,0) = 7 then a.debet else 0 end),0), "
                    + " coalesce(sum(case when coalesce(a1.jenis_transaksi,0) = 7 then a.kredit else 0 end),0), "
                    + " case when " + nsd + " > 0 then " + nsd + " else 0 end, "
                    + " case when " + nsd + " < 0 then -" + nsd + " else 0 end, "
                    + " case when " + klasLR + " and " + nsd + " > 0 then " + nsd + " else 0 end, "
                    + " case when " + klasLR + " and " + nsd + " < 0 then -" + nsd + " else 0 end, "
                    + " case when " + klasNR + " and " + nsd + " > 0 then " + nsd + " else 0 end, "
                    + " case when " + klasNR + " and " + nsd + " < 0 then -" + nsd + " else 0 end "
                    + FROM_LEDGER + w
                    + " group by d.id, d.kode, d.nama "
                    + " having coalesce(sum(a.debet),0) <> 0 or coalesce(sum(a.kredit),0) <> 0 "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num","num","num","num","num","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("NS Debet","num")); kolom.add(new Kolom("NS Kredit","num"));
                kolom.add(new Kolom("Penyesuaian Debet","num")); kolom.add(new Kolom("Penyesuaian Kredit","num"));
                kolom.add(new Kolom("NSD Debet","num")); kolom.add(new Kolom("NSD Kredit","num"));
                kolom.add(new Kolom("Laba Rugi Debet","num")); kolom.add(new Kolom("Laba Rugi Kredit","num"));
                kolom.add(new Kolom("Neraca Debet","num")); kolom.add(new Kolom("Neraca Kredit","num"));

            } else if ("akn_ringkasan_bb".equals(r)) {
                judul = "Ringkasan Buku Besar";
                catatan = "Total Debet, Kredit, dan Saldo (D-K) per akun dari jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, coalesce(sum(a.debet),0), coalesce(sum(a.kredit),0), "
                    + " coalesce(sum(a.debet),0)-coalesce(sum(a.kredit),0) " + FROM_LEDGER + w
                    + " group by d.id, d.kode, d.nama "
                    + " having coalesce(sum(a.debet),0) <> 0 or coalesce(sum(a.kredit),0) <> 0 "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num")); kolom.add(new Kolom("Saldo (D-K)","num"));

            } else if ("akn_neraca_saldo".equals(r) || "lk_trial".equals(r)) {
                judul = "Neraca Percobaan (Neraca Saldo / Trial Balance)";
                catatan = "Saldo tiap akun dipisah kolom Debet/Kredit. Total Debet HARUS sama dengan total Kredit (seimbang). Jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, "
                    + " case when coalesce(sum(a.debet),0)-coalesce(sum(a.kredit),0) > 0 then coalesce(sum(a.debet),0)-coalesce(sum(a.kredit),0) else 0 end, "
                    + " case when coalesce(sum(a.debet),0)-coalesce(sum(a.kredit),0) < 0 then coalesce(sum(a.kredit),0)-coalesce(sum(a.debet),0) else 0 end "
                    + FROM_LEDGER + w
                    + " group by d.id, d.kode, d.nama "
                    + " having coalesce(sum(a.debet),0) <> 0 or coalesce(sum(a.kredit),0) <> 0 "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Saldo Debet","num")); kolom.add(new Kolom("Saldo Kredit","num"));

            } else if ("akn_daftar_akun".equals(r)) {
                judul = "Daftar Akun Perkiraan (Bagan Akun)";
                catatan = "Seluruh akun beserta klasifikasi Kelompok Laporan (Neraca/Laba Rugi/Arus Kas). Akun bertanda '- belum dipetakan -' TIDAK muncul di Neraca/Laba Rugi.";
                sql = "select d.kode, d.nama, "
                    + " coalesce((select string_agg(distinct f.keterangan, ', ') "
                    + "   from akunting.kelompok_laporan_punya_akun b "
                    + "   join akunting.kelompok_laporan c on c.id = b.kelompok_laporan "
                    + "   join akunting.jenis_laporan f on f.id = c.jenis_laporan "
                    + "   where b.akun = d.id and (c.aktif is null or c.aktif)), '- belum dipetakan -') "
                    + " from akunting.akun d order by d.kode ";
                tipe = new String[]{"text","text","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text")); kolom.add(new Kolom("Klasifikasi Laporan","text"));

            } else if ("akn_rekening_koran".equals(r)) {
                judul = "Rekening Koran (Kas & Bank)"; grupIdx = 0;
                catatan = "Mutasi akun Kas/Bank per akun (Debet = uang masuk, Kredit = uang keluar) dari jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select (d.kode || ' - ' || d.nama) as akun, cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0), coalesce(a.kredit,0) " + FROM_LEDGER + w + FILTER_KASBANK
                    + " order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"text","tgl","text","text","num","num"};
                kolom.add(new Kolom("Akun Kas/Bank","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Penerimaan","num")); kolom.add(new Kolom("Pengeluaran","num"));

            } else if ("akn_penerimaan".equals(r)) {
                judul = "Ringkasan Daftar Penerimaan (Kas & Bank Masuk)";
                catatan = "Total uang MASUK (Debet) per akun Kas/Bank pada periode (jurnal TERPOSTING Satuan Kerja kantin).";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, coalesce(sum(a.debet),0) " + FROM_LEDGER + w + FILTER_KASBANK
                    + " group by d.id, d.kode, d.nama having coalesce(sum(a.debet),0) > 0 order by 3 desc ";
                tipe = new String[]{"text","text","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Akun Kas/Bank","text")); kolom.add(new Kolom("Penerimaan","num"));

            } else if ("akn_pembayaran".equals(r)) {
                judul = "Ringkasan Daftar Pembayaran (Kas & Bank Keluar)";
                catatan = "Total uang KELUAR (Kredit) per akun Kas/Bank pada periode (jurnal TERPOSTING Satuan Kerja kantin).";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, coalesce(sum(a.kredit),0) " + FROM_LEDGER + w + FILTER_KASBANK
                    + " group by d.id, d.kode, d.nama having coalesce(sum(a.kredit),0) > 0 order by 3 desc ";
                tipe = new String[]{"text","text","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Akun Kas/Bank","text")); kolom.add(new Kolom("Pembayaran","num"));

            } else if ("akn_histori_bb".equals(r)) {
                judul = "Histori Buku Besar (Saldo Berjalan)";
                catatan = "Mutasi tiap akun berikut SALDO BERJALAN (running balance) per baris, dari jurnal TERPOSTING Satuan Kerja kantin.";
                H.grandTotal = false; // saldo berjalan tak boleh di-grand-total
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select (d.kode || ' - ' || d.nama) as akun, cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0), coalesce(a.kredit,0), "
                    + " sum(coalesce(a.debet,0)-coalesce(a.kredit,0)) over (partition by d.id order by a.tanggal_transaksi, a.id "
                    + "   rows between unbounded preceding and current row) as saldo " + FROM_LEDGER + w
                    + " order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"text","tgl","text","text","num","num","num"};
                kolom.add(new Kolom("Akun","text")); kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num")); kolom.add(new Kolom("Saldo Berjalan","num"));

            } else if ("akn_ringkasan_beban".equals(r)) {
                judul = "Ringkasan Pencatatan Beban";
                catatan = "Total beban per akun (jenis Laba Rugi berklasifikasi Beban/Biaya/HPP) dari jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, " + LABEL_KLAS + " as kelompok, "
                    + " coalesce(sum(a.debet),0)-coalesce(sum(a.kredit),0) as beban " + FROM_LEDGER + JOIN_KLAS + w + FILTER_BEBAN
                    + " group by d.kode, d.nama, c.keterangan, m.keterangan "
                    + " having coalesce(sum(a.debet),0)-coalesce(sum(a.kredit),0) <> 0 order by 4 desc ";
                tipe = new String[]{"text","text","text","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text")); kolom.add(new Kolom("Kelompok","text")); kolom.add(new Kolom("Total Beban","num"));

            } else if ("akn_rincian_beban".equals(r)) {
                judul = "Rincian Beban Pembayaran"; grupIdx = 2;
                catatan = "Rincian baris jurnal yang membebani akun Beban/Biaya (dikelompokkan per akun) dari jurnal TERPOSTING Satuan Kerja kantin.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select cast(a.tanggal_transaksi as date), coalesce(a.kode,'-'), (d.kode || ' - ' || d.nama) as akun, "
                    + " coalesce(a.keterangan,''), coalesce(a.debet,0) " + FROM_LEDGER + JOIN_KLAS + w + FILTER_BEBAN
                    + " and coalesce(a.debet,0) > 0 order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"tgl","text","text","text","num"};
                kolom.add(new Kolom("Tanggal","tgl")); kolom.add(new Kolom("No. Jurnal","text")); kolom.add(new Kolom("Akun Beban","text"));
                kolom.add(new Kolom("Keterangan","text")); kolom.add(new Kolom("Nilai","num"));

            } else if ("akn_laba_rugi".equals(r)) {
                H.judul = "Laba Rugi (Berbasis Jurnal Akuntansi)";
                H.catatan = "Dihitung dari jurnal TERPOSTING Satuan Kerja kantin memakai klasifikasi Kelompok Laporan jenis 'Laba Rugi'. "
                    + "Akun Pendapatan/Beban yang BELUM dipetakan tidak muncul (lihat 'Diagnosa Pemetaan Akun'). Laba Bersih = total Kredit - total Debet akun Laba Rugi.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};
                Map<String,Object> pl = new LinkedHashMap<String,Object>();
                String w = klausaLedger(session, tglMulai, tglSampai, pl);
                String q = "select " + LABEL_KLAS + " as kelompok, d.kode, d.nama, "
                    + " coalesce(sum(a.kredit),0) - coalesce(sum(a.debet),0) as natural_kredit, "
                    + " coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) as natural_debet, "
                    + TAG_KLAS + " as tag "
                    + FROM_LEDGER + JOIN_KLAS
                    + w
                    + " and (c.aktif is null or c.aktif) "
                    + " and ( lower(coalesce(f.keterangan,'')) like '%laba%' or lower(coalesce(f.keterangan,'')) like '%rugi%' "
                    + "       or lower(coalesce(f.keterangan,'')) like '%pendapatan%' or lower(coalesce(f.keterangan,'')) like '%beban%' or lower(coalesce(f.keterangan,'')) like '%biaya%' ) "
                    + " group by c.keterangan, m.keterangan, d.kode, d.nama, f.keterangan, c.urut "
                    + " order by coalesce(c.urut,0), kelompok, d.kode ";
                try {
                    SQLQuery lq = session.createSQLQuery(q);
                    for (Map.Entry<String,Object> e : pl.entrySet()) { lq.setParameter(e.getKey(), e.getValue()); }
                    List<?> lrows = lq.list();
                    Map<String, List<Object[]>> grpPend = new LinkedHashMap<String, List<Object[]>>();
                    Map<String, List<Object[]>> grpBeb = new LinkedHashMap<String, List<Object[]>>();
                    double totalPend = 0.0, totalBeban = 0.0, labaBersih = 0.0;
                    for (Object ro : lrows) {
                        Object[] rr = (Object[]) ro;
                        String kelompok = rr[0] == null ? "(Tanpa Kelompok)" : rr[0].toString();
                        String kode = rr[1] == null ? "" : rr[1].toString();
                        String nama = rr[2] == null ? "" : rr[2].toString();
                        double natK = (rr[3] instanceof Number) ? ((Number) rr[3]).doubleValue() : 0.0;
                        double natD = (rr[4] instanceof Number) ? ((Number) rr[4]).doubleValue() : 0.0;
                        String tag = rr[5] == null ? "" : rr[5].toString();
                        labaBersih += natK;
                        boolean beban = tag.contains("beban") || tag.contains("biaya") || tag.contains("hpp")
                            || tag.contains("harga pokok") || tag.contains("pengeluaran");
                        if (beban) {
                            totalBeban += natD;
                            if (!grpBeb.containsKey(kelompok)) { grpBeb.put(kelompok, new ArrayList<Object[]>()); }
                            grpBeb.get(kelompok).add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(natD) });
                        } else {
                            totalPend += natK;
                            if (!grpPend.containsKey(kelompok)) { grpPend.put(kelompok, new ArrayList<Object[]>()); }
                            grpPend.get(kelompok).add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(natK) });
                        }
                    }
                    if (lrows.isEmpty()) {
                        H.baris.add(new Object[]{"Belum ada data. Pastikan transaksi kantin sudah DIPOSTING ke jurnal & akun dipetakan ke Kelompok Laporan.", null});
                        return H;
                    }
                    H.baris.add(new Object[]{"PENDAPATAN", null});
                    for (Map.Entry<String, List<Object[]>> e : grpPend.entrySet()) {
                        H.baris.add(new Object[]{"  " + e.getKey(), null});
                        double sub = 0.0;
                        for (Object[] x : e.getValue()) { H.baris.add(x); sub += ((Number) x[1]).doubleValue(); }
                        H.baris.add(new Object[]{"  Subtotal " + e.getKey(), Double.valueOf(sub)});
                    }
                    H.baris.add(new Object[]{"TOTAL PENDAPATAN", Double.valueOf(totalPend)});
                    H.baris.add(new Object[]{"BEBAN", null});
                    for (Map.Entry<String, List<Object[]>> e : grpBeb.entrySet()) {
                        H.baris.add(new Object[]{"  " + e.getKey(), null});
                        double sub = 0.0;
                        for (Object[] x : e.getValue()) { H.baris.add(x); sub += ((Number) x[1]).doubleValue(); }
                        H.baris.add(new Object[]{"  Subtotal " + e.getKey(), Double.valueOf(sub)});
                    }
                    H.baris.add(new Object[]{"TOTAL BEBAN", Double.valueOf(totalBeban)});
                    H.baris.add(new Object[]{"LABA (RUGI) BERSIH", Double.valueOf(labaBersih)});
                } catch (Exception e) {
                    H.status = "99"; H.message = "Gagal menyusun Laba Rugi berbasis jurnal: " + e.getMessage();
                }
                return H;

            } else if ("akn_laporan_aktivitas".equals(r)) {
                H.judul = "Laporan Aktivitas (Perhitungan Surplus/Defisit)";
                H.catatan = "Format laporan aktivitas nirlaba/yayasan dari jurnal TERPOSTING + klasifikasi Kelompok "
                    + "Laporan jenis 'Laba Rugi': Pendapatan, Harga Pokok Penjualan, Laba Kotor beserta Contribution "
                    + "Margin, Biaya Tetap, lalu Surplus (Defisit) beserta Profit Margin. Akun beban yang kelompoknya "
                    + "bertanda HPP / harga pokok dihitung sebagai HPP, sisanya sebagai biaya tetap. Akun yang BELUM "
                    + "dipetakan tidak muncul (lihat 'Diagnosa Pemetaan Akun').";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};
                Map<String,Object> pa = new LinkedHashMap<String,Object>();
                String wa = klausaLedger(session, tglMulai, tglSampai, pa);
                String qa = "select " + LABEL_KLAS + " as kelompok, d.kode as kode, d.nama as nama, "
                    + " coalesce(sum(a.kredit),0) - coalesce(sum(a.debet),0) as natural_kredit, "
                    + " coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) as natural_debet, "
                    + TAG_KLAS + " as tag "
                    + FROM_LEDGER + JOIN_KLAS + wa
                    + " and (c.aktif is null or c.aktif) "
                    + " and ( lower(coalesce(f.keterangan,'')) like '%laba%' or lower(coalesce(f.keterangan,'')) like '%rugi%' "
                    + "       or lower(coalesce(f.keterangan,'')) like '%pendapatan%' "
                    + "       or lower(coalesce(f.keterangan,'')) like '%beban%' or lower(coalesce(f.keterangan,'')) like '%biaya%' ) "
                    + " group by c.keterangan, m.keterangan, d.kode, d.nama, f.keterangan, c.urut "
                    + " order by coalesce(c.urut,0), kelompok, d.kode ";
                try {
                    SQLQuery aq = session.createSQLQuery(qa);
                    for (Map.Entry<String,Object> e : pa.entrySet()) { aq.setParameter(e.getKey(), e.getValue()); }
                    List<?> arows = aq.list();
                    if (arows.isEmpty()) {
                        H.baris.add(new Object[]{"Belum ada data. Pastikan transaksi sudah DIPOSTING ke jurnal & akun "
                            + "dipetakan ke Kelompok Laporan jenis Laba Rugi.", null});
                        return H;
                    }
                    Map<String, List<Object[]>> gPend = new LinkedHashMap<String, List<Object[]>>();
                    List<Object[]> barisHpp = new ArrayList<Object[]>();
                    Map<String, List<Object[]>> gTetap = new LinkedHashMap<String, List<Object[]>>();
                    double totalPend = 0.0, totalHpp = 0.0, totalTetap = 0.0;
                    for (Object ro : arows) {
                        Object[] rr = (Object[]) ro;
                        String kelompok = rr[0] == null ? "(Tanpa Kelompok)" : rr[0].toString();
                        String kode = rr[1] == null ? "" : rr[1].toString();
                        String nama = rr[2] == null ? "" : rr[2].toString();
                        double natK = (rr[3] instanceof Number) ? ((Number) rr[3]).doubleValue() : 0.0;
                        double natD = (rr[4] instanceof Number) ? ((Number) rr[4]).doubleValue() : 0.0;
                        String tag = rr[5] == null ? "" : rr[5].toString();
                        boolean hpp = tag.contains("hpp") || tag.contains("harga pokok");
                        boolean beban = hpp || tag.contains("beban") || tag.contains("biaya")
                            || tag.contains("pengeluaran");
                        if (hpp) {
                            totalHpp += natD;
                            barisHpp.add(new Object[]{ "      " + kode + " " + nama, Double.valueOf(natD) });
                        } else if (beban) {
                            totalTetap += natD;
                            if (!gTetap.containsKey(kelompok)) { gTetap.put(kelompok, new ArrayList<Object[]>()); }
                            gTetap.get(kelompok).add(new Object[]{ "      " + kode + " " + nama, Double.valueOf(natD) });
                        } else {
                            totalPend += natK;
                            if (!gPend.containsKey(kelompok)) { gPend.put(kelompok, new ArrayList<Object[]>()); }
                            gPend.get(kelompok).add(new Object[]{ "      " + kode + " " + nama, Double.valueOf(natK) });
                        }
                    }
                    double labaKotor = totalPend - totalHpp;
                    double surplus = labaKotor - totalTetap;

                    H.baris.add(new Object[]{"A. PENDAPATAN", null});
                    for (Map.Entry<String, List<Object[]>> e : gPend.entrySet()) {
                        H.baris.add(new Object[]{"    " + e.getKey(), null});
                        double sub = 0.0;
                        for (Object[] x : e.getValue()) { H.baris.add(x); sub += ((Number) x[1]).doubleValue(); }
                        H.baris.add(new Object[]{"    Subtotal " + e.getKey(), Double.valueOf(sub)});
                    }
                    H.baris.add(new Object[]{"JUMLAH PENDAPATAN", Double.valueOf(totalPend)});
                    H.baris.add(new Object[]{"B. BIAYA", null});
                    H.baris.add(new Object[]{"  1. HARGA POKOK PENJUALAN (HPP)", null});
                    for (Object[] x : barisHpp) { H.baris.add(x); }
                    H.baris.add(new Object[]{"     Jumlah Harga Pokok Penjualan", Double.valueOf(totalHpp)});
                    H.baris.add(new Object[]{"  2. LABA (RUGI) KOTOR", Double.valueOf(labaKotor)});
                    H.baris.add(new Object[]{"     Contribution Margin (%)",
                        totalPend == 0.0 ? null : Double.valueOf(labaKotor * 100.0 / totalPend)});
                    H.baris.add(new Object[]{"  3. BIAYA TETAP", null});
                    for (Map.Entry<String, List<Object[]>> e : gTetap.entrySet()) {
                        H.baris.add(new Object[]{"     " + e.getKey(), null});
                        double sub = 0.0;
                        for (Object[] x : e.getValue()) { H.baris.add(x); sub += ((Number) x[1]).doubleValue(); }
                        H.baris.add(new Object[]{"     Subtotal " + e.getKey(), Double.valueOf(sub)});
                    }
                    H.baris.add(new Object[]{"     Jumlah Biaya Tetap", Double.valueOf(totalTetap)});
                    H.baris.add(new Object[]{"  4. SURPLUS (DEFISIT) - LABA (RUGI) USAHA", Double.valueOf(surplus)});
                    H.baris.add(new Object[]{"     Profit Margin (%)",
                        totalPend == 0.0 ? null : Double.valueOf(surplus * 100.0 / totalPend)});
                } catch (Exception e) {
                    H.status = "99"; H.message = "Gagal menyusun Laporan Aktivitas: " + e.getMessage();
                }
                return H;

            } else if ("akn_lr_2periode".equals(r)) {
                judul = "Laba Rugi \u2014 2 Periode (Berbasis Jurnal)"; grupIdx = 0;
                catatan = "Periode berjalan (Tgl Mulai s.d Tgl Sampai) dibandingkan dengan periode SEBELUMNYA yang "
                    + "sama panjang, langsung dari jurnal TERPOSTING. Nilai memakai saldo alami akun "
                    + "(pendapatan positif bila menambah, beban positif bila membebani).";
                String[] sblm = periodeSebelumnya(tglMulai, tglSampai);
                prm.put("p1a", sblm[0]); prm.put("p1b", sblm[1]);
                prm.put("p2a", ada(tglMulai) ? tglMulai.trim() : sblm[0]);
                prm.put("p2b", ada(tglSampai) ? tglSampai.trim() : sblm[1]);
                prm.put("satker", Long.valueOf(kantinSatkerId(session)));
                sql = "select " + LABEL_KLAS + " as kelompok, d.kode, d.nama, "
                    + " sum(case when cast(a.tanggal_transaksi as date) between cast(:p1a as date) and cast(:p1b as date) "
                    + "   then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end) as p1, "
                    + " sum(case when cast(a.tanggal_transaksi as date) between cast(:p2a as date) and cast(:p2b as date) "
                    + "   then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end) as p2, "
                    + " sum(case when cast(a.tanggal_transaksi as date) between cast(:p2a as date) and cast(:p2b as date) "
                    + "   then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end) "
                    + " - sum(case when cast(a.tanggal_transaksi as date) between cast(:p1a as date) and cast(:p1b as date) "
                    + "   then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end) as selisih "
                    + FROM_LEDGER + JOIN_KLAS
                    + " where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) "
                    + " and (c.aktif is null or c.aktif) "
                    + " and ( lower(coalesce(f.keterangan,'')) like '%laba%' or lower(coalesce(f.keterangan,'')) like '%rugi%' ) "
                    + " and cast(a.tanggal_transaksi as date) between cast(:p1a as date) and cast(:p2b as date) "
                    + " group by c.keterangan, m.keterangan, d.kode, d.nama, c.urut "
                    + " having sum(case when cast(a.tanggal_transaksi as date) between cast(:p1a as date) and cast(:p1b as date) "
                    + "   then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end) <> 0 "
                    + "   or sum(case when cast(a.tanggal_transaksi as date) between cast(:p2a as date) and cast(:p2b as date) "
                    + "   then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end) <> 0 "
                    + " order by coalesce(c.urut,0), kelompok, d.kode ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("Kelompok","text")); kolom.add(new Kolom("Kode","text"));
                kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Periode Lalu (" + sblm[0] + " s.d " + sblm[1] + ")","num"));
                kolom.add(new Kolom("Periode Ini","num")); kolom.add(new Kolom("Selisih","num"));

            } else if ("akn_neraca_2tanggal".equals(r)) {
                judul = "Neraca \u2014 2 Tanggal (Berbasis Jurnal)"; grupIdx = 0;
                catatan = "Saldo KUMULATIF tiap akun neraca pada dua tanggal: kolom pertama per Tgl Mulai, kolom "
                    + "kedua per Tgl Sampai, beserta perubahannya. Nilai positif = saldo debet, negatif = saldo kredit.";
                prm.put("t1", ada(tglMulai) ? tglMulai.trim() : "1900-01-01");
                prm.put("t2", ada(tglSampai) ? tglSampai.trim() : "2999-12-31");
                prm.put("satker", Long.valueOf(kantinSatkerId(session)));
                sql = "select " + LABEL_KLAS + " as kelompok, d.kode, d.nama, "
                    + " sum(case when cast(a.tanggal_transaksi as date) <= cast(:t1 as date) "
                    + "   then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end) as s1, "
                    + " sum(case when cast(a.tanggal_transaksi as date) <= cast(:t2 as date) "
                    + "   then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end) as s2, "
                    + " sum(case when cast(a.tanggal_transaksi as date) <= cast(:t2 as date) "
                    + "   then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end) "
                    + " - sum(case when cast(a.tanggal_transaksi as date) <= cast(:t1 as date) "
                    + "   then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end) as perubahan "
                    + FROM_LEDGER + JOIN_KLAS
                    + " where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) "
                    + " and (c.aktif is null or c.aktif) "
                    + " and lower(coalesce(f.keterangan,'')) like '%neraca%' "
                    + " and cast(a.tanggal_transaksi as date) <= cast(:t2 as date) "
                    + " group by c.keterangan, m.keterangan, d.kode, d.nama, c.urut "
                    + " having sum(case when cast(a.tanggal_transaksi as date) <= cast(:t2 as date) "
                    + "   then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end) <> 0 "
                    + "   or sum(case when cast(a.tanggal_transaksi as date) <= cast(:t1 as date) "
                    + "   then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end) <> 0 "
                    + " order by coalesce(c.urut,0), kelompok, d.kode ";
                tipe = new String[]{"text","text","text","num","num","num"};
                kolom.add(new Kolom("Kelompok","text")); kolom.add(new Kolom("Kode","text"));
                kolom.add(new Kolom("Nama Akun","text")); kolom.add(new Kolom("Per Tgl Mulai","num"));
                kolom.add(new Kolom("Per Tgl Sampai","num")); kolom.add(new Kolom("Perubahan","num"));

            } else if ("akn_lr_12bulan".equals(r)) {
                judul = "Laba Rugi \u2014 12 Bulan (Berbasis Jurnal)"; grupIdx = 0;
                catatan = "Dua belas bulan berakhir pada bulan Tgl Sampai, satu kolom per bulan, langsung dari "
                    + "jurnal TERPOSTING. Nilai memakai saldo alami akun.";
                String[] bulan = duaBelasBulan(tglSampai);
                prm.put("satker", Long.valueOf(kantinSatkerId(session)));
                StringBuilder kolomBulan = new StringBuilder();
                for (int ib = 0; ib < 12; ib++) {
                    prm.put("b" + ib, bulan[ib]);
                    kolomBulan.append(", sum(case when to_char(a.tanggal_transaksi,'YYYY-MM') = :b").append(ib)
                        .append(" then coalesce(a.kredit,0) - coalesce(a.debet,0) else 0 end)");
                }
                sql = "select " + LABEL_KLAS + " as kelompok, d.kode, d.nama " + kolomBulan
                    + FROM_LEDGER + JOIN_KLAS
                    + " where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) "
                    + " and (c.aktif is null or c.aktif) "
                    + " and ( lower(coalesce(f.keterangan,'')) like '%laba%' or lower(coalesce(f.keterangan,'')) like '%rugi%' ) "
                    + " and to_char(a.tanggal_transaksi,'YYYY-MM') between :b0 and :b11 "
                    + " group by c.keterangan, m.keterangan, d.kode, d.nama, c.urut "
                    + " order by coalesce(c.urut,0), kelompok, d.kode ";
                tipe = new String[]{"text","text","text","num","num","num","num","num","num","num","num","num","num","num","num"};
                kolom.add(new Kolom("Kelompok","text")); kolom.add(new Kolom("Kode","text"));
                kolom.add(new Kolom("Nama Akun","text"));
                for (int ib = 0; ib < 12; ib++) {
                    kolom.add(new Kolom(bulan[ib], "num"));
                }

            } else if ("akn_neraca_lajur".equals(r)) {
                judul = "Neraca Lajur (Kertas Kerja)";
                catatan = "Kertas kerja akuntansi: saldo tiap akun dari jurnal TERPOSTING s/d Tgl Sampai, lalu "
                    + "dipilah ke kolom Laba Rugi (akun nominal) dan Neraca (akun riil). Total kolom Laba Rugi "
                    + "dan Neraca harus sama-sama seimbang setelah laba/rugi bersih diperhitungkan.";
                prm.put("t2", ada(tglSampai) ? tglSampai.trim() : "2999-12-31");
                prm.put("satker", Long.valueOf(kantinSatkerId(session)));
                sql = "select d.kode, d.nama, "
                    + " case when saldo > 0 then saldo else 0 end as ns_debet, "
                    + " case when saldo < 0 then -saldo else 0 end as ns_kredit, "
                    + " case when jenis = 'LR' and saldo > 0 then saldo else 0 end as lr_debet, "
                    + " case when jenis = 'LR' and saldo < 0 then -saldo else 0 end as lr_kredit, "
                    + " case when jenis = 'NR' and saldo > 0 then saldo else 0 end as nr_debet, "
                    + " case when jenis = 'NR' and saldo < 0 then -saldo else 0 end as nr_kredit "
                    + " from ( select d.kode as kode, d.nama as nama, "
                    + "   coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) as saldo, "
                    + "   case when lower(coalesce(f.keterangan,'')) like '%neraca%' then 'NR' else 'LR' end as jenis "
                    + FROM_LEDGER + JOIN_KLAS
                    + "   where a1.posting_history is not null and ( :satker = -1 or a1.satuan_kerja = :satker ) "
                    + "   and (c.aktif is null or c.aktif) "
                    + "   and cast(a.tanggal_transaksi as date) <= cast(:t2 as date) "
                    + "   group by d.kode, d.nama, f.keterangan "
                    + "   having abs(coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0)) >= 0.005 ) d "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("N. Saldo Debet","num")); kolom.add(new Kolom("N. Saldo Kredit","num"));
                kolom.add(new Kolom("Laba Rugi Debet","num")); kolom.add(new Kolom("Laba Rugi Kredit","num"));
                kolom.add(new Kolom("Neraca Debet","num")); kolom.add(new Kolom("Neraca Kredit","num"));

            } else if ("akn_neraca".equals(r)) {
                H.judul = "Neraca (Berbasis Jurnal Akuntansi)";
                H.catatan = "Saldo KUMULATIF seluruh jurnal TERPOSTING sampai dengan Tgl Sampai (Tgl Mulai diabaikan, "
                    + "karena neraca bersifat kumulatif), dikelompokkan memakai klasifikasi Kelompok Laporan jenis 'Neraca'. "
                    + "Laba (rugi) berjalan dari akun Laba Rugi ditambahkan pada sisi Kewajiban & Modal agar neraca seimbang. "
                    + "Akun yang BELUM dipetakan tetap ditampilkan pada kelompok '(Belum dipetakan ke Kelompok Laporan)' "
                    + "agar neraca tetap seimbang dan kekurangan pemetaan langsung terlihat; rinciannya ada di laporan 'Diagnosa Pemetaan Akun'.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};
                Map<String,Object> pn = new LinkedHashMap<String,Object>();
                String wn = klausaLedgerSampai(session, tglSampai, pn);
                String qn = "select " + LABEL_KLAS + " as kelompok, d.kode, d.nama, "
                    + " coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) as saldo_debet, " + TAG_KLAS + " as tag "
                    + FROM_LEDGER + JOIN_KLAS + wn
                    + " and (c.aktif is null or c.aktif) "
                    + " and lower(coalesce(f.keterangan,'')) like '%neraca%' "
                    + " group by c.keterangan, m.keterangan, d.kode, d.nama, f.keterangan, c.urut "
                    + " having coalesce(sum(a.debet),0) <> 0 or coalesce(sum(a.kredit),0) <> 0 "
                    + " order by coalesce(c.urut,0), kelompok, d.kode ";
                try {
                    SQLQuery nq = session.createSQLQuery(qn);
                    for (Map.Entry<String,Object> e : pn.entrySet()) { nq.setParameter(e.getKey(), e.getValue()); }
                    List<?> nrows = nq.list();
                    Map<String, List<Object[]>> grpAktiva = new LinkedHashMap<String, List<Object[]>>();
                    Map<String, List<Object[]>> grpPasiva = new LinkedHashMap<String, List<Object[]>>();
                    double totalAktiva = 0.0, totalPasiva = 0.0;
                    for (Object ro : nrows) {
                        Object[] rr = (Object[]) ro;
                        String kelompok = rr[0] == null ? "(Tanpa Kelompok)" : rr[0].toString();
                        String kode = rr[1] == null ? "" : rr[1].toString();
                        String nama = rr[2] == null ? "" : rr[2].toString();
                        double saldoD = (rr[3] instanceof Number) ? ((Number) rr[3]).doubleValue() : 0.0;
                        String tag = rr[4] == null ? "" : rr[4].toString();
                        // Urutan cek penting dan tidak boleh dibalik:
                        // 1) EKUITAS lebih dulu -- "ASET BERSIH"/"AKTIVA BERSIH" (istilah ekuitas pada
                        //    laporan nirlaba) mengandung kata "aset" sehingga akan salah masuk AKTIVA
                        //    bila dicek belakangan;
                        // 2) baru AKTIVA -- "piutang" mengandung "utang", jadi harus mendahului PASIVA;
                        // 3) sisanya PASIVA.
                        boolean ekuitas = tag.contains("aset bersih") || tag.contains("aktiva bersih")
                            || tag.contains("ekuitas") || tag.contains("modal") || tag.contains("laba ditahan");
                        boolean aktiva = !ekuitas && (tag.contains("aktiva") || tag.contains("aset")
                            || tag.contains("asset") || tag.contains("harta") || tag.contains("piutang")
                            || tag.contains("persediaan") || tag.contains("kas") || tag.contains("bank")
                            || tag.contains("dibayar dimuka") || tag.contains("dibayar di muka")
                            || tag.contains("uang muka"));
                        boolean pasiva = !aktiva && (ekuitas || tag.contains("kewajiban")
                            || tag.contains("hutang") || tag.contains("utang") || tag.contains("pasiva"));
                        if (!aktiva && !pasiva) { aktiva = saldoD >= 0; }   // cadangan: ikut saldo alami akun
                        if (aktiva) {
                            totalAktiva += saldoD;
                            if (!grpAktiva.containsKey(kelompok)) { grpAktiva.put(kelompok, new ArrayList<Object[]>()); }
                            grpAktiva.get(kelompok).add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(saldoD) });
                        } else {
                            double saldoK = -saldoD;
                            totalPasiva += saldoK;
                            if (!grpPasiva.containsKey(kelompok)) { grpPasiva.put(kelompok, new ArrayList<Object[]>()); }
                            grpPasiva.get(kelompok).add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(saldoK) });
                        }
                    }
                    Map<String,Object> pl2 = new LinkedHashMap<String,Object>();
                    String wl2 = klausaLedgerSampai(session, tglSampai, pl2);
                    double labaBerjalan = angkaTunggal(session,
                        "select coalesce(sum(a.kredit),0) - coalesce(sum(a.debet),0) " + FROM_LEDGER + JOIN_KLAS + wl2
                        + " and (c.aktif is null or c.aktif) "
                        + " and ( lower(coalesce(f.keterangan,'')) like '%laba%' or lower(coalesce(f.keterangan,'')) like '%rugi%' ) ", pl2);
                    // Akun yang BELUM dipetakan tetap ditampilkan: tanpa ini neraca pasti timpang
                    // (mis. akun KAS belum dipetakan -> TOTAL AKTIVA 0). Sisi ditentukan dari saldo alami,
                    // dan pemakai langsung melihat apa yang masih perlu dipetakan.
                    Map<String,Object> pu = new LinkedHashMap<String,Object>();
                    String wu = klausaLedgerSampai(session, tglSampai, pu);
                    String qu = "select d.kode, d.nama, coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) as saldo_debet "
                        + FROM_LEDGER + wu
                        + " and not exists ( select 1 from akunting.kelompok_laporan_punya_akun b2 "
                        + "     join akunting.kelompok_laporan c2 on c2.id = b2.kelompok_laporan "
                        + "     where b2.akun = d.id and (c2.aktif is null or c2.aktif) ) "
                        + " group by d.kode, d.nama "
                        + " having coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) <> 0 order by d.kode ";
                    SQLQuery uq = session.createSQLQuery(qu);
                    for (Map.Entry<String,Object> e : pu.entrySet()) { uq.setParameter(e.getKey(), e.getValue()); }
                    List<?> urows = uq.list();
                    String LABEL_BELUM = "(Belum dipetakan ke Kelompok Laporan)";
                    for (Object ro : urows) {
                        Object[] rr = (Object[]) ro;
                        String kode = rr[0] == null ? "" : rr[0].toString();
                        String nama = rr[1] == null ? "" : rr[1].toString();
                        double saldoD = (rr[2] instanceof Number) ? ((Number) rr[2]).doubleValue() : 0.0;
                        if (saldoD >= 0) {
                            totalAktiva += saldoD;
                            if (!grpAktiva.containsKey(LABEL_BELUM)) { grpAktiva.put(LABEL_BELUM, new ArrayList<Object[]>()); }
                            grpAktiva.get(LABEL_BELUM).add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(saldoD) });
                        } else {
                            totalPasiva += -saldoD;
                            if (!grpPasiva.containsKey(LABEL_BELUM)) { grpPasiva.put(LABEL_BELUM, new ArrayList<Object[]>()); }
                            grpPasiva.get(LABEL_BELUM).add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(-saldoD) });
                        }
                    }
                    if (nrows.isEmpty() && urows.isEmpty() && labaBerjalan == 0.0) {
                        H.baris.add(new Object[]{"Belum ada data. Pastikan transaksi sudah DIPOSTING ke jurnal & akun dipetakan ke Kelompok Laporan jenis 'Neraca'.", null});
                        return H;
                    }
                    H.baris.add(new Object[]{"AKTIVA", null});
                    for (Map.Entry<String, List<Object[]>> e : grpAktiva.entrySet()) {
                        H.baris.add(new Object[]{"  " + e.getKey(), null});
                        double sub = 0.0;
                        for (Object[] x : e.getValue()) { H.baris.add(x); sub += ((Number) x[1]).doubleValue(); }
                        H.baris.add(new Object[]{"  Subtotal " + e.getKey(), Double.valueOf(sub)});
                    }
                    H.baris.add(new Object[]{"TOTAL AKTIVA", Double.valueOf(totalAktiva)});
                    H.baris.add(new Object[]{"KEWAJIBAN & MODAL", null});
                    for (Map.Entry<String, List<Object[]>> e : grpPasiva.entrySet()) {
                        H.baris.add(new Object[]{"  " + e.getKey(), null});
                        double sub = 0.0;
                        for (Object[] x : e.getValue()) { H.baris.add(x); sub += ((Number) x[1]).doubleValue(); }
                        H.baris.add(new Object[]{"  Subtotal " + e.getKey(), Double.valueOf(sub)});
                    }
                    H.baris.add(new Object[]{"  LABA (RUGI) BERJALAN", Double.valueOf(labaBerjalan)});
                    double totalPasivaPlus = totalPasiva + labaBerjalan;
                    H.baris.add(new Object[]{"TOTAL KEWAJIBAN & MODAL", Double.valueOf(totalPasivaPlus)});
                    H.baris.add(new Object[]{"SELISIH (harus 0)", Double.valueOf(totalAktiva - totalPasivaPlus)});
                } catch (Exception e) {
                    H.status = "99"; H.message = "Gagal menyusun Neraca berbasis jurnal: " + e.getMessage();
                }
                return H;

            } else if ("akn_arus_kas".equals(r)) {
                H.judul = "Arus Kas (Berbasis Jurnal Akuntansi)";
                H.catatan = "Mutasi akun Kas/Bank pada jurnal TERPOSTING: saldo awal (sebelum Tgl Mulai), penerimaan & "
                    + "pengeluaran periode yang diuraikan menurut AKUN LAWAN-nya, lalu saldo akhir (s/d Tgl Sampai). "
                    + "Pada jurnal yang punya banyak baris, nilai kas dialokasikan PROPORSIONAL ke tiap akun lawan, "
                    + "sehingga totalnya tetap sama dengan mutasi kas sesungguhnya.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};
                try {
                    Map<String,Object> pAwal = new LinkedHashMap<String,Object>();
                    String wAwal = klausaLedgerSebelum(session, tglMulai, pAwal);
                    double saldoAwal = angkaTunggal(session,
                        "select coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) " + FROM_LEDGER + wAwal + FILTER_KASBANK, pAwal);

                    Map<String,Object> pAkhir = new LinkedHashMap<String,Object>();
                    String wAkhir = klausaLedgerSampai(session, tglSampai, pAkhir);
                    double saldoAkhir = angkaTunggal(session,
                        "select coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) " + FROM_LEDGER + wAkhir + FILTER_KASBANK, pAkhir);

                    Map<String,Object> pp = new LinkedHashMap<String,Object>();
                    String wp = klausaLedger(session, tglMulai, tglSampai, pp);
                    String qm = "with kas as ( select a.grup_transaksi as gid, sum(coalesce(a.debet,0)) as masuk, "
                        + "     sum(coalesce(a.kredit,0)) as keluar " + FROM_LEDGER + wp + FILTER_KASBANK
                        + "   group by a.grup_transaksi ), "
                        + " lawan as ( select a.grup_transaksi as gid, d.kode as kode, d.nama as nama, "
                        + "     sum(coalesce(a.debet,0)) as ld, sum(coalesce(a.kredit,0)) as lk " + FROM_LEDGER + wp + FILTER_BUKAN_KASBANK
                        + "   group by a.grup_transaksi, d.kode, d.nama ), "
                        + " tot as ( select gid, sum(ld) as sld, sum(lk) as slk from lawan group by gid ) "
                        + " select l.kode, l.nama, "
                        + "   sum(case when coalesce(t.slk,0) > 0 then k.masuk * (l.lk / t.slk) else 0 end) as masuk, "
                        + "   sum(case when coalesce(t.sld,0) > 0 then k.keluar * (l.ld / t.sld) else 0 end) as keluar "
                        + " from kas k join lawan l on l.gid = k.gid join tot t on t.gid = k.gid "
                        + " group by l.kode, l.nama "
                        + " having sum(case when coalesce(t.slk,0) > 0 then k.masuk * (l.lk / t.slk) else 0 end) <> 0 "
                        + "     or sum(case when coalesce(t.sld,0) > 0 then k.keluar * (l.ld / t.sld) else 0 end) <> 0 "
                        + " order by l.kode ";
                    SQLQuery mq = session.createSQLQuery(qm);
                    for (Map.Entry<String,Object> e : pp.entrySet()) { mq.setParameter(e.getKey(), e.getValue()); }
                    List<?> mrows = mq.list();

                    List<Object[]> masukList = new ArrayList<Object[]>();
                    List<Object[]> keluarList = new ArrayList<Object[]>();
                    double totalMasuk = 0.0, totalKeluar = 0.0;
                    for (Object ro : mrows) {
                        Object[] rr = (Object[]) ro;
                        String kode = rr[0] == null ? "" : rr[0].toString();
                        String nama = rr[1] == null ? "" : rr[1].toString();
                        double masuk = (rr[2] instanceof Number) ? ((Number) rr[2]).doubleValue() : 0.0;
                        double keluar = (rr[3] instanceof Number) ? ((Number) rr[3]).doubleValue() : 0.0;
                        if (masuk != 0.0) { masukList.add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(masuk) }); totalMasuk += masuk; }
                        if (keluar != 0.0) { keluarList.add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(keluar) }); totalKeluar += keluar; }
                    }
                    if (mrows.isEmpty() && saldoAwal == 0.0 && saldoAkhir == 0.0) {
                        H.baris.add(new Object[]{"Belum ada mutasi Kas/Bank pada jurnal TERPOSTING untuk periode ini.", null});
                        return H;
                    }
                    H.baris.add(new Object[]{"SALDO AWAL KAS & BANK", Double.valueOf(saldoAwal)});
                    H.baris.add(new Object[]{"PENERIMAAN (menurut akun lawan)", null});
                    for (Object[] x : masukList) { H.baris.add(x); }
                    H.baris.add(new Object[]{"TOTAL PENERIMAAN", Double.valueOf(totalMasuk)});
                    H.baris.add(new Object[]{"PENGELUARAN (menurut akun lawan)", null});
                    for (Object[] x : keluarList) { H.baris.add(x); }
                    H.baris.add(new Object[]{"TOTAL PENGELUARAN", Double.valueOf(totalKeluar)});
                    H.baris.add(new Object[]{"KENAIKAN (PENURUNAN) KAS & BANK", Double.valueOf(totalMasuk - totalKeluar)});
                    H.baris.add(new Object[]{"SALDO AKHIR KAS & BANK", Double.valueOf(saldoAkhir)});
                    // Selisih hanya bermakna bila periodenya tertutup (Tgl Mulai & Tgl Sampai diisi).
                    if (ada(tglMulai) && ada(tglSampai)) {
                        H.baris.add(new Object[]{"SELISIH (harus 0)", Double.valueOf(saldoAwal + totalMasuk - totalKeluar - saldoAkhir)});
                    }
                    Map<String,Object> pr2 = new LinkedHashMap<String,Object>();
                    String wr2 = klausaLedgerSampai(session, tglSampai, pr2);
                    String qr2 = "select d.kode, d.nama, coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) "
                        + FROM_LEDGER + wr2 + FILTER_KASBANK
                        + " group by d.kode, d.nama "
                        + " having coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) <> 0 order by d.kode ";
                    SQLQuery rq = session.createSQLQuery(qr2);
                    for (Map.Entry<String,Object> e : pr2.entrySet()) { rq.setParameter(e.getKey(), e.getValue()); }
                    List<?> rrows = rq.list();
                    if (!rrows.isEmpty()) {
                        H.baris.add(new Object[]{"RINCIAN SALDO AKHIR PER AKUN KAS/BANK", null});
                        for (Object ro : rrows) {
                            Object[] rr = (Object[]) ro;
                            String kode = rr[0] == null ? "" : rr[0].toString();
                            String nama = rr[1] == null ? "" : rr[1].toString();
                            double sal = (rr[2] instanceof Number) ? ((Number) rr[2]).doubleValue() : 0.0;
                            H.baris.add(new Object[]{ "    " + kode + " " + nama, Double.valueOf(sal) });
                        }
                    }
                } catch (Exception e) {
                    H.status = "99"; H.message = "Gagal menyusun Arus Kas berbasis jurnal: " + e.getMessage();
                }
                return H;

            } else if ("akn_posisi_dana".equals(r)) {
                judul = "Posisi Dana (Saldo Kas & Bank per Rekening)";
                catatan = "Satu baris per akun Kas/Bank: SALDO AWAL (seluruh jurnal terposting SEBELUM Tgl Mulai), "
                    + "MUTASI Debet/Kredit dalam periode, dan SALDO AKHIR (= awal + debet - kredit). Padanan lembar "
                    + "'Posisi Saldo Bank' pada paket laporan keuangan yayasan. Jurnal TERPOSTING Satuan Kerja kantin.";
                String wPd = klausaLedgerSampai(session, tglSampai, prm);
                String sebelumPd = ada(tglMulai)
                    ? " cast(a.tanggal_transaksi as date) < cast(:tglMulaiPd as date) " : " false ";
                if (ada(tglMulai)) { prm.put("tglMulaiPd", tglMulai.trim()); }
                sql = "select d.kode as kode, d.nama as nama, "
                    + " coalesce(sum(case when " + sebelumPd + " then coalesce(a.debet,0) - coalesce(a.kredit,0) "
                    + "     else 0 end),0) as saldo_awal, "
                    + " coalesce(sum(case when " + sebelumPd + " then 0 else coalesce(a.debet,0) end),0) as mutasi_debet, "
                    + " coalesce(sum(case when " + sebelumPd + " then 0 else coalesce(a.kredit,0) end),0) as mutasi_kredit, "
                    + " coalesce(sum(coalesce(a.debet,0) - coalesce(a.kredit,0)),0) as saldo_akhir "
                    + FROM_LEDGER + wPd + FILTER_KASBANK
                    + " group by d.id, d.kode, d.nama "
                    + " having coalesce(sum(coalesce(a.debet,0)),0) <> 0 or coalesce(sum(coalesce(a.kredit,0)),0) <> 0 "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Akun Kas/Bank","text"));
                kolom.add(new Kolom("Saldo Awal","num")); kolom.add(new Kolom("Mutasi Debet","num"));
                kolom.add(new Kolom("Mutasi Kredit","num")); kolom.add(new Kolom("Saldo Akhir","num"));

            } else if ("akn_neraca_percobaan".equals(r)) {
                judul = "Neraca Percobaan Lengkap (Saldo Awal, Mutasi, Saldo Akhir)";
                catatan = "Format kertas kerja yayasan: SALDO AWAL Debet/Kredit (sebelum Tgl Mulai), MUTASI periode "
                    + "yang dipisah MUTASI KAS (jurnal yang menyentuh akun Kas/Bank) dan MUTASI NON KAS (jurnal "
                    + "penyesuaian/memorial), lalu SALDO AKHIR Debet/Kredit. Total tiap pasang kolom harus seimbang. "
                    + "Jurnal TERPOSTING Satuan Kerja kantin.";
                String wNp = klausaLedgerSampai(session, tglSampai, prm);
                String sebelumNp = ada(tglMulai)
                    ? " cast(a.tanggal_transaksi as date) < cast(:tglMulaiNp as date) " : " false ";
                if (ada(tglMulai)) { prm.put("tglMulaiNp", tglMulai.trim()); }
                // Jurnal "kas" = grup transaksinya memuat minimal satu baris berakun Kas/Bank.
                String grupKas = " exists ( select 1 from akunting.transaksi a2 "
                    + "     join akunting.akun d2 on d2.id = a2.akun "
                    + "     where a2.grup_transaksi = a.grup_transaksi "
                    + "       and ( d2.bank_id is not null or d2.norek is not null "
                    + "             or lower(coalesce(d2.nama,'')) like '%kas%' "
                    + "             or lower(coalesce(d2.nama,'')) like '%bank%' ) ) ";
                String sAwal = "coalesce(sum(case when " + sebelumNp
                    + " then coalesce(a.debet,0) - coalesce(a.kredit,0) else 0 end),0)";
                String sAkhir = "coalesce(sum(coalesce(a.debet,0) - coalesce(a.kredit,0)),0)";
                sql = "select d.kode as kode, d.nama as nama, "
                    + " case when " + sAwal + " > 0 then " + sAwal + " else 0 end as awal_debet, "
                    + " case when " + sAwal + " < 0 then -" + sAwal + " else 0 end as awal_kredit, "
                    + " coalesce(sum(case when not (" + sebelumNp + ") and " + grupKas
                    + "     then coalesce(a.debet,0) else 0 end),0) as kas_debet, "
                    + " coalesce(sum(case when not (" + sebelumNp + ") and " + grupKas
                    + "     then coalesce(a.kredit,0) else 0 end),0) as kas_kredit, "
                    + " coalesce(sum(case when not (" + sebelumNp + ") and not (" + grupKas
                    + "     ) then coalesce(a.debet,0) else 0 end),0) as nonkas_debet, "
                    + " coalesce(sum(case when not (" + sebelumNp + ") and not (" + grupKas
                    + "     ) then coalesce(a.kredit,0) else 0 end),0) as nonkas_kredit, "
                    + " case when " + sAkhir + " > 0 then " + sAkhir + " else 0 end as akhir_debet, "
                    + " case when " + sAkhir + " < 0 then -" + sAkhir + " else 0 end as akhir_kredit "
                    + FROM_LEDGER + wNp
                    + " group by d.id, d.kode, d.nama "
                    + " having coalesce(sum(coalesce(a.debet,0)),0) <> 0 or coalesce(sum(coalesce(a.kredit,0)),0) <> 0 "
                    + " order by d.kode ";
                tipe = new String[]{"text","text","num","num","num","num","num","num","num","num"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Saldo Awal Debet","num")); kolom.add(new Kolom("Saldo Awal Kredit","num"));
                kolom.add(new Kolom("Mutasi Kas Debet","num")); kolom.add(new Kolom("Mutasi Kas Kredit","num"));
                kolom.add(new Kolom("Mutasi Non Kas Debet","num")); kolom.add(new Kolom("Mutasi Non Kas Kredit","num"));
                kolom.add(new Kolom("Saldo Akhir Debet","num")); kolom.add(new Kolom("Saldo Akhir Kredit","num"));

            } else if ("akn_arus_kas_aktivitas".equals(r)) {
                H.judul = "Arus Kas per Aktivitas (Operasional / Investasi / Pendanaan)";
                H.catatan = "Mutasi Kas/Bank jurnal TERPOSTING diuraikan menurut AKUN LAWAN, lalu dikelompokkan ke "
                    + "AKTIVITAS memakai Kelompok Laporan jenis 'Arus Kas' (Akuntansi > Setup Laporan); bila akun belum "
                    + "dipetakan di sana dipakai kolom 'Aktifitas (Arus Kas)' pada master Kode Akun. Nilai kas dialokasikan "
                    + "PROPORSIONAL ke tiap akun lawan sehingga totalnya sama dengan mutasi kas sesungguhnya. Padanan "
                    + "lembar 'Laporan Penerimaan dan Pengeluaran Cash' pada paket laporan keuangan yayasan.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Penerimaan","num"));
                H.kolom.add(new Kolom("Pengeluaran","num")); H.kolom.add(new Kolom("Bersih","num"));
                H.tipe = new String[]{"text","num","num","num"};
                try {
                    Map<String,Object> pAwalAk = new LinkedHashMap<String,Object>();
                    String wAwalAk = klausaLedgerSebelum(session, tglMulai, pAwalAk);
                    double saldoAwalAk = angkaTunggal(session,
                        "select coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) "
                        + FROM_LEDGER + wAwalAk + FILTER_KASBANK, pAwalAk);

                    Map<String,Object> pAkhirAk = new LinkedHashMap<String,Object>();
                    String wAkhirAk = klausaLedgerSampai(session, tglSampai, pAkhirAk);
                    double saldoAkhirAk = angkaTunggal(session,
                        "select coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) "
                        + FROM_LEDGER + wAkhirAk + FILTER_KASBANK, pAkhirAk);

                    // Seksi aktivitas akun lawan: Kelompok Laporan jenis "Arus Kas" -> master_grup_laporan.nama;
                    // cadangan: kolom akun.aktifitas; bila dua-duanya kosong dikumpulkan ke keranjang "belum
                    // dipetakan" supaya kekurangan setup TERLIHAT, bukan diam-diam masuk Operasional.
                    String seksiAk = " coalesce( ( select max(m3.nama) "
                        + "     from akunting.kelompok_laporan_punya_akun b3 "
                        + "     join akunting.kelompok_laporan c3 on c3.id = b3.kelompok_laporan "
                        + "     join akunting.jenis_laporan f3 on f3.id = c3.jenis_laporan "
                        + "     left join akunting.master_grup_laporan m3 on m3.id = c3.master_grup_laporan "
                        + "     where b3.akun = d.id and (c3.aktif is null or c3.aktif) "
                        + "       and lower(coalesce(f3.keterangan,'')) like '%arus%' ), "
                        + "   nullif(trim(coalesce(d.aktifitas,'')),''), "
                        + "   '(Belum dipetakan ke Aktivitas Arus Kas)' ) ";

                    Map<String,Object> ppAk = new LinkedHashMap<String,Object>();
                    String wpAk = klausaLedger(session, tglMulai, tglSampai, ppAk);
                    String qAk = "with kas as ( select a.grup_transaksi as gid, "
                        + "     sum(coalesce(a.debet,0)) as masuk, sum(coalesce(a.kredit,0)) as keluar "
                        + FROM_LEDGER + wpAk + FILTER_KASBANK + " group by a.grup_transaksi ), "
                        + " lawan as ( select a.grup_transaksi as gid, " + seksiAk + " as seksi, "
                        + "     (d.kode || ' ' || d.nama) as label, "
                        + "     sum(coalesce(a.debet,0)) as ld, sum(coalesce(a.kredit,0)) as lk "
                        + FROM_LEDGER + wpAk + FILTER_BUKAN_KASBANK
                        + "   group by a.grup_transaksi, " + seksiAk + ", d.kode, d.nama ), "
                        + " tot as ( select gid, sum(ld) as sld, sum(lk) as slk from lawan group by gid ) "
                        + " select l.seksi as seksi, l.label as label, "
                        + "   sum(case when coalesce(t.slk,0) > 0 then k.masuk * (l.lk / t.slk) else 0 end) as masuk, "
                        + "   sum(case when coalesce(t.sld,0) > 0 then k.keluar * (l.ld / t.sld) else 0 end) as keluar "
                        + " from kas k join lawan l on l.gid = k.gid join tot t on t.gid = k.gid "
                        + " group by l.seksi, l.label "
                        + " having sum(case when coalesce(t.slk,0) > 0 then k.masuk * (l.lk / t.slk) else 0 end) <> 0 "
                        + "     or sum(case when coalesce(t.sld,0) > 0 then k.keluar * (l.ld / t.sld) else 0 end) <> 0 "
                        + " order by " + URUT_SEKSI_ARUS + ", l.seksi, l.label ";
                    SQLQuery qq = session.createSQLQuery(qAk);
                    for (Map.Entry<String,Object> e : ppAk.entrySet()) { qq.setParameter(e.getKey(), e.getValue()); }
                    List<?> rowsAk = qq.list();

                    if (rowsAk.isEmpty() && saldoAwalAk == 0.0 && saldoAkhirAk == 0.0) {
                        H.baris.add(new Object[]{"Belum ada mutasi Kas/Bank pada jurnal TERPOSTING untuk periode ini.",
                            null, null, null});
                        return H;
                    }

                    H.baris.add(new Object[]{"SALDO AWAL KAS & BANK", null, null, Double.valueOf(saldoAwalAk)});
                    String seksiKini = null;
                    double subMasuk = 0.0, subKeluar = 0.0, totMasuk = 0.0, totKeluar = 0.0;
                    for (Object ro : rowsAk) {
                        Object[] rr = (Object[]) ro;
                        String seksi = rr[0] == null ? "(Tanpa Aktivitas)" : rr[0].toString();
                        String label = rr[1] == null ? "" : rr[1].toString();
                        double masuk = (rr[2] instanceof Number) ? ((Number) rr[2]).doubleValue() : 0.0;
                        double keluar = (rr[3] instanceof Number) ? ((Number) rr[3]).doubleValue() : 0.0;
                        if (!seksi.equals(seksiKini)) {
                            if (seksiKini != null) {
                                H.baris.add(new Object[]{"    Arus Kas Bersih - " + seksiKini,
                                    Double.valueOf(subMasuk), Double.valueOf(subKeluar),
                                    Double.valueOf(subMasuk - subKeluar)});
                            }
                            seksiKini = seksi; subMasuk = 0.0; subKeluar = 0.0;
                            H.baris.add(new Object[]{seksi.toUpperCase(), null, null, null});
                        }
                        H.baris.add(new Object[]{"    " + label, Double.valueOf(masuk), Double.valueOf(keluar),
                            Double.valueOf(masuk - keluar)});
                        subMasuk += masuk; subKeluar += keluar; totMasuk += masuk; totKeluar += keluar;
                    }
                    if (seksiKini != null) {
                        H.baris.add(new Object[]{"    Arus Kas Bersih - " + seksiKini,
                            Double.valueOf(subMasuk), Double.valueOf(subKeluar),
                            Double.valueOf(subMasuk - subKeluar)});
                    }
                    H.baris.add(new Object[]{"KENAIKAN (PENURUNAN) KAS & BANK", Double.valueOf(totMasuk),
                        Double.valueOf(totKeluar), Double.valueOf(totMasuk - totKeluar)});
                    H.baris.add(new Object[]{"SALDO AKHIR KAS & BANK", null, null, Double.valueOf(saldoAkhirAk)});
                    if (ada(tglMulai) && ada(tglSampai)) {
                        H.baris.add(new Object[]{"SELISIH (harus 0)", null, null,
                            Double.valueOf(saldoAwalAk + totMasuk - totKeluar - saldoAkhirAk)});
                    }
                } catch (Exception e) {
                    H.status = "99"; H.message = "Gagal menyusun Arus Kas per Aktivitas: " + e.getMessage();
                }
                return H;

            } else if ("akn_buku_kas_umum".equals(r)) {
                judul = "Buku Kas Umum (Mutasi Kas & Bank)"; grupIdx = 0;
                catatan = "Buku kas per rekening: tiap mutasi Kas/Bank berurut tanggal dengan AKUN LAWAN-nya "
                    + "dan SALDO BERJALAN. Saldo berjalan sudah memperhitungkan saldo sebelum Tgl Mulai, "
                    + "sehingga baris terakhir tiap rekening sama dengan saldo akhir pada Posisi Dana. "
                    + "Padanan lembar 'Mutasi Kas & Bank' pada paket laporan keuangan yayasan. "
                    + "Jurnal TERPOSTING Satuan Kerja terpilih.";
                String wBku = klausaLedger(session, tglMulai, tglSampai, prm);
                String awalBku = "0";
                if (ada(tglMulai)) {
                    prm.put("tglMulaiBku", tglMulai.trim());
                    awalBku = "( select coalesce(sum(x.debet),0) - coalesce(sum(x.kredit),0) "
                        + "   from akunting.transaksi x "
                        + "   join akunting.grup_transaksi x1 on x1.id = x.grup_transaksi "
                        + "   where x.akun = a.akun and x1.posting_history is not null "
                        + "     and ( :satker = -1 or x1.satuan_kerja = :satker ) "
                        + "     and cast(x.tanggal_transaksi as date) < cast(:tglMulaiBku as date) )";
                }
                sql = "select (d.kode || ' - ' || d.nama) as akun, "
                    + " cast(a.tanggal_transaksi as date) as tgl, coalesce(a.kode,'-') as nojurnal, "
                    + " coalesce(a.keterangan,'') as uraian, "
                    + " coalesce( ( select string_agg(distinct (d2.kode || ' ' || d2.nama), ', ') "
                    + "     from akunting.transaksi a2 join akunting.akun d2 on d2.id = a2.akun "
                    + "     where a2.grup_transaksi = a.grup_transaksi and a2.akun <> a.akun ), '-' ) as lawan, "
                    + " coalesce(a.debet,0) as masuk, coalesce(a.kredit,0) as keluar, "
                    + " ( " + awalBku + " + sum(coalesce(a.debet,0) - coalesce(a.kredit,0)) "
                    + "     over (partition by d.id order by a.tanggal_transaksi, a.id "
                    + "           rows between unbounded preceding and current row) ) as saldo "
                    + FROM_LEDGER + wBku + FILTER_KASBANK
                    + " order by d.kode, a.tanggal_transaksi, a.id ";
                tipe = new String[]{"text","tgl","text","text","text","num","num","num"};
                kolom.add(new Kolom("Akun Kas/Bank","text")); kolom.add(new Kolom("Tanggal","tgl"));
                kolom.add(new Kolom("No. Jurnal","text")); kolom.add(new Kolom("Uraian","text"));
                kolom.add(new Kolom("Akun Lawan","text")); kolom.add(new Kolom("Penerimaan","num"));
                kolom.add(new Kolom("Pengeluaran","num")); kolom.add(new Kolom("Saldo Berjalan","num"));

            } else if ("akn_diagnosa_aktivitas".equals(r)) {
                judul = "Diagnosa Pemetaan Aktivitas Arus Kas";
                catatan = "Akun LAWAN yang benar-benar menggerakkan Kas/Bank pada jurnal TERPOSTING tetapi BELUM "
                    + "punya aktivitas arus kas -- baik lewat Kelompok Laporan jenis 'Arus Kas' maupun kolom "
                    + "'Aktifitas (Arus Kas)' di master Kode Akun. Selama akun ini belum dipetakan, nilainya "
                    + "menumpuk di keranjang '(Belum dipetakan ke Aktivitas Arus Kas)' pada laporan Arus Kas "
                    + "per Aktivitas. Urut dari penyumbang terbesar supaya yang paling berpengaruh dibereskan dulu.";
                String wDa = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode as kode, d.nama as nama, "
                    + " coalesce(sum(coalesce(a.debet,0) + coalesce(a.kredit,0)),0) as nilai, "
                    + " case when exists ( select 1 from akunting.kelompok_laporan_punya_akun b4 "
                    + "         join akunting.kelompok_laporan c4 on c4.id = b4.kelompok_laporan "
                    + "         join akunting.jenis_laporan f4 on f4.id = c4.jenis_laporan "
                    + "         where b4.akun = d.id and lower(coalesce(f4.keterangan,'')) like '%arus%' ) "
                    + "      then 'Terpetakan, tetapi kelompoknya non-aktif' "
                    + "      else 'BELUM dipetakan (Kelompok Laporan maupun kolom Aktifitas)' end as status "
                    + FROM_LEDGER + wDa + FILTER_BUKAN_KASBANK
                    + " and exists ( select 1 from akunting.transaksi a3 "
                    + "     join akunting.akun d3 on d3.id = a3.akun "
                    + "     where a3.grup_transaksi = a.grup_transaksi "
                    + "       and ( d3.bank_id is not null or d3.norek is not null "
                    + "             or lower(coalesce(d3.nama,'')) like '%kas%' "
                    + "             or lower(coalesce(d3.nama,'')) like '%bank%' ) ) "
                    + " and nullif(trim(coalesce(d.aktifitas,'')),'') is null "
                    + " and not exists ( select 1 from akunting.kelompok_laporan_punya_akun b5 "
                    + "     join akunting.kelompok_laporan c5 on c5.id = b5.kelompok_laporan "
                    + "     join akunting.jenis_laporan f5 on f5.id = c5.jenis_laporan "
                    + "     where b5.akun = d.id and (c5.aktif is null or c5.aktif) "
                    + "       and lower(coalesce(f5.keterangan,'')) like '%arus%' ) "
                    + " group by d.id, d.kode, d.nama "
                    + " order by coalesce(sum(coalesce(a.debet,0) + coalesce(a.kredit,0)),0) desc, d.kode ";
                tipe = new String[]{"text","text","num","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Nilai Mutasi Terkait Kas","num")); kolom.add(new Kolom("Status","text"));

            } else if ("akn_diagnosa_akun".equals(r)) {
                judul = "Diagnosa Pemetaan Akun (Jurnal vs Kelompok Laporan)";
                catatan = "Akun yang DIPAKAI jurnal TERPOSTING tetapi BELUM terpetakan ke Kelompok Laporan aktif, sehingga "
                    + "nilainya TIDAK ikut muncul di Laba Rugi / Neraca berbasis jurnal. Petakan lewat menu Akuntansi > "
                    + "Kelompok Laporan agar laporan resmi lengkap.";
                String w = klausaLedger(session, tglMulai, tglSampai, prm);
                sql = "select d.kode, d.nama, coalesce(sum(a.debet),0), coalesce(sum(a.kredit),0), "
                    + " coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0), "
                    + " case when exists ( select 1 from akunting.kelompok_laporan_punya_akun b2 where b2.akun = d.id ) "
                    + "      then 'Terpetakan, tetapi kelompoknya non-aktif' else 'BELUM dipetakan' end "
                    + FROM_LEDGER + w
                    + " and not exists ( select 1 from akunting.kelompok_laporan_punya_akun b2 "
                    + "     join akunting.kelompok_laporan c2 on c2.id = b2.kelompok_laporan "
                    + "     where b2.akun = d.id and (c2.aktif is null or c2.aktif) ) "
                    + " group by d.id, d.kode, d.nama "
                    + " order by abs(coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0)) desc, d.kode ";
                tipe = new String[]{"text","text","num","num","num","text"};
                kolom.add(new Kolom("Kode","text")); kolom.add(new Kolom("Nama Akun","text"));
                kolom.add(new Kolom("Debet","num")); kolom.add(new Kolom("Kredit","num"));
                kolom.add(new Kolom("Saldo (D-K)","num")); kolom.add(new Kolom("Status","text"));

            } else if ("fin_laba_rugi_rincian_hpp".equals(r)) {
                // DRILL-DOWN "HPP (Modal Barang Terjual)" -- dibuka saat pengguna mengklik angka
                // HPP di Laporan Laba Rugi. Memakai FILTER & RUMUS PERSIS SAMA dgn baris HPP di
                // fin_laba_rugi (qty terjual x hargabeli produk), hanya dipecah per produk dan
                // diurutkan dari penyumbang terbesar -- sehingga salah input harga modal per item
                // langsung terlihat di baris teratas (keluhan pengguna 2026-08-19: HPP minus miliaran).
                H.judul = "Rincian HPP per Produk";
                H.catatan = "Penyusun angka HPP: qty terjual x harga modal tiap produk. "
                        + "Kolom Catatan menandai produk yang harga modalnya melebihi harga jual -- "
                        + "biasanya itu salah input dan menjadi sebab HPP membengkak.";
                H.grup = -1; H.grandTotal = true;
                H.kolom.add(new Kolom("Kode","text")); H.kolom.add(new Kolom("Produk","text"));
                H.kolom.add(new Kolom("Toko","text")); H.kolom.add(new Kolom("Qty Terjual","num"));
                H.kolom.add(new Kolom("Harga Modal","num")); H.kolom.add(new Kolom("Harga Jual","num"));
                H.kolom.add(new Kolom("Subtotal HPP","num")); H.kolom.add(new Kolom("Catatan","text"));
                H.tipe = new String[]{"text","text","text","num","num","num","num","text"};
                Map<String,Object> pr1 = new LinkedHashMap<String,Object>();
                String wr1 = " where 1=1 " + kondToko("p.toko", tokoId, pr1)
                        + klausaTanggal("p.waktu", tglMulai, tglSampai, pr1);
                String sqlHpp = "select coalesce(pr.kode,''), pr.nama, coalesce(t.nama,'-'),"
                        + " sum(coalesce(p.qty,0)), coalesce(pr.hargabeli,0), coalesce(pr.hargajual,0),"
                        + " sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0)),"
                        + " case when coalesce(pr.hargabeli,0) > coalesce(pr.hargajual,0)"
                        + "      then 'PERIKSA: harga modal > harga jual' else '' end"
                        + " from koperasi.pembelian p"
                        + " join koperasi.produk pr on pr.id = p.produk"
                        + " left join koperasi.toko t on t.id = p.toko" + wr1
                        + " group by pr.id, pr.kode, pr.nama, t.nama, pr.hargabeli, pr.hargajual"
                        + " order by 7 desc";
                isiBarisDariSql(session, H, sqlHpp, pr1, 8);
                return H;
            } else if ("fin_laba_rugi_rincian_penjualan".equals(r)) {
                // DRILL-DOWN "Penjualan" -- rincian nota penyusun angka pendapatan.
                H.judul = "Rincian Penjualan per Nota";
                H.catatan = "Penyusun angka Penjualan: total tiap nota pada rentang tanggal yang sama.";
                H.grup = -1; H.grandTotal = true;
                H.kolom.add(new Kolom("Waktu","tgl")); H.kolom.add(new Kolom("Kode Nota","text"));
                H.kolom.add(new Kolom("Toko","text")); H.kolom.add(new Kolom("Kasir","text"));
                H.kolom.add(new Kolom("Total Nota","num"));
                H.tipe = new String[]{"tgl","text","text","text","num"};
                Map<String,Object> pr2 = new LinkedHashMap<String,Object>();
                String wr2 = " where 1=1 " + kondToko("h.toko", tokoId, pr2)
                        + klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, pr2);
                String sqlJual = "select h.tanggal_pembayaran, coalesce(h.kode,''), coalesce(t.nama,'-'),"
                        + " coalesce(h.kasir_login_nama, h.oleh, '-'), coalesce(h.total_biaya,0)"
                        + " from koperasi.pembelian_anggota_koperasi h"
                        + " left join koperasi.toko t on t.id = h.toko" + wr2
                        + " order by h.tanggal_pembayaran desc";
                isiBarisDariSql(session, H, sqlJual, pr2, 5);
                return H;
            } else if ("fin_laba_rugi".equals(r)) {
                H.judul = "Laporan Laba Rugi";
                H.catatan = "Laporan sederhana berbasis transaksi kantin (bukan jurnal akuntansi penuh). HPP = qty terjual x harga modal.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};

                Map<String,Object> p1 = new LinkedHashMap<String,Object>();
                String w1 = " where 1=1 " + kondToko("h.toko", tokoId, p1) + klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, p1);
                double revenue = execScalar(session, "select coalesce(sum(coalesce(h.total_biaya,0)),0) from koperasi.pembelian_anggota_koperasi h " + w1, p1);
                Map<String,Object> p2 = new LinkedHashMap<String,Object>();
                String w2 = " where 1=1 " + kondToko("p.toko", tokoId, p2) + klausaTanggal("p.waktu", tglMulai, tglSampai, p2);
                double hpp = execScalar(session, "select coalesce(sum(coalesce(p.qty,0)*coalesce(pr.hargabeli,0)),0) from koperasi.pembelian p left join koperasi.produk pr on pr.id=p.produk " + w2, p2);
                double retur = 0.0;
                if (tabelAda(session, "koperasi.retur_barang")) {
                    Map<String,Object> p3 = new LinkedHashMap<String,Object>();
                    String w3 = " where 1=1 " + kondToko("x.toko", tokoId, p3) + klausaTanggal("x.waktu", tglMulai, tglSampai, p3);
                    retur = execScalar(session, "select coalesce(sum(coalesce(x.total, x.qty*x.hargasatuan,0)),0) from koperasi.retur_barang x " + w3, p3);
                }
                double pakai = 0.0;
                if (tabelAda(session, "koperasi.pemakaian_bahan_baku")) {
                    Map<String,Object> p4 = new LinkedHashMap<String,Object>();
                    String w4 = " where 1=1 " + kondToko("x.toko", tokoId, p4) + klausaTanggal("x.waktu", tglMulai, tglSampai, p4);
                    pakai = execScalar(session, "select coalesce(sum(coalesce(x.qty,0)*coalesce(pr.hargabeli,0)),0) from koperasi.pemakaian_bahan_baku x left join koperasi.produk pr on pr.id=x.produk " + w4, p4);
                }
                double pendapatanBersih = revenue - retur;
                double labaKotor = pendapatanBersih - hpp;
                double labaBersih = labaKotor - pakai;
                H.baris.add(new Object[]{"PENDAPATAN", null});
                H.baris.add(new Object[]{"    Penjualan", Double.valueOf(revenue)});
                if (retur != 0.0) { H.baris.add(new Object[]{"    Retur Barang", Double.valueOf(-retur)}); }
                H.baris.add(new Object[]{"    Pendapatan Bersih", Double.valueOf(pendapatanBersih)});
                H.baris.add(new Object[]{"HARGA POKOK PENJUALAN", null});
                H.baris.add(new Object[]{"    HPP (Modal Barang Terjual)", Double.valueOf(hpp)});
                H.baris.add(new Object[]{"LABA KOTOR", Double.valueOf(labaKotor)});
                if (pakai != 0.0) {
                    H.baris.add(new Object[]{"BEBAN OPERASIONAL", null});
                    H.baris.add(new Object[]{"    Pemakaian Bahan Baku", Double.valueOf(pakai)});
                }
                H.baris.add(new Object[]{"LABA (RUGI) BERSIH", Double.valueOf(labaBersih)});
                return H;

            } else if ("fin_neraca".equals(r)) {
                H.judul = "Neraca (Posisi Keuangan)";
                H.catatan = "Sederhana, per Tgl Sampai. Kas = penerimaan penjualan - pengeluaran pengadaan (kumulatif). Persediaan = stok x harga modal (posisi terkini). Modal = penyeimbang.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};

                Map<String,Object> pa = new LinkedHashMap<String,Object>();
                String wa = " where 1=1 " + kondToko("h.toko", tokoId, pa) + klausaSampai("h.tanggal_pembayaran", tglSampai, pa);
                double kasIn = execScalar(session, "select coalesce(sum(coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)),0) from koperasi.pembelian_anggota_koperasi h " + wa, pa);
                Map<String,Object> pb = new LinkedHashMap<String,Object>();
                String wb = " where 1=1 " + kondToko("a.toko", tokoId, pb) + klausaSampai("a.waktupengadaan", tglSampai, pb);
                double kasOut = execScalar(session, "select coalesce(sum(coalesce(a.totalharga, a.qty*a.hargabelisatuan,0)),0) from koperasi.pengadaan_produk a " + wb, pb);
                double kas = kasIn - kasOut;
                Map<String,Object> pc = new LinkedHashMap<String,Object>();
                String wc = " where pr.aktif=true " + kondToko("pr.toko", tokoId, pc);
                double persediaan = execScalar(session, "select coalesce(sum(coalesce(pr.stok,0)*coalesce(pr.hargabeli,0)),0) from koperasi.produk pr " + wc, pc);
                Map<String,Object> pd = new LinkedHashMap<String,Object>();
                String wd = " where u.nilai_piutang > 0 " + kondToko("u.toko", tokoId, pd) + klausaSampai("u.tanggal", tglSampai, pd);
                double piutang = execScalar(session, "select coalesce(sum(u.nilai_piutang),0) from (" + sqlSumberKasbon() + ") u " + wd, pd);
                double totalAktiva = kas + persediaan + piutang;
                double utang = 0.0;
                double modal = totalAktiva - utang;
                H.baris.add(new Object[]{"AKTIVA", null});
                H.baris.add(new Object[]{"    Kas & Setara Kas", Double.valueOf(kas)});
                H.baris.add(new Object[]{"    Persediaan Barang", Double.valueOf(persediaan)});
                H.baris.add(new Object[]{"    Piutang Usaha", Double.valueOf(piutang)});
                H.baris.add(new Object[]{"TOTAL AKTIVA", Double.valueOf(totalAktiva)});
                H.baris.add(new Object[]{"KEWAJIBAN & EKUITAS", null});
                H.baris.add(new Object[]{"    Utang Usaha", Double.valueOf(utang)});
                H.baris.add(new Object[]{"    Modal / Ekuitas", Double.valueOf(modal)});
                H.baris.add(new Object[]{"TOTAL KEWAJIBAN & EKUITAS", Double.valueOf(totalAktiva)});
                return H;

            } else if ("fin_penerimaan_per_metode".equals(r)) {
                // Penerimaan penjualan per akun kas/bank (metode bayar) per hari.
                //
                // Nota SPLIT dipecah dulu: satu nota yang dibayar mis. Rp5.000 QRIS BSI +
                // Rp4.000 Tunai menyumbang ke DUA metode, bukan seluruhnya ke satu metode.
                // Slot 1 nominalnya IMPLISIT (total_biaya dikurangi slot 2..5 -- lihat
                // JavaDoc PembelianAnggotaKoperasi), slot 2..5 memakai nominal_bayar_N.
                // Kolom "Jumlah Transaksi" memakai COUNT DISTINCT id nota supaya nota
                // split tidak terhitung ganda di dalam satu metode.
                judul = "Penerimaan per Kas/Bank (Harian)"; grupIdx = 0;
                catatan = "Nota dengan pembayaran split dipecah ke tiap metode sesuai nominalnya, "
                        + "sehingga jumlah seluruh metode sama dengan total penerimaan.";
                StringBuilder w = new StringBuilder(" where 1=1 ");
                if (tokoId != null) { w.append(" and h.toko = :tokoId "); prm.put("tokoId", tokoId); }
                w.append(klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, prm));
                String kondisi = w.toString();
                StringBuilder u = new StringBuilder();
                u.append("select h.id as id_nota, cast(h.tanggal_pembayaran as date) as tanggal,")
                 .append(" coalesce(nullif(trim(cb.nama),''),'-') as metode,")
                 .append(" (coalesce(h.total_biaya,0)-coalesce(h.nominal_bayar_2,0)-coalesce(h.nominal_bayar_3,0)")
                 .append("  -coalesce(h.nominal_bayar_4,0)-coalesce(h.nominal_bayar_5,0)) as nilai")
                 .append(" from koperasi.pembelian_anggota_koperasi h")
				 .append(" left join koperasi.cara_pembayaran_koperasi cb on cast(cb.id as text) = nullif(trim(cast(h.cara_pembayaran_koperasi as text)),'')")
                 .append(kondisi);
                for (int slot = 2; slot <= 5; slot++) {
                    u.append(" union all select h.id, cast(h.tanggal_pembayaran as date),")
                     .append(" coalesce(nullif(trim(cb.nama),''),'-'), coalesce(h.nominal_bayar_").append(slot).append(",0)")
                     .append(" from koperasi.pembelian_anggota_koperasi h")
					 .append(" left join koperasi.cara_pembayaran_koperasi cb on cast(cb.id as text) = nullif(trim(cast(h.cara_pembayaran_koperasi_").append(slot).append(" as text)),'')")
                     .append(kondisi)
                     .append(" and coalesce(h.nominal_bayar_").append(slot).append(",0) <> 0 ");
                }
                sql = "select s.metode, s.tanggal, count(distinct s.id_nota), coalesce(sum(s.nilai),0)"
                    + " from (" + u + ") s where s.nilai <> 0"
                    + " group by s.metode, s.tanggal order by s.metode asc, s.tanggal asc";
                tipe = new String[]{"text","tgl","num","num"};
                kolom.add(new Kolom("Nama Kas/Bank","text"));
                kolom.add(new Kolom("Tanggal","tgl"));
                kolom.add(new Kolom("Total Transaksi Penerimaan","num"));
                kolom.add(new Kolom("Total Penerimaan","num"));

            } else if ("fin_arus_kas".equals(r)) {
                H.judul = "Laporan Arus Kas";
                H.catatan = "Arus kas operasional sederhana: penerimaan penjualan vs pengeluaran pengadaan pada periode.";
                H.grup = -1; H.grandTotal = false;
                H.kolom.add(new Kolom("Keterangan","text")); H.kolom.add(new Kolom("Nilai","num"));
                H.tipe = new String[]{"text","num"};

                Map<String,Object> q1 = new LinkedHashMap<String,Object>();
                String wq1 = " where 1=1 " + kondToko("h.toko", tokoId, q1) + klausaTanggal("h.tanggal_pembayaran", tglMulai, tglSampai, q1);
                double masuk = execScalar(session, "select coalesce(sum(coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)),0) from koperasi.pembelian_anggota_koperasi h " + wq1, q1);
                Map<String,Object> q2 = new LinkedHashMap<String,Object>();
                String wq2 = " where 1=1 " + kondToko("a.toko", tokoId, q2) + klausaTanggal("a.waktupengadaan", tglMulai, tglSampai, q2);
                double keluar = execScalar(session, "select coalesce(sum(coalesce(a.totalharga, a.qty*a.hargabelisatuan,0)),0) from koperasi.pengadaan_produk a " + wq2, q2);
                double net = masuk - keluar;
                H.baris.add(new Object[]{"ARUS KAS OPERASIONAL", null});
                H.baris.add(new Object[]{"    Penerimaan dari Penjualan", Double.valueOf(masuk)});
                H.baris.add(new Object[]{"    Pengeluaran untuk Pengadaan", Double.valueOf(-keluar)});
                H.baris.add(new Object[]{"ARUS KAS BERSIH", Double.valueOf(net)});
                return H;

            } else {
                H.status = "soon"; H.message = "Laporan ini sedang disiapkan dan akan tersedia pada pembaruan berikutnya."; return H;
            }

            // ==== "Per toko / total": bila diminta & branch mendukung (tokoIdCol di-set), tambahkan kolom
            //      "Toko" (nama via subquery id->nama) di AKHIR + kelompokkan per toko. Kolom di AKHIR
            //      agar TIDAK menggeser posisi kolom / referensi positional (order by 3, dst.). ====
            if (perToko && tokoIdCol != null && sql != null) {
                sql = perTokoSql(sql, tokoIdCol);
                String[] t2 = new String[tipe.length + 1];
                System.arraycopy(tipe, 0, t2, 0, tipe.length);
                t2[tipe.length] = "text";
                tipe = t2;
                kolom.add(new Kolom("Toko", "text"));
                grupIdx = kolom.size() - 1; // kelompokkan berdasar kolom Toko (kolom terakhir)
                catatan = (catatan == null || catatan.trim().length() == 0) ? "Dikelompokkan per toko." : (catatan + " Dikelompokkan per toko.");
            }

            // Eksekusi via JDBC MENTAH (session.doWork) + baca ResultSet BY-POSITION memakai tipe[]
            // kita sendiri. Sengaja TIDAK memakai Hibernate q.list()/auto-discovery tipe skalar:
            // bila SQL punya banyak kolom coalesce(...)/ekspresi tanpa alias, PostgreSQL memberi
            // label sama (mis. semua "coalesce") sehingga Hibernate bisa salah tipe & melempar
            // "Bad value for type double : <teks>". Membaca by-position menghilangkan masalah itu total.
            final java.util.List<Object> pVals = new java.util.ArrayList<Object>();
            final String jdbcSql = toPositional(sql, prm, pVals);
            final String[] tipeF = tipe;
            final Hasil HF = H;
            session.doWork(new org.hibernate.jdbc.Work() {
                public void execute(java.sql.Connection conn) throws java.sql.SQLException {
                    java.sql.PreparedStatement ps = conn.prepareStatement(jdbcSql);
                    try {
                        for (int i = 0; i < pVals.size(); i++) { ps.setObject(i + 1, pVals.get(i)); }
                        // Satu baris lebih dari batas sengaja diminta: kelebihannya dipakai
                        // untuk MENGETAHUI bahwa hasilnya terpotong, bukan untuk ditampilkan.
                        ps.setMaxRows(BATAS_BARIS_LAPORAN + 1);
                        java.sql.ResultSet rs = ps.executeQuery();
                        try {
                            int nc = rs.getMetaData().getColumnCount();
                            while (rs.next()) {
                                if (HF.baris.size() >= BATAS_BARIS_LAPORAN) {
                                    HF.terpotong = true;
                                    break;
                                }
                                Object[] out = new Object[tipeF.length];
                                for (int i = 0; i < tipeF.length; i++) {
                                    if (i >= nc) { out[i] = "num".equals(tipeF[i]) ? Double.valueOf(0d) : ("tgl".equals(tipeF[i]) ? null : ""); continue; }
                                    if ("num".equals(tipeF[i])) {
                                        // Ambil via getObject lalu koersi — JANGAN getDouble langsung: bila
                                        // sebuah kolom TEKS tak sengaja ada di posisi tipe "num" (salah susun
                                        // tipe[]), getDouble melempar "Bad value for type double". Cara ini
                                        // aman: angka -> nilai, teks angka -> di-parse, selain itu -> 0.
                                        Object ov = rs.getObject(i + 1);
                                        double d = 0d;
                                        if (ov instanceof Number) { d = ((Number) ov).doubleValue(); }
                                        else if (ov != null) { try { d = Double.parseDouble(ov.toString().trim()); } catch (Exception ex) { d = 0d; } }
                                        out[i] = Double.valueOf(d);
                                    } else if ("tgl".equals(tipeF[i])) {
                                        java.sql.Timestamp t = rs.getTimestamp(i + 1); out[i] = (t == null) ? null : new java.util.Date(t.getTime());
                                    } else {
                                        String s = rs.getString(i + 1); out[i] = (s == null) ? "" : s;
                                    }
                                }
                                HF.baris.add(out);
                            }
                        } finally { rs.close(); }
                    } finally { ps.close(); }
                }
            });

            if (H.terpotong) {
                // Laporan yang diam-diam terpotong lebih berbahaya daripada laporan yang gagal:
                // angkanya terlihat wajar dan tetap dipakai untuk mengambil keputusan.
                String peringatan = "PERHATIAN: hanya " + BATAS_BARIS_LAPORAN
                        + " baris pertama yang ditampilkan. Angka di bawah TIDAK lengkap —"
                        + " persempit rentang tanggal atau pakai filter produk/kasir.";
                catatan = (catatan == null || catatan.trim().length() == 0)
                        ? peringatan : (peringatan + " " + catatan);
            }
            H.judul = judul; H.catatan = catatan; H.grup = grupIdx; H.tipe = tipe;
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/koperasi/helper/LaporanKantinUtil.java:2386");
            // Tampilkan AKAR penyebab (mis. pesan Postgres "column ... does not exist") — bukan cuma
            // "could not execute query" dari Hibernate — agar laporan yang gagal bisa langsung didiagnosa.
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) { root = root.getCause(); }
            String detail = root.getMessage();
            if (detail != null) { detail = detail.replaceAll("\\s+", " ").trim(); if (detail.length() > 400) detail = detail.substring(0, 400) + "..."; }
            String pesan = "Terjadi kesalahan: " + e.getMessage();
            if (detail != null && !detail.equals(e.getMessage())) { pesan += " — " + detail; }
            H.status = "99"; H.message = pesan;
        } finally {
            // WAJIB dibersihkan: thread dipakai ulang oleh kontainer, penanda yang tertinggal akan
            // membuat permintaan berikutnya ikut lintas satuan kerja tanpa diminta.
            LINTAS_SATKER.remove();
            SATKER_PILIHAN.remove();
        }
        return H;
    }
}
