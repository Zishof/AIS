package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.koperasi.helper.AkunKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.inventory.Produk;
import ais.database.model.library.Penyedia;
import ais.database.model.rab.SatuanKerja;

/**
 * Penjurnalan rantai <b>pengadaan &rarr; pembayaran</b> milik toko yang selama ini terputus.
 *
 * <p><b>Celah yang ditutup.</b> Sebelum ini modul kantin hanya punya dua posting (Penjualan &amp;
 * HPP). Akibatnya jurnal HPP <i>mengkredit</i> Persediaan yang tidak pernah <i>didebet</i>, Utang
 * Usaha tak pernah muncul di Neraca, dan pembayaran ke pemasok maupun penerimaan piutang tidak
 * berbekas di buku besar. Helper ini menambahkan empat jenis jurnal:</p>
 *
 * <table border="1">
 * <tr><th>Jenis</th><th>Debet</th><th>Kredit</th><th>Sumber</th></tr>
 * <tr><td>kulakan</td><td>Persediaan per barang</td><td>Utang Supplier (kredit/DP) atau Kas</td>
 *     <td>{@code koperasi.pengadaan_produk} + {@code pengadaan_faktur} + {@code payable_faktur_info}</td></tr>
 * <tr><td>bayar_hutang</td><td>Utang Supplier</td><td>Kas/Bank</td>
 *     <td>{@code koperasi.pembayaran_hutang_supplier}</td></tr>
 * <tr><td>terima_piutang</td><td>Kas/Bank</td><td>Piutang Usaha</td>
 *     <td>{@code koperasi.penerimaan_piutang_customer}</td></tr>
 * <tr><td>penyesuaian</td><td colspan="2">retur beli, retur jual, selisih opname, mutasi antar outlet</td>
 *     <td>{@code retur_pembelian}, {@code retur_penjualan}, {@code stok_opname}, {@code mutasi_stok_toko}</td></tr>
 * </table>
 *
 * <p><b>Pola pemakaian sama dengan Posting HPP/Penjualan yang sudah ada:</b> aksi {@code *_draft}
 * hanya MENGHITUNG dan menampilkan draf jurnal per transaksi (lengkap dengan alasan bila belum
 * siap), aksi {@code *_terapkan} baru menulis. Tiap dokumen sumber diberi penanda
 * {@code posting_history} / {@code posting_pembelian} sehingga tidak mungkin terposting dua kali,
 * satu transaksi basis data per dokumen, dan seluruh penulisan lewat Hibernate agar terekam Envers.</p>
 *
 * <p><b>Dokumen pembalik.</b> Modul reversal AP/AR mengoreksi dengan menerbitkan dokumen baru
 * bernominal NEGATIF (dokumen asal ditandai DIBATALKAN, tidak dihapus). Dokumen pembalik itu ikut
 * dijurnal di sini dengan sisi debet/kredit yang ditukar, sehingga pembatalan benar-benar
 * mengembalikan buku besar; dokumen asal yang dibatalkan tetapi BELUM sempat diposting dilewati.</p>
 *
 * <p>Baris yang akunnya belum lengkap TIDAK diposting dan tidak disembunyikan &mdash; ia tetap
 * tampil dengan alasannya ("akun persediaan belum diatur", dst.) supaya yang perlu dibenahi
 * kelihatan, bukan diam-diam hilang dari laporan.</p>
 */
public final class PostingKantinLanjutanHelper {

	/**
	 * Gerbang aksi granular (grid CRUD {@code TbmroleAction}). Admin global boleh; pengguna tanpa
	 * peran dianggap boleh (kompatibilitas akun lama). Kotak CRUD yang BELUM PERNAH diatur admin
	 * mengikuti visibilitas menunya -- lihat {@code EbisnisMenuKatalog.bolehAksiAkuntansi}.
	 */
	private static boolean bolehAksiMenu(Tbmuser tbmuser, String kunciMenu, String aksi) {
		if (ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		ais.database.model.Tbmrole peran = tbmuser == null ? null : tbmuser.hakAkses();
		if (peran == null) {
			return true;
		}
		return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(peran.getEbisnisMenu(), peran.getRoleId(),
				kunciMenu, aksi);
	}

	/** Balasan seragam saat aksi ditolak gerbang peran. */
	private static void tolakHak(JSONObject hasil, String pekerjaan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", "Anda tidak memiliki hak " + pekerjaan
				+ ". Hubungi admin untuk mengaktifkannya pada Grup Pengguna.");
	}


    public static final String JENIS_KULAKAN = "Kulakan Toko";
    public static final String JENIS_BAYAR_HUTANG = "Pembayaran Hutang Supplier Toko";
    public static final String JENIS_TERIMA_PIUTANG = "Penerimaan Piutang Customer Toko";
    public static final String JENIS_PENYESUAIAN = "Penyesuaian Persediaan Toko";

    private PostingKantinLanjutanHelper() {
    }

    // ==================================================================== model draf

    /** Satu draf jurnal (satu dokumen sumber = satu jurnal). */
    private static final class Draf {
        String jenis = "";
        String kunciSumber = "";      // tabel:id -- dipakai saat menandai
        long idSumber;
        String referensi = "";
        String keterangan = "";
        Date tanggal;
        final List<Akun> akunDebet = new ArrayList<Akun>();
        final List<Double> nilaiDebet = new ArrayList<Double>();
        final List<Akun> akunKredit = new ArrayList<Akun>();
        final List<Double> nilaiKredit = new ArrayList<Double>();
        final List<Long> idPengadaan = new ArrayList<Long>();   // khusus kulakan (banyak baris/faktur)
        String alasan = "";
        // Konteks sisi kredit kulakan: baru bisa dipakai setelah total faktur lengkap.
        Akun kunciKreditUtang;
        Akun kunciKreditKas;
        double dibayarAwal;

        void debet(Akun a, double n) {
            if (a == null || n <= 0) {
                return;
            }
            akunDebet.add(a);
            nilaiDebet.add(Double.valueOf(n));
        }

        void kredit(Akun a, double n) {
            if (a == null || n <= 0) {
                return;
            }
            akunKredit.add(a);
            nilaiKredit.add(Double.valueOf(n));
        }

        double totalDebet() {
            double t = 0;
            for (int i = 0; i < nilaiDebet.size(); i++) {
                t += nilaiDebet.get(i).doubleValue();
            }
            return t;
        }

        double totalKredit() {
            double t = 0;
            for (int i = 0; i < nilaiKredit.size(); i++) {
                t += nilaiKredit.get(i).doubleValue();
            }
            return t;
        }

        /** Siap diposting bila seimbang, bernilai, dan tidak ada alasan penolakan. */
        boolean siap() {
            return alasan.isEmpty() && totalDebet() > 0
                    && Math.abs(totalDebet() - totalKredit()) < 0.005;
        }

        String ringkasAkun(List<Akun> daftar, List<Double> nilai) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < daftar.size(); i++) {
                if (sb.length() > 0) {
                    sb.append(" + ");
                }
                sb.append(AkunKantinUtil.label(daftar.get(i))).append(" ")
                        .append(Common.numberFormat.get().format(nilai.get(i)));
            }
            return sb.toString();
        }
    }

    /** Baris mentah hasil query; sengaja dibaca tuntas dulu sebelum entitas di-load. */
    private static final class Mentah {
        final Object[] kolom;

        Mentah(Object[] kolom) {
            this.kolom = kolom;
        }

        long lng(int i) {
            Object v = kolom[i - 1];
            return v instanceof Number ? ((Number) v).longValue() : 0;
        }

        double dbl(int i) {
            Object v = kolom[i - 1];
            return v instanceof Number ? ((Number) v).doubleValue() : 0;
        }

        String str(int i) {
            Object v = kolom[i - 1];
            return v == null ? "" : v.toString();
        }

        boolean bool(int i) {
            Object v = kolom[i - 1];
            return v instanceof Boolean && ((Boolean) v).booleanValue();
        }

        /** Kolomnya berisi nilai (bukan NULL) -- pengganti rs.wasNull(). */
        boolean ada(int i) {
            return kolom[i - 1] != null;
        }

        Date tgl(int i) {
            Object v = kolom[i - 1];
            return v instanceof java.util.Date ? (java.util.Date) v : null;
        }
    }

    /**
     * Membaca SELURUH hasil query ke memori lalu menutup statement-nya. Wajib dilakukan sebelum
     * memanggil Hibernate (session.get / query) pada koneksi yang sama: Hibernate dapat menutup
     * statement yang sedang dipakai sehingga ResultSet-nya ikut mati di tengah perulangan.
     */
    private static List<Mentah> baca(Session session, String sql, String mulai, String sampai, int jumlahKolom)
            throws Exception {
        List<Mentah> keluar = new ArrayList<Mentah>();
        Connection conn = session.connection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, mulai);
        ps.setString(2, sampai);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Object[] k = new Object[jumlahKolom];
            for (int i = 0; i < jumlahKolom; i++) {
                k[i] = rs.getObject(i + 1);
            }
            keluar.add(new Mentah(k));
        }
        rs.close();
        ps.close();
        return keluar;
    }

    // ==================================================================== dispatch

    public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
            throws Exception {
        boolean terapkan = action.endsWith("_terapkan");
        String jenis;
        if (action.startsWith("posting_kulakan")) {
            jenis = "kulakan";
        } else if (action.startsWith("posting_bayar_hutang")) {
            jenis = "bayar_hutang";
        } else if (action.startsWith("posting_terima_piutang")) {
            jenis = "terima_piutang";
        } else if (action.startsWith("posting_penyesuaian")) {
            jenis = "penyesuaian";
        } else {
            hasil.put("status", "99");
            hasil.put("message", "Aksi posting kantin lanjutan tidak dikenal: " + action);
            return;
        }
        // Draf boleh dilihat siapa pun yang menunya tampil; MENERAPKAN (menulis jurnal) butuh
        // hak "create" pada kunci posting yang bersangkutan -- kewenangan yang biasa dipisah.
        if (terapkan && !bolehAksiMenu(tbmuser, "posting_" + jenis, "create")) {
            tolakHak(hasil, "memposting " + jenis.replace("_", " "));
            return;
        }
        jalankan(jenis, terapkan, tbmuser, payload, hasil);
        // Balasan DRAF membawa hak menerapkannya. Di layar, tombol Terapkan baru muncul
        // sesudah draf tampil -- jadi di sinilah tempat paling awal memberi tahu bahwa
        // tombol itu akan ditolak, bukan sesudah pengguna memeriksa seluruh barisnya.
        if (!terapkan && !hasil.has("hak")) {
            JSONObject hak = new JSONObject();
            hak.put("create", bolehAksiMenu(tbmuser, "posting_" + jenis, "create"));
            hasil.put("hak", hak);
        }
    }

    private static void jalankan(String jenis, boolean terapkan, Tbmuser tbmuser, JSONObject payload,
            JSONObject hasil) throws Exception {
        String mulai = payload == null ? "" : payload.optString("mulai", "").trim();
        String sampai = payload == null ? "" : payload.optString("sampai", "").trim();
        if (mulai.isEmpty() || sampai.isEmpty()) {
            hasil.put("status", "99");
            hasil.put("message", "Tanggal mulai dan sampai wajib diisi.");
            return;
        }
        // Konteks toko: akun Kas/Piutang diambil dari master Toko lebih dulu (lihat AkunKantinUtil).
        // Baris yang punya kolom toko sendiri memakai tokonya; sisanya memakai toko aktif dari POS.
        Long tokoDefault = null;
        if (payload != null && payload.has("tokoId") && !payload.isNull("tokoId")
                && payload.optLong("tokoId", 0) > 0) {
            tokoDefault = Long.valueOf(payload.optLong("tokoId"));
        }
        Set<Long> dipilih = new HashSet<Long>();
        JSONArray ids = payload == null ? null : payload.optJSONArray("posting_ids");
        for (int i = 0; ids != null && i < ids.length(); i++) {
            dipilih.add(Long.valueOf(ids.optLong(i)));
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            SatuanKerja satker = AkunKantinUtil.satkerKantin();
            List<Draf> draf;
            if ("kulakan".equals(jenis)) {
                draf = drafKulakan(session, satker, mulai, sampai, tokoDefault);
            } else if ("bayar_hutang".equals(jenis)) {
                draf = drafBayarHutang(session, satker, mulai, sampai, tokoDefault);
            } else if ("terima_piutang".equals(jenis)) {
                draf = drafTerimaPiutang(session, satker, mulai, sampai, tokoDefault);
            } else {
                draf = drafPenyesuaian(session, satker, mulai, sampai, tokoDefault);
            }

            JSONArray arr = new JSONArray();
            int siap = 0;
            double totalSiap = 0;
            for (int i = 0; i < draf.size(); i++) {
                Draf d = draf.get(i);
                JSONObject j = new JSONObject();
                j.put("id", d.idSumber);
                j.put("jenis", d.jenis);
                j.put("referensi", d.referensi);
                j.put("keterangan", d.keterangan);
                j.put("tanggal", d.tanggal == null ? "" : Common.dateFormat3.get().format(d.tanggal));
                j.put("nilai", d.totalDebet());
                j.put("debet", d.ringkasAkun(d.akunDebet, d.nilaiDebet));
                j.put("kredit", d.ringkasAkun(d.akunKredit, d.nilaiKredit));
                j.put("siap", d.siap());
				j.put("alasan", d.siap() ? "" : lengkapiLangkahPerbaikan(
						d.alasan.isEmpty() ? "Jurnal tidak seimbang" : d.alasan));
                arr.put(j);
                if (d.siap()) {
                    siap++;
                    totalSiap += d.totalDebet();
                }
            }
            hasil.put("status", "00");
            hasil.put("jenis", jenis);
            hasil.put("rincian", arr);
            hasil.put("jumlahDraf", draf.size());
            hasil.put("jumlahSiap", siap);
            hasil.put("totalSiap", totalSiap);

            if (!terapkan) {
                hasil.put("message", draf.isEmpty()
                        ? "Tidak ada dokumen yang belum diposting pada periode ini."
                        : siap + " dari " + draf.size() + " dokumen siap diposting.");
                return;
            }

            // PostingHistory.getNama() membaca tbmuser tanpa penjagaan null, jadi pastikan
            // penggunanya ada SEBELUM menulis apa pun -- kalau tidak, galatnya muncul sebagai
            // PropertyAccessException yang membingungkan.
            Tbmuser pengguna = tbmuser;
            if (pengguna == null) {
                try {
                    pengguna = Common.getCurrentUser();
                } catch (Exception e) {
                    pengguna = null;
                }
            }
            if (pengguna == null) {
                hasil.put("status", "01");
                hasil.put("message", "Sesi pengguna tidak ditemukan. Silakan masuk kembali sebelum memposting.");
                return;
            }

            int berhasil = 0;
            JSONArray masalah = new JSONArray();
            for (int i = 0; i < draf.size(); i++) {
                Draf d = draf.get(i);
                if (!d.siap()) {
                    continue;
                }
                if (!dipilih.isEmpty() && !dipilih.contains(Long.valueOf(d.idSumber))) {
                    continue;
                }
                try {
                    if (postingSatu(session, d, pengguna)) {
                        berhasil++;
                    } else {
                        masalah.put(d.referensi + ": jurnal ditolak (periode mungkin sudah ditutup).");
                    }
                } catch (Exception ex) {
                    batalkanDiam(session);
                    ais.common.ErrorAuditUtil.record(ex,
                            "auto-audit PostingKantinLanjutanHelper.postingSatu " + d.kunciSumber);
                    masalah.put(d.referensi + ": " + ex.getMessage());
                }
            }
            hasil.put("diposting", berhasil);
            hasil.put("masalah", masalah);
            hasil.put("message", berhasil + " jurnal terbentuk"
                    + (masalah.length() > 0 ? ", " + masalah.length() + " gagal." : "."));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

	/**
	 * Semua draf posting toko melewati titik ini. Pesan sebab dari kalkulator tetap
	 * dipertahankan, lalu dilengkapi lokasi setting dan tindakan sesudah simpan agar
	 * pengguna tidak berhenti pada kalimat "akun belum diatur".
	 */
	private static String lengkapiLangkahPerbaikan(String alasan) {
		String pesan = alasan == null ? "" : alasan.trim();
		if (pesan.isEmpty() || pesan.indexOf("Langkah perbaikan:") >= 0) {
			return pesan;
		}
		String kecil = pesan.toLowerCase(java.util.Locale.ENGLISH);
		String langkah = null;
		if (kecil.indexOf("persediaan") >= 0 || kecil.indexOf("hpp") >= 0) {
			langkah = "buka Master Aset atau Kelompok Aset barang terkait, lalu isi Akun Persediaan dan Akun HPP";
		} else if (kecil.indexOf("utang supplier") >= 0 || kecil.indexOf("utang penyedia") >= 0) {
			langkah = "buka Master Data > Penyedia, cari supplier terkait, lalu isi Akun Utang";
		} else if (kecil.indexOf("piutang") >= 0) {
			langkah = "buka Master Data > Toko dan lengkapi Akun Piutang Usaha; periksa juga akun pada Cara Pembayaran piutang";
		} else if (kecil.indexOf("kas/bank") >= 0 || kecil.indexOf("cara pembayaran") >= 0) {
			langkah = "buka Master Data > Cara Pembayaran, cari metode yang disebutkan dan isi kolom Akun; bila memakai fallback outlet, lengkapi Akun Kas pada Master Toko";
		} else if (kecil.indexOf("retur penjualan") >= 0 || kecil.indexOf("pendapatan") >= 0) {
			langkah = "buka Master Data > Jenis Produk/Produk terkait dan lengkapi akun Pendapatan atau Retur Penjualan";
		}
		if (langkah == null) {
			return pesan;
		}
		return pesan + " Langkah perbaikan: (1) " + langkah + "; (2) klik Simpan; "
				+ "(3) kembali ke halaman Posting dan klik Muat ulang. Jangan mengubah draf jurnal secara manual.";
	}

    // ==================================================================== penulisan

    private static boolean postingSatu(Session session, Draf d, Tbmuser tbmuser) throws Exception {
        PostingHistory ph = new PostingHistory(namaJenis(d.jenis));
        ph.setTanggal(d.tanggal == null ? new Date() : d.tanggal);
        ph.setTbmuser(tbmuser);
        ph.setKeterangan(d.keterangan);

        session.getTransaction().begin();
        boolean ok;
        try {
            session.save(ph);
            ok = CommonAkunting.saveTransaksi(d.akunDebet.toArray(new Akun[] {}),
                    d.akunKredit.toArray(new Akun[] {}), null, null, ph, true, d.keterangan,
                    d.tanggal == null ? new Date() : d.tanggal,
                    d.nilaiDebet.toArray(new Double[] {}), d.nilaiKredit.toArray(new Double[] {}),
                    Double.valueOf(0.0), null, AkunKantinUtil.satkerKantin(), session);
            if (!ok) {
                session.getTransaction().rollback();
                return false;
            }
            tandai(session, d, ph);
            session.getTransaction().commit();
            return true;
        } catch (Exception e) {
            batalkanDiam(session);
            throw e;
        }
    }

    /** Menandai dokumen sumber supaya tidak bisa diposting dua kali. */
    private static void tandai(Session session, Draf d, PostingHistory ph) {
        long idPh = ph.getId().longValue();
        if ("kulakan".equals(d.jenis)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.idPengadaan.size(); i++) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(d.idPengadaan.get(i).longValue());
            }
            if (sb.length() > 0) {
                session.createSQLQuery("UPDATE koperasi.pengadaan_produk SET posting_pembelian = " + idPh
                        + " WHERE posting_pembelian IS NULL AND id IN (" + sb + ")").executeUpdate();
            }
            return;
        }
        String tabel = d.kunciSumber.substring(0, d.kunciSumber.indexOf(':'));
        session.createSQLQuery("UPDATE " + tabel + " SET posting_history = " + idPh
                + " WHERE posting_history IS NULL AND id = " + d.idSumber).executeUpdate();
    }

    private static String namaJenis(String jenis) {
        if ("kulakan".equals(jenis)) {
            return JENIS_KULAKAN;
        }
        if ("bayar_hutang".equals(jenis)) {
            return JENIS_BAYAR_HUTANG;
        }
        if ("terima_piutang".equals(jenis)) {
            return JENIS_TERIMA_PIUTANG;
        }
        return JENIS_PENYESUAIAN;
    }

    // ==================================================================== penghitung dasbor draft jurnal

    private static int hitungSql(Session session, String sql) {
        Number n = (Number) session.createSQLQuery(sql).uniqueResult();
        return n == null ? 0 : n.intValue();
    }

    /**
     * Jumlah dokumen sumber satu jenis yang BELUM diposting pada rentang -- predikat dokumennya
     * sama dengan draf masing-masing (penanda {@code posting_history}/{@code posting_pembelian}).
     * Kesiapan akun sengaja TIDAK diperiksa di sini supaya penghitungnya murah untuk dasbor:
     * dokumen yang akunnya belum lengkap memang harus tetap terhitung draf yang menunggu
     * dibereskan, bukan menghilang dari hitungan.
     */
    public static int hitungDraftPending(Session session, String jenis, java.util.Date mulai,
            java.util.Date sampai) {
        try {
            String m = Common.databaseDateFormat.get().format(mulai);
            String s = Common.databaseDateFormat.get().format(sampai);
            String rentang = " BETWEEN date('" + m + "') AND date('" + s + "')";
            if ("kulakan".equals(jenis)) {
                return hitungSql(session, "SELECT count(*) FROM koperasi.pengadaan_produk pp"
                        + " WHERE pp.posting_pembelian IS NULL AND date(pp.waktupengadaan)" + rentang);
            }
            if ("bayar_hutang".equals(jenis)) {
                return hitungSql(session, "SELECT count(*) FROM koperasi.pembayaran_hutang_supplier p"
                        + " WHERE p.posting_history IS NULL AND date(p.tanggal)" + rentang);
            }
            if ("terima_piutang".equals(jenis)) {
                return hitungSql(session, "SELECT count(*) FROM koperasi.penerimaan_piutang_customer p"
                        + " WHERE p.posting_history IS NULL AND date(p.tanggal)" + rentang);
            }
            return hitungSql(session, "SELECT count(*) FROM koperasi.retur_pembelian r"
                            + " WHERE r.posting_history IS NULL AND date(r.waktu)" + rentang)
                    + hitungSql(session, "SELECT count(*) FROM koperasi.retur_penjualan r"
                            + " WHERE r.posting_history IS NULL AND date(r.waktu)" + rentang)
                    + hitungSql(session, "SELECT count(*) FROM koperasi.stok_opname o"
                            + " WHERE o.posting_history IS NULL AND COALESCE(o.selisih,0) <> 0"
                            + " AND date(o.waktuopname)" + rentang)
                    + hitungSql(session, "SELECT count(*) FROM koperasi.mutasi_stok_toko m"
                            + " WHERE m.posting_history IS NULL AND date(m.waktu)" + rentang);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Jumlah jurnal terposting sepanjang waktu untuk satu jenis (riwayat ber-jenis itu). */
    public static int hitungTerposting(Session session, String jenis) {
        try {
            return hitungSql(session, "SELECT count(*) FROM akunting.posting_history WHERE jenis = '"
                    + namaJenis(jenis) + "'");
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================================================================== draf: kulakan

    /**
     * Kulakan dikelompokkan PER FAKTUR bila barisnya bertaut faktur; baris lama tanpa faktur
     * dijurnal sendiri-sendiri dan diperlakukan TUNAI (sesuai kontrak modul AP: faktur tanpa
     * info termin = cash lunas).
     */
    private static List<Draf> drafKulakan(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        Map<String, Draf> peta = new LinkedHashMap<String, Draf>();
        Map<String, Double> totalFaktur = new LinkedHashMap<String, Double>();
        List<Mentah> barisMentah = baca(session,
                "SELECT pp.id, pp.produk, COALESCE(pp.totalharga, COALESCE(pp.qty,0)*COALESCE(pp.hargabelisatuan,0), 0),"
                        + " pp.waktupengadaan, COALESCE(pp.nomorfaktur,''), pp.faktur_pengadaan,"
                        + " f.supplier, COALESCE(f.nomor_faktur,''), i.jenis_pembayaran, COALESCE(i.dibayar_awal,0),"
                        + " COALESCE(pr.nama,''), COALESCE(pp.namasupplier,''), pp.toko"
                        + " FROM koperasi.pengadaan_produk pp"
                        + " LEFT JOIN koperasi.produk pr ON pr.id = pp.produk"
                        + " LEFT JOIN koperasi.pengadaan_faktur f ON f.id = pp.faktur_pengadaan"
                        + " LEFT JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id"
                        + " WHERE pp.posting_pembelian IS NULL"
                        + " AND date(pp.waktupengadaan) BETWEEN date(?) AND date(?)"
                        + " ORDER BY pp.waktupengadaan, pp.id", mulai, sampai, 13);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long idBaris = m.lng(1);
            long idProduk = m.lng(2);
            double nilai = m.dbl(3);
            Date tanggal = m.tgl(4);
            String noFaktur = m.str(5);
            long idFaktur = m.lng(6);
            boolean adaFaktur = m.ada(6);
            long idSupplier = m.lng(7);
            boolean adaSupplier = m.ada(7);
            String noFakturHeader = m.str(8);
            String jenisBayar = m.str(9);
            double dibayarAwal = m.dbl(10);
            String namaProduk = m.str(11);
            String namaSupplierTeks = m.str(12);
            Long tokoBaris = m.ada(13) ? Long.valueOf(m.lng(13)) : tokoDefault;

            String kunci = adaFaktur ? ("F" + idFaktur) : ("P" + idBaris);
            Draf d = peta.get(kunci);
            if (d == null) {
                d = new Draf();
                d.jenis = "kulakan";
                d.kunciSumber = "koperasi.pengadaan_produk:" + idBaris;
                d.idSumber = adaFaktur ? idFaktur : idBaris;
                d.referensi = adaFaktur
                        ? ("Faktur " + (noFakturHeader.isEmpty() ? ("#" + idFaktur) : noFakturHeader))
                        : ("Kulakan #" + idBaris + (noFaktur.isEmpty() ? "" : " / " + noFaktur));
                d.tanggal = tanggal;
                d.keterangan = "Kulakan toko " + d.referensi
                        + (namaSupplierTeks.isEmpty() ? "" : " - " + namaSupplierTeks);
                peta.put(kunci, d);
                totalFaktur.put(kunci, Double.valueOf(0));
                // Sisi kredit ditentukan sekali per dokumen (lihat di bawah, setelah total diketahui).
                d.akunKredit.clear();
                if (adaFaktur && adaSupplier) {
                    Penyedia sup = (Penyedia) session.get(Penyedia.class, Long.valueOf(idSupplier));
                    d.akunDebet.clear();
                    simpanKonteksKredit(d, session, sup, jenisBayar, dibayarAwal, tokoBaris);
                } else {
                    simpanKonteksKredit(d, session, null, null, 0, tokoBaris);
                }
            }
            d.idPengadaan.add(Long.valueOf(idBaris));
            totalFaktur.put(kunci, Double.valueOf(totalFaktur.get(kunci).doubleValue() + nilai));

            Produk produk = (Produk) session.get(Produk.class, Long.valueOf(idProduk));
            Akun persediaan = AkunKantinUtil.akunPersediaan(session, produk, satker);
            if (persediaan == null) {
                d.alasan = "Akun persediaan belum diatur untuk barang "
                        + (namaProduk.isEmpty() ? ("#" + idProduk) : namaProduk)
                        + " (isi Akun Transaksi pada Master Aset / Kelompok Aset).";
            } else {
                gabungDebet(d, persediaan, nilai);
            }
        }

        // Sisi kredit baru bisa dihitung setelah total tiap dokumen lengkap.
        List<Draf> keluar = new ArrayList<Draf>();
        for (Map.Entry<String, Draf> e : peta.entrySet()) {
            Draf d = e.getValue();
            double total = totalFaktur.get(e.getKey()).doubleValue();
            selesaikanKreditKulakan(d, total);
            keluar.add(d);
        }
        return keluar;
    }

    /** Menyimpan konteks kredit (akun utang/kas + nilai DP) pada draf, dipakai saat total lengkap. */
    private static void simpanKonteksKredit(Draf d, Session session, Penyedia supplier, String jenisBayar,
            double dibayarAwal, Long tokoId) {
        boolean kredit = jenisBayar != null
                && (jenisBayar.toUpperCase().indexOf("CREDIT") >= 0 || jenisBayar.toUpperCase().indexOf("KREDIT") >= 0
                        || jenisBayar.toUpperCase().indexOf("DP") >= 0);
        Akun akunKas = AkunKantinUtil.akunKasBank(session, null, tokoId);
        if (!kredit) {
            if (akunKas == null) {
                d.alasan = "Akun Kas/Bank toko belum diatur (konfigurasi " + AkunKantinUtil.CFG_KAS_TOKO + ").";
            }
            d.kunciKreditKas = akunKas;
            d.kunciKreditUtang = null;
            d.dibayarAwal = 0;
            return;
        }
        Akun akunUtang = AkunKantinUtil.akunUtangSupplier(session, supplier);
        if (akunUtang == null) {
            d.alasan = "Akun Utang supplier belum diatur (isi Akun Utang pada master Penyedia atau konfigurasi "
                    + AkunKantinUtil.CFG_UTANG_SUPPLIER + ").";
        }
        if (dibayarAwal > 0 && akunKas == null) {
            d.alasan = "Faktur ini punya pembayaran awal, tetapi akun Kas/Bank toko belum diatur.";
        }
        d.kunciKreditUtang = akunUtang;
        d.kunciKreditKas = akunKas;
        d.dibayarAwal = dibayarAwal;
    }

    private static void selesaikanKreditKulakan(Draf d, double total) {
        if (d.kunciKreditUtang == null && d.kunciKreditKas != null) {
            d.kredit(d.kunciKreditKas, total);
            return;
        }
        double dp = d.dibayarAwal > total ? total : d.dibayarAwal;
        if (dp > 0) {
            d.kredit(d.kunciKreditKas, dp);
        }
        double sisa = total - dp;
        if (sisa > 0) {
            d.kredit(d.kunciKreditUtang, sisa);
        }
    }

    /** Menggabung nilai debet ke akun yang sama supaya jurnal tidak berbaris-baris kembar. */
    private static void gabungDebet(Draf d, Akun akun, double nilai) {
        for (int i = 0; i < d.akunDebet.size(); i++) {
            Akun a = d.akunDebet.get(i);
            if (a != null && akun != null && a.getId() != null && a.getId().equals(akun.getId())) {
                d.nilaiDebet.set(i, Double.valueOf(d.nilaiDebet.get(i).doubleValue() + nilai));
                return;
            }
        }
        d.debet(akun, nilai);
    }

    // ==================================================================== draf: bayar hutang

    private static List<Draf> drafBayarHutang(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        List<Mentah> barisMentah = baca(session,
                "SELECT p.id, p.supplier, COALESCE(p.nominal,0), p.tanggal, COALESCE(p.metode,''),"
                        + " COALESCE(p.keterangan,''), COALESCE(p.kode_unik,''), COALESCE(s.nama,''),"
                        + " COALESCE(p.status_dok,'')"
                        + " FROM koperasi.pembayaran_hutang_supplier p"
                        + " LEFT JOIN library.penyedia s ON s.id = p.supplier"
                        + " WHERE p.posting_history IS NULL"
                        + " AND date(p.tanggal) BETWEEN date(?) AND date(?)"
                        + " ORDER BY p.tanggal, p.id", mulai, sampai, 9);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long id = m.lng(1);
            long idSupplier = m.lng(2);
            boolean adaSupplier = m.ada(2);
            double nominal = m.dbl(3);
            Date tanggal = m.tgl(4);
            String metode = m.str(5);
            String ket = m.str(6);
            String kodeUnik = m.str(7);
            String namaSupplier = m.str(8);
            String statusDok = m.str(9);

            Draf d = new Draf();
            d.jenis = "bayar_hutang";
            d.kunciSumber = "koperasi.pembayaran_hutang_supplier:" + id;
            d.idSumber = id;
            d.referensi = kodeUnik.isEmpty() ? ("Bayar #" + id) : kodeUnik;
            d.tanggal = tanggal;
            d.keterangan = "Pembayaran hutang supplier " + namaSupplier + " " + d.referensi
                    + (ket.isEmpty() ? "" : " - " + ket);

            if (statusDok != null && statusDok.toUpperCase().indexOf("BATAL") >= 0) {
                d.alasan = "Dokumen berstatus " + statusDok + ", tidak dijurnal.";
            }
            Penyedia sup = adaSupplier ? (Penyedia) session.get(Penyedia.class, Long.valueOf(idSupplier)) : null;
            Akun akunUtang = AkunKantinUtil.akunUtangSupplier(session, sup);
            Akun akunKas = AkunKantinUtil.akunKasBank(session, metode, tokoDefault);
            if (akunUtang == null) {
                d.alasan = "Akun Utang supplier belum diatur (master Penyedia / konfigurasi "
                        + AkunKantinUtil.CFG_UTANG_SUPPLIER + ").";
            } else if (akunKas == null) {
                d.alasan = "Akun Kas/Bank untuk metode '" + metode + "' belum diatur (master Cara Pembayaran"
                        + " atau konfigurasi " + AkunKantinUtil.CFG_KAS_TOKO + ").";
            } else if (Math.abs(nominal) < 0.005) {
                d.alasan = "Nominal pembayaran nol.";
            } else if (nominal < 0) {
                // Dokumen PEMBALIK dari modul reversal (nominalnya negatif): sisi jurnal ditukar
                // supaya pembatalan benar-benar mengembalikan buku besar. Tanpa ini, pembayaran
                // yang sudah diposting lalu dibatalkan akan meninggalkan utang yang terlanjur
                // berkurang di jurnal.
                d.debet(akunKas, -nominal);
                d.kredit(akunUtang, -nominal);
                d.keterangan = "Pembalik - " + d.keterangan;
            } else {
                d.debet(akunUtang, nominal);
                d.kredit(akunKas, nominal);
            }
            keluar.add(d);
        }
        return keluar;
    }

    // ==================================================================== draf: terima piutang

    private static List<Draf> drafTerimaPiutang(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        // Akun piutang dibaca DULU (memanggil Hibernate) supaya tidak ada query lain yang berjalan
        // saat hasil query di bawah sedang dibaca.
        Akun akunPiutang = AkunKantinUtil.akunPiutang(session, tokoDefault);
        List<Mentah> barisMentah = baca(session,
                "SELECT p.id, COALESCE(p.nominal,0), p.tanggal, COALESCE(p.metode,''), COALESCE(p.nomor,''),"
                        + " COALESCE(p.keterangan,''), COALESCE(a.nama,''), COALESCE(p.status_dok,'')"
                        + " FROM koperasi.penerimaan_piutang_customer p"
                        + " LEFT JOIN koperasi.anggota_koperasi a ON a.id = p.customer"
                        + " WHERE p.posting_history IS NULL"
                        + " AND date(p.tanggal) BETWEEN date(?) AND date(?)"
                        + " ORDER BY p.tanggal, p.id", mulai, sampai, 8);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long id = m.lng(1);
            double nominal = m.dbl(2);
            Date tanggal = m.tgl(3);
            String metode = m.str(4);
            String nomor = m.str(5);
            String ket = m.str(6);
            String namaCustomer = m.str(7);
            String statusDok = m.str(8);

            Draf d = new Draf();
            d.jenis = "terima_piutang";
            d.kunciSumber = "koperasi.penerimaan_piutang_customer:" + id;
            d.idSumber = id;
            d.referensi = nomor.isEmpty() ? ("Terima #" + id) : nomor;
            d.tanggal = tanggal;
            d.keterangan = "Penerimaan piutang " + namaCustomer + " " + d.referensi
                    + (ket.isEmpty() ? "" : " - " + ket);

            Akun akunKas = AkunKantinUtil.akunKasBank(session, metode, tokoDefault);
            if (statusDok != null && statusDok.toUpperCase().indexOf("BATAL") >= 0) {
                d.alasan = "Dokumen berstatus " + statusDok + ", tidak dijurnal.";
            } else if (akunKas == null) {
                d.alasan = "Akun Kas/Bank untuk metode '" + metode + "' belum diatur.";
            } else if (akunPiutang == null) {
                d.alasan = "Akun Piutang Usaha toko belum diatur (konfigurasi "
                        + AkunKantinUtil.CFG_PIUTANG_TOKO + " atau akun pada Cara Pembayaran piutang).";
            } else if (Math.abs(nominal) < 0.005) {
                d.alasan = "Nominal penerimaan nol.";
            } else if (nominal < 0) {
                // Dokumen PEMBALIK (nominal negatif) -- sisi jurnal ditukar, lihat catatan
                // pada draf pembayaran hutang.
                d.debet(akunPiutang, -nominal);
                d.kredit(akunKas, -nominal);
                d.keterangan = "Pembalik - " + d.keterangan;
            } else {
                d.debet(akunKas, nominal);
                d.kredit(akunPiutang, nominal);
            }
            keluar.add(d);
        }
        return keluar;
    }

    // ==================================================================== draf: penyesuaian

    private static List<Draf> drafPenyesuaian(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        keluar.addAll(drafReturPembelian(session, satker, mulai, sampai, tokoDefault));
        keluar.addAll(drafReturPenjualan(session, satker, mulai, sampai, tokoDefault));
        keluar.addAll(drafOpname(session, satker, mulai, sampai, tokoDefault));
        keluar.addAll(drafMutasi(session, satker, mulai, sampai, tokoDefault));
        return keluar;
    }

    /** Retur beli: barang keluar kembali ke pemasok -> debet Utang/Kas, kredit Persediaan. */
    private static List<Draf> drafReturPembelian(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        List<Mentah> barisMentah = baca(session,
                "SELECT r.id, r.produk, COALESCE(r.totalnilai, COALESCE(r.qty,0)*COALESCE(r.hargasatuan,0), 0),"
                        + " r.waktu, r.supplier, COALESCE(r.alasan,''), COALESCE(pr.nama,''), COALESCE(s.nama,''), r.toko"
                        + " FROM koperasi.retur_pembelian r"
                        + " LEFT JOIN koperasi.produk pr ON pr.id = r.produk"
                        + " LEFT JOIN library.penyedia s ON s.id = r.supplier"
                        + " WHERE r.posting_history IS NULL AND date(r.waktu) BETWEEN date(?) AND date(?)"
                        + " ORDER BY r.waktu, r.id", mulai, sampai, 9);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long id = m.lng(1);
            long idProduk = m.lng(2);
            double nilai = m.dbl(3);
            Date tanggal = m.tgl(4);
            long idSupplier = m.lng(5);
            boolean adaSupplier = m.ada(5);
            String alasan = m.str(6);
            String namaProduk = m.str(7);
            String namaSupplier = m.str(8);
            Long tokoBaris = m.ada(9) ? Long.valueOf(m.lng(9)) : tokoDefault;

            Draf d = new Draf();
            d.jenis = "penyesuaian";
            d.kunciSumber = "koperasi.retur_pembelian:" + id;
            d.idSumber = id;
            d.referensi = "Retur Beli #" + id;
            d.tanggal = tanggal;
            d.keterangan = "Retur pembelian " + namaProduk + " ke " + namaSupplier
                    + (alasan.isEmpty() ? "" : " (" + alasan + ")");

            Produk produk = (Produk) session.get(Produk.class, Long.valueOf(idProduk));
            Akun persediaan = AkunKantinUtil.akunPersediaan(session, produk, satker);
            Penyedia sup = adaSupplier ? (Penyedia) session.get(Penyedia.class, Long.valueOf(idSupplier)) : null;
            Akun lawan = AkunKantinUtil.akunUtangSupplier(session, sup);
            if (lawan == null) {
                lawan = AkunKantinUtil.akunKasBank(session, null, tokoBaris);
            }
            if (persediaan == null) {
                d.alasan = "Akun persediaan belum diatur untuk barang " + namaProduk + ".";
            } else if (lawan == null) {
                d.alasan = "Akun Utang supplier / Kas belum diatur.";
            } else if (nilai <= 0) {
                d.alasan = "Nilai retur nol.";
            } else {
                d.debet(lawan, nilai);
                d.kredit(persediaan, nilai);
            }
            keluar.add(d);
        }
        return keluar;
    }

    /**
     * Retur jual: debet Retur Penjualan (kontra-pendapatan), kredit Kas/Piutang. Bila barang
     * kembali ke stok, ditambah pasangan debet Persediaan / kredit HPP sebesar harga pokoknya
     * supaya persediaan dan HPP ikut terkoreksi.
     */
    private static List<Draf> drafReturPenjualan(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        List<Mentah> barisMentah = baca(session,
                "SELECT r.id, r.produk, COALESCE(r.totalnilai, COALESCE(r.qty,0)*COALESCE(r.hargasatuan,0), 0),"
                        + " r.waktu, COALESCE(r.kembalikan_ke_stok,false), COALESCE(r.qty,0),"
                        + " COALESCE(pr.hargabeli,0), COALESCE(pr.nama,''), COALESCE(r.metodepengembalian,''),"
                        + " COALESCE(r.namapembeli,''), r.toko"
                        + " FROM koperasi.retur_penjualan r"
                        + " LEFT JOIN koperasi.produk pr ON pr.id = r.produk"
                        + " WHERE r.posting_history IS NULL AND date(r.waktu) BETWEEN date(?) AND date(?)"
                        + " ORDER BY r.waktu, r.id", mulai, sampai, 11);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long id = m.lng(1);
            long idProduk = m.lng(2);
            double nilai = m.dbl(3);
            Date tanggal = m.tgl(4);
            boolean keStok = m.bool(5);
            double qty = m.dbl(6);
            double hargaBeli = m.dbl(7);
            String namaProduk = m.str(8);
            String metode = m.str(9);
            String pembeli = m.str(10);
            Long tokoBaris = m.ada(11) ? Long.valueOf(m.lng(11)) : tokoDefault;

            Draf d = new Draf();
            d.jenis = "penyesuaian";
            d.kunciSumber = "koperasi.retur_penjualan:" + id;
            d.idSumber = id;
            d.referensi = "Retur Jual #" + id;
            d.tanggal = tanggal;
            d.keterangan = "Retur penjualan " + namaProduk
                    + (pembeli.isEmpty() ? "" : " dari " + pembeli);

            Produk produk = (Produk) session.get(Produk.class, Long.valueOf(idProduk));
            Akun akunRetur = AkunKantinUtil.akunReturPenjualan(session, produk);
            Akun akunKas = AkunKantinUtil.akunKasBank(session, metode, tokoBaris);
            if (akunRetur == null) {
                d.alasan = "Akun Retur Penjualan / Pendapatan belum diatur untuk barang " + namaProduk + ".";
            } else if (akunKas == null) {
                d.alasan = "Akun Kas/Bank pengembalian belum diatur.";
            } else if (nilai <= 0) {
                d.alasan = "Nilai retur nol.";
            } else {
                d.debet(akunRetur, nilai);
                d.kredit(akunKas, nilai);
                if (keStok && qty > 0 && hargaBeli > 0) {
                    Akun persediaan = AkunKantinUtil.akunPersediaan(session, produk, satker);
                    Akun akunHpp = AkunKantinUtil.akunHpp(session, produk, satker);
                    if (persediaan != null && akunHpp != null) {
                        double pokok = qty * hargaBeli;
                        d.debet(persediaan, pokok);
                        d.kredit(akunHpp, pokok);
                        d.keterangan = d.keterangan + " (barang kembali ke stok)";
                    }
                }
            }
            keluar.add(d);
        }
        return keluar;
    }

    /** Selisih opname dinilai dengan harga beli produk: minus = susut (beban), plus = temuan. */
    private static List<Draf> drafOpname(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        List<Mentah> barisMentah = baca(session,
                "SELECT o.id, o.produk, COALESCE(o.selisih,0), o.waktuopname, COALESCE(pr.hargabeli,0),"
                        + " COALESCE(pr.nama,''), COALESCE(o.keterangan,'')"
                        + " FROM koperasi.stok_opname o"
                        + " LEFT JOIN koperasi.produk pr ON pr.id = o.produk"
                        + " WHERE o.posting_history IS NULL AND COALESCE(o.selisih,0) <> 0"
                        + " AND date(o.waktuopname) BETWEEN date(?) AND date(?)"
                        + " ORDER BY o.waktuopname, o.id", mulai, sampai, 7);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long id = m.lng(1);
            long idProduk = m.lng(2);
            double selisih = m.dbl(3);
            Date tanggal = m.tgl(4);
            double hargaBeli = m.dbl(5);
            String namaProduk = m.str(6);
            String ket = m.str(7);

            Draf d = new Draf();
            d.jenis = "penyesuaian";
            d.kunciSumber = "koperasi.stok_opname:" + id;
            d.idSumber = id;
            d.referensi = "Opname #" + id;
            d.tanggal = tanggal;
            d.keterangan = "Selisih stok opname " + namaProduk + " " + Common.numberFormat.get().format(selisih)
                    + (ket.isEmpty() ? "" : " - " + ket);

            Produk produk = (Produk) session.get(Produk.class, Long.valueOf(idProduk));
            Akun persediaan = AkunKantinUtil.akunPersediaan(session, produk, satker);
            Akun akunSelisih = AkunKantinUtil.akunSelisihPersediaan(session, produk, satker);
            double nilai = Math.abs(selisih) * hargaBeli;
            if (persediaan == null) {
                d.alasan = "Akun persediaan belum diatur untuk barang " + namaProduk + ".";
            } else if (akunSelisih == null) {
                d.alasan = "Akun selisih persediaan belum diatur (konfigurasi "
                        + AkunKantinUtil.CFG_SELISIH_PERSEDIAAN + ").";
            } else if (nilai <= 0) {
                d.alasan = "Harga pokok barang nol, selisih tidak dapat dinilai.";
            } else if (selisih < 0) {
                d.debet(akunSelisih, nilai);
                d.kredit(persediaan, nilai);
            } else {
                d.debet(persediaan, nilai);
                d.kredit(akunSelisih, nilai);
            }
            keluar.add(d);
        }
        return keluar;
    }

    /**
     * Mutasi antar outlet hanya berdampak akuntansi bila akun persediaan kedua outlet BERBEDA;
     * bila sama, perpindahan tidak mengubah buku besar sehingga baris langsung ditandai "tidak
     * perlu jurnal" (dan tetap ditampilkan agar tidak terkesan hilang).
     */
    private static List<Draf> drafMutasi(Session session, SatuanKerja satker, String mulai, String sampai,
            Long tokoDefault)
            throws Exception {
        List<Draf> keluar = new ArrayList<Draf>();
        List<Mentah> barisMentah = baca(session,
                "SELECT m.id, m.produk_asal, m.produk_tujuan, COALESCE(m.qty,0), m.waktu,"
                        + " COALESCE(pa.hargabeli,0), COALESCE(pa.nama,''), COALESCE(ta.nama,''), COALESCE(tt.nama,'')"
                        + " FROM koperasi.mutasi_stok_toko m"
                        + " LEFT JOIN koperasi.produk pa ON pa.id = m.produk_asal"
                        + " LEFT JOIN koperasi.toko ta ON ta.id = m.toko_asal"
                        + " LEFT JOIN koperasi.toko tt ON tt.id = m.toko_tujuan"
                        + " WHERE m.posting_history IS NULL AND date(m.waktu) BETWEEN date(?) AND date(?)"
                        + " ORDER BY m.waktu, m.id", mulai, sampai, 9);
        for (int bi = 0; bi < barisMentah.size(); bi++) {
            Mentah m = barisMentah.get(bi);
            long id = m.lng(1);
            long idAsal = m.lng(2);
            long idTujuan = m.lng(3);
            double qty = m.dbl(4);
            Date tanggal = m.tgl(5);
            double hargaBeli = m.dbl(6);
            String namaProduk = m.str(7);
            String tokoAsal = m.str(8);
            String tokoTujuan = m.str(9);

            Draf d = new Draf();
            d.jenis = "penyesuaian";
            d.kunciSumber = "koperasi.mutasi_stok_toko:" + id;
            d.idSumber = id;
            d.referensi = "Mutasi #" + id;
            d.tanggal = tanggal;
            d.keterangan = "Mutasi " + namaProduk + " dari " + tokoAsal + " ke " + tokoTujuan;

            Produk pAsal = (Produk) session.get(Produk.class, Long.valueOf(idAsal));
            Produk pTujuan = m.ada(3) ? (Produk) session.get(Produk.class, Long.valueOf(idTujuan)) : null;
            Akun akunAsal = AkunKantinUtil.akunPersediaan(session, pAsal, satker);
            Akun akunTujuan = pTujuan == null ? akunAsal : AkunKantinUtil.akunPersediaan(session, pTujuan, satker);
            double nilai = qty * hargaBeli;
            if (akunAsal == null || akunTujuan == null) {
                d.alasan = "Akun persediaan belum diatur untuk barang " + namaProduk + ".";
            } else if (akunAsal.getId() != null && akunAsal.getId().equals(akunTujuan.getId())) {
                d.alasan = "Akun persediaan kedua outlet sama, tidak perlu jurnal.";
            } else if (nilai <= 0) {
                d.alasan = "Nilai mutasi nol.";
            } else {
                d.debet(akunTujuan, nilai);
                d.kredit(akunAsal, nilai);
            }
            keluar.add(d);
        }
        return keluar;
    }

    private static void batalkanDiam(Session session) {
        try {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PostingKantinLanjutanHelper.batalkanDiam");
        }
    }
}
