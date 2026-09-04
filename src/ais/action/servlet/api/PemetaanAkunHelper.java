package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.database.model.asset.MasterAsset;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.library.Penyedia;

/**
 * Pemetaan otomatis akun ke Kelompok Laporan.
 *
 * <p><b>Masalahnya.</b> Laporan resmi berbasis jurnal (Laba Rugi, Neraca) hanya menampilkan akun yang
 * sudah dipetakan ke Kelompok Laporan. Akun yang belum dipetakan nilainya tidak ikut terhitung, dan
 * pada data nyata mayoritas akun memang belum dipetakan sehingga laporan resmi tampak timpang.</p>
 *
 * <p><b>Cara kerja.</b> Kelompok tidak ditebak dari kata kunci, melainkan diambil dari BAGAN AKUN itu
 * sendiri: tiap akun ditelusuri rantai induknya sampai akar, lalu
 * <ul>
 *   <li>jenis laporan ditentukan akar-nya (1/2/3/7/8 = Neraca, 4/5/6 = Rugi Laba), dan</li>
 *   <li>kelompoknya memakai nama akun induk pada jenjang ke-3 (jatuh ke jenjang ke-2 lalu akar bila
 *       jenjangnya lebih dangkal) &mdash; mis. 512.112 BEBAN ADM.BANK masuk kelompok
 *       "BEBAN ADMINISTRASI UMUM" (512.000), 111.101 KAS YAYASAN masuk "KAS" (111.000).</li>
 * </ul>
 * Dengan begitu nama kelompok selalu memakai istilah yang sudah dipakai lembaga pada bagan akunnya.</p>
 *
 * <p><b>Aturan aman.</b> Hanya MENAMBAH: akun yang sudah punya kelompok aktif tidak disentuh, tidak
 * ada baris yang dihapus atau dipindah. Kelompok yang sudah ada dipakai ulang (pembandingan nama
 * dinormalkan, ASET/AKTIVA dianggap sama) dan hanya dibuat baru bila memang belum ada. Bila master
 * global Jenis Laporan belum memiliki Neraca atau Rugi Laba, mode terapkan membuat tepat dua master
 * standar itu secara idempoten; tanpa keduanya tidak satu pun akun dapat masuk laporan resmi. Semua
 * penulisan lewat Hibernate supaya terekam Envers, satu transaksi per baris.</p>
 *
 * <p>Aksi API: {@code pemetaan_akun_usulan} (pratinjau, tidak menulis apa pun, tidak digerbangi --
 * hanya membaca) dan {@code pemetaan_akun_terapkan} (MENULIS, digerbangi kunci menu
 * {@code pemetaan_akun} aksi {@code create} lewat {@link #bolehAksiMenu}, pola yang sama dengan
 * {@code TutupBukuHelper}/{@code SaldoAwalAkunHelper}/{@code JurnalPenyesuaianHelper} di paket ini
 * -- sebelum gerbang ini dipasang, cabang {@code pemetaan_akun_} di {@code PosApi} meneruskan
 * langsung ke sini tanpa pemeriksaan hak apa pun).</p>
 *
 * <p><b>Seksi laporan (masterGrupLaporan).</b> Baris {@link KelompokLaporan} baru yang dibuat di
 * sini diisi {@code masterGrupLaporan} HANYA bila nama AKAR bagan akun (mis. "AKTIVA", "KEWAJIBAN")
 * sudah cocok dengan {@link MasterGrupLaporan} yang memang ada di data tenant ini (pencocokan nama
 * dinormalkan lewat {@link #normal(String)}, tanpa membuat baris {@code MasterGrupLaporan} baru dan
 * tanpa menebak konvensi digit kode akun) -- konsisten dengan aturan aman "hanya menambah, tidak
 * menebak" di bawah. Bila tidak cocok, kolomnya dibiarkan {@code null} seperti sebelumnya (baris
 * tetap tercetak di bawah judul semu "Lainnya", lihat Javadoc {@link KelompokLaporan}).</p>
 */
public final class PemetaanAkunHelper {

    private PemetaanAkunHelper() {
    }

    /**
     * Gerbang aksi granular (grid CRUD {@code TbmroleAction}), pola identik dengan
     * {@code TutupBukuHelper.bolehAksiMenu}/{@code SaldoAwalAkunHelper.bolehAksiMenu}. Admin global
     * boleh; pengguna tanpa peran dianggap boleh (kompatibilitas akun lama). Kotak CRUD yang BELUM
     * PERNAH diatur admin mengikuti visibilitas menunya -- lihat
     * {@code EbisnisMenuKatalog.bolehAksiAkuntansi}.
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

    /**
     * Hak menu Pemetaan Akun, dikirim bersama PRATINJAU (usulan) -- di situlah tombol "Terapkan"
     * berada, tempat paling tepat memberi tahu bahwa tombolnya akan ditolak. Pemetaan akun hanya
     * mengenal satu wewenang: menerapkan.
     *
     * <p>Bukan gerbang: gerbang sebenarnya tetap pemeriksaan pada cabang
     * {@code pemetaan_akun_terapkan} di bawah.</p>
     */
    private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
        JSONObject j = new JSONObject();
        j.put("create", bolehAksiMenu(tbmuser, "pemetaan_akun", "create"));
        return j;
    }

    /** Satu akun beserta posisinya pada bagan. */
    private static final class Simpul {
        long id;
        String kode = "";
        String nama = "";
        Long parent;
    }

    /** Usulan pemetaan satu akun. */
    private static final class Usul {
        Simpul akun;
        Simpul akar;       // akar rantai induk akun ini, dipakai mencocokkan MasterGrupLaporan
        String jenis;      // "Neraca" / "Rugi Laba"
        String kelompok;   // nama kelompok tujuan
        String kodeKelompok;
    }

    public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
        if ("pemetaan_akun_kantin_audit".equals(action)) {
            jalankanKantin(payload, hasil, false);
            hasil.put("hak", hakAksesJson(tbmuser));
        } else if ("pemetaan_akun_kantin_terapkan".equals(action)) {
            if (!bolehAksiMenu(tbmuser, "pemetaan_akun", "create")) {
                tolakHak(hasil, "menerapkan pemetaan akun Kantin/POS");
                return;
            }
            jalankanKantin(payload, hasil, true);
        } else if ("pemetaan_akun_usulan".equals(action)) {
            jalankan(payload, hasil, false);
            hasil.put("hak", hakAksesJson(tbmuser));
        } else if ("pemetaan_akun_terapkan".equals(action)) {
            if (!bolehAksiMenu(tbmuser, "pemetaan_akun", "create")) {
                tolakHak(hasil, "menerapkan pemetaan akun");
                return;
            }
            jalankan(payload, hasil, true);
        } else {
            hasil.put("status", "99");
            hasil.put("message", "Aksi pemetaan akun tidak dikenal: " + action);
        }
    }

    /**
     * Audit/perbaikan idempoten untuk rantai Kantin -&gt; POS -&gt; Kulakan -&gt; Akuntansi.
     *
     * <p>Kode akun bawaan diambil dari bagan akun eBisnis yang dilampirkan pada UAT 4 September
     * 2026. Server selalu me-resolve KODE ke id tenant saat ini; id tidak pernah di-hardcode.
     * Mode terapkan hanya mengisi relasi kosong, kecuali payload {@code timpa=true} diberikan
     * secara eksplisit. Produk yang diperiksa dibatasi pada produk yang masih mempunyai transaksi
     * penjualan/HPP atau kulakan yang belum diposting, sehingga tidak menyapu katalog tenant di
     * luar lingkup UAT.</p>
     */
    private static void jalankanKantin(JSONObject payload, JSONObject hasil, boolean terapkan) throws Exception {
        JSONObject p = payload == null ? new JSONObject() : payload;
        Long tokoId = p.has("tokoId") && !p.isNull("tokoId") && p.optLong("tokoId", 0) > 0
                ? Long.valueOf(p.optLong("tokoId")) : null;
        if (terapkan && tokoId == null) {
            hasil.put("status", "91");
            hasil.put("description", "tokoId wajib dipilih agar pemetaan akun Kantin tidak mengenai toko lain.");
            return;
        }
        boolean timpa = p.optBoolean("timpa", false);
        String kodeKas = p.optString("kodeKas", "111.101").trim();
        String kodePiutang = p.optString("kodePiutang", "131.300").trim();
        String kodePersediaan = p.optString("kodePersediaan", "151.200").trim();
        String kodeUtang = p.optString("kodeUtang", "310.600").trim();
        String kodePendapatan = p.optString("kodePendapatan", "410.900").trim();
        String kodeHpp = p.optString("kodeHpp", "510.900").trim();
        String mulai = p.optString("mulai", "").trim();
        String sampai = p.optString("sampai", "").trim();
        if (mulai.isEmpty() || sampai.isEmpty()) {
            mulai = "";
            sampai = "";
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            Akun kas = akunKantinDariKode(session, kodeKas, "Kas");
            Akun piutang = akunKantinDariKode(session, kodePiutang, "Piutang Usaha Toko");
            Akun persediaan = akunKantinDariKode(session, kodePersediaan, "Persediaan");
            Akun utang = akunKantinDariKode(session, kodeUtang, "Utang Usaha Toko");
            Akun pendapatan = akunKantinDariKode(session, kodePendapatan, "Pendapatan Penjualan Toko");
            Akun hpp = akunKantinDariKode(session, kodeHpp, "Beban Pokok Penjualan Toko");

            JSONArray akun = new JSONArray();
            akun.put(relasiAkun("Kas/Tunai", kas));
            akun.put(relasiAkun("Piutang penjualan kredit", piutang));
            akun.put(relasiAkun("Persediaan barang kantin", persediaan));
            akun.put(relasiAkun("Utang supplier", utang));
            akun.put(relasiAkun("Pendapatan penjualan", pendapatan));
            akun.put(relasiAkun("HPP", hpp));

            List<Long> produkId = produkBelumPosting(session.connection(), tokoId, mulai, sampai);
            Set<Long> jenisId = new HashSet<Long>();
            int produkTanpaMaster = 0;
            int produkTanpaPersediaan = 0;
            int masterDibuat = 0;
            int masterDipetakan = 0;
            int jenisTanpaPendapatan = 0;
            int jenisTanpaHpp = 0;
            int jenisDipetakan = 0;

            List<Produk> produk = new ArrayList<Produk>();
            for (int i = 0; i < produkId.size(); i++) {
                Produk pr = (Produk) session.get(Produk.class, produkId.get(i));
                if (pr == null) continue;
                produk.add(pr);
                if (pr.getJenisProduk() != null && pr.getJenisProduk().getId() != null) {
                    jenisId.add(pr.getJenisProduk().getId());
                }
                MasterAsset ma = pr.getMasterAsset();
                if (ma == null) {
                    produkTanpaMaster++;
                } else if (!formulaPunyaAkun(ma.akunTransaksiEfektif())) {
                    produkTanpaPersediaan++;
                }
            }
            for (java.util.Iterator<Long> it = jenisId.iterator(); it.hasNext();) {
                JenisProduk jp = (JenisProduk) session.get(JenisProduk.class, it.next());
                if (jp == null) continue;
                if (jp.getAkunPendapatan() == null) jenisTanpaPendapatan++;
                if (jp.getAkunHpp() == null) jenisTanpaHpp++;
            }

            Toko toko = tokoId == null ? null : (Toko) session.get(Toko.class, tokoId);
            int tokoTanpaKas = toko != null && toko.getAkunKas() == null ? 1 : 0;
            int tokoTanpaPiutang = toko != null && toko.getAkunPiutang() == null ? 1 : 0;
            int tokoDipetakan = 0;

            List<Long> penyediaId = penyediaBelumPosting(session.connection(), tokoId, mulai, sampai);
            int penyediaTanpaUtang = 0;
            int penyediaDipetakan = 0;
            for (int i = 0; i < penyediaId.size(); i++) {
                Penyedia py = (Penyedia) session.get(Penyedia.class, penyediaId.get(i));
                if (py != null && py.getAkunUtang() == null) penyediaTanpaUtang++;
            }

            if (terapkan) {
                tx = session.beginTransaction();
                if (toko == null) {
                    throw new IllegalArgumentException("Toko id " + tokoId + " tidak ditemukan.");
                }
                if (timpa || toko.getAkunKas() == null) {
                    toko.setAkunKas(kas);
                    tokoDipetakan++;
                }
                if (timpa || toko.getAkunPiutang() == null) {
                    toko.setAkunPiutang(piutang);
                    tokoDipetakan++;
                }
                session.saveOrUpdate(toko);

                for (int i = 0; i < produk.size(); i++) {
                    Produk pr = produk.get(i);
                    MasterAsset ma = pr.getMasterAsset();
                    if (ma == null) {
                        String kode = pr.getKode() == null ? "" : pr.getKode().trim();
                        if (!kode.isEmpty()) {
                            ma = (MasterAsset) session.createCriteria(MasterAsset.class)
                                    .add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
                        }
                        if (ma == null) {
                            ma = new MasterAsset();
                            ma.setKode(kode.isEmpty() ? null : kode);
                            ma.setNama(pr.getNama() == null ? "Produk POS" : pr.getNama());
                            session.save(ma);
                            masterDibuat++;
                        }
                        pr.setMasterAsset(ma);
                        session.saveOrUpdate(pr);
                    }
                    if (timpa || !formulaPunyaAkun(ma.akunTransaksiEfektif())) {
                        ma.setAkunTransaksi(formulaAkun(persediaan));
                        session.saveOrUpdate(ma);
                        masterDipetakan++;
                    }
                }

                for (java.util.Iterator<Long> it = jenisId.iterator(); it.hasNext();) {
                    JenisProduk jp = (JenisProduk) session.get(JenisProduk.class, it.next());
                    if (jp == null) continue;
                    boolean berubah = false;
                    if (timpa || jp.getAkunPendapatan() == null) {
                        jp.setAkunPendapatan(pendapatan);
                        berubah = true;
                    }
                    if (timpa || jp.getAkunHpp() == null) {
                        jp.setAkunHpp(hpp);
                        berubah = true;
                    }
                    if (timpa || jp.getAkunSelisihPersediaan() == null) {
                        jp.setAkunSelisihPersediaan(hpp);
                        berubah = true;
                    }
                    if (berubah) {
                        session.saveOrUpdate(jp);
                        jenisDipetakan++;
                    }
                }

                for (int i = 0; i < penyediaId.size(); i++) {
                    Penyedia py = (Penyedia) session.get(Penyedia.class, penyediaId.get(i));
                    if (py != null && (timpa || py.getAkunUtang() == null)) {
                        py.setAkunUtang(utang);
                        session.saveOrUpdate(py);
                        penyediaDipetakan++;
                    }
                }
                tx.commit();

                // Cadangan untuk produk/supplier baru. Disimpan lewat manager supaya cache konfigurasi
                // ikut berubah dan request posting berikutnya tidak perlu restart server.
                ais.common.KonfigurasiManager.simpanKonfigurasi(
                        ais.action.master.koperasi.helper.AkunKantinUtil.CFG_KAS_TOKO, String.valueOf(kas.getId()));
                ais.common.KonfigurasiManager.simpanKonfigurasi(
                        ais.action.master.koperasi.helper.AkunKantinUtil.CFG_PIUTANG_TOKO, String.valueOf(piutang.getId()));
                ais.common.KonfigurasiManager.simpanKonfigurasi(
                        ais.action.master.koperasi.helper.AkunKantinUtil.CFG_UTANG_SUPPLIER, String.valueOf(utang.getId()));
                ais.common.KonfigurasiManager.simpanKonfigurasi(
                        ais.action.master.koperasi.helper.AkunKantinUtil.CFG_PERSEDIAAN_TOKO, String.valueOf(persediaan.getId()));
                ais.common.KonfigurasiManager.simpanKonfigurasi(
                        ais.action.master.koperasi.helper.AkunKantinUtil.CFG_HPP_TOKO, String.valueOf(hpp.getId()));
                ais.common.KonfigurasiManager.simpanKonfigurasi(
                        ais.action.master.koperasi.helper.AkunKantinUtil.CFG_PENDAPATAN_TOKO, String.valueOf(pendapatan.getId()));
            }

            JSONObject kekurangan = new JSONObject();
            kekurangan.put("tokoTanpaKas", tokoTanpaKas);
            kekurangan.put("tokoTanpaPiutang", tokoTanpaPiutang);
            kekurangan.put("produkTanpaMasterAset", produkTanpaMaster);
            kekurangan.put("produkTanpaAkunPersediaan", produkTanpaPersediaan);
            kekurangan.put("jenisProdukTanpaPendapatan", jenisTanpaPendapatan);
            kekurangan.put("jenisProdukTanpaHpp", jenisTanpaHpp);
            kekurangan.put("penyediaTanpaUtang", penyediaTanpaUtang);
            JSONObject perubahan = new JSONObject();
            perubahan.put("relasiToko", tokoDipetakan);
            perubahan.put("masterAsetDibuat", masterDibuat);
            perubahan.put("masterAsetDipetakan", masterDipetakan);
            perubahan.put("jenisProdukDipetakan", jenisDipetakan);
            perubahan.put("penyediaDipetakan", penyediaDipetakan);
            perubahan.put("konfigurasiCadangan", terapkan ? 6 : 0);

            hasil.put("status", "00");
            hasil.put("mode", terapkan ? "TERAPKAN" : "AUDIT");
            hasil.put("sumberAkun", "cetak_data_260904124814.xlsx");
            hasil.put("tokoId", tokoId == null ? JSONObject.NULL : tokoId);
            hasil.put("mulai", mulai.isEmpty() ? JSONObject.NULL : mulai);
            hasil.put("sampai", sampai.isEmpty() ? JSONObject.NULL : sampai);
            hasil.put("produkBelumPosting", produk.size());
            hasil.put("jenisProdukTerkait", jenisId.size());
            hasil.put("penyediaTerkait", penyediaId.size());
            hasil.put("akun", akun);
            hasil.put("kekuranganSebelum", kekurangan);
            hasil.put("perubahan", perubahan);
            hasil.put("message", terapkan
                    ? "Pemetaan akun Kantin diterapkan secara idempoten. Jalankan audit ulang sebelum posting."
                    : "Audit pemetaan akun Kantin selesai; belum ada data yang diubah.");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try { tx.rollback(); } catch (Exception rollback) {
                    ais.common.ErrorAuditUtil.record(rollback, "PemetaanAkunHelper.kantin rollback");
                }
            }
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static Akun akunKantinDariKode(Session session, String kode, String peran) throws Exception {
        PreparedStatement ps = session.connection().prepareStatement(
                "SELECT a.id, NOT EXISTS (SELECT 1 FROM akunting.akun b WHERE b.parent=a.id)"
                        + " FROM akunting.akun a WHERE REPLACE(TRIM(a.kode),',','.')=? ORDER BY a.id DESC LIMIT 1");
        ps.setString(1, kode.replace(',', '.'));
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            rs.close(); ps.close();
            throw new IllegalArgumentException("Akun " + peran + " dengan kode " + kode + " tidak ditemukan.");
        }
        long id = rs.getLong(1);
        boolean daun = rs.getBoolean(2);
        rs.close(); ps.close();
        if (!daun) {
            throw new IllegalArgumentException("Akun " + peran + " " + kode
                    + " bukan akun daun dan tidak boleh menampung transaksi.");
        }
        return (Akun) session.get(Akun.class, Long.valueOf(id));
    }

    private static JSONObject relasiAkun(String peran, Akun akun) throws Exception {
        JSONObject j = new JSONObject();
        j.put("peran", peran);
        j.put("id", akun.getId());
        j.put("kode", akun.getKode() == null ? "" : akun.getKode());
        j.put("nama", akun.getNama() == null ? "" : akun.getNama());
        return j;
    }

    private static boolean formulaPunyaAkun(String teks) {
        try {
            JSONArray a = teks == null || teks.trim().isEmpty() ? new JSONArray() : new JSONArray(teks);
            for (int i = 0; i < a.length(); i++) {
                JSONObject j = a.optJSONObject(i);
                if (j != null && !j.isNull("akun") && j.optLong("akun", 0) > 0) return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static String formulaAkun(Akun akun) throws Exception {
        JSONObject j = new JSONObject();
        long key = System.nanoTime();
        if (key < 0) key = -key;
        j.put("key", Long.valueOf(key));
        j.put("akun", akun.getId());
        j.put("satuanKerja", JSONObject.NULL);
        return new JSONArray().put(j).toString();
    }

    private static List<Long> produkBelumPosting(Connection conn, Long tokoId,
            String mulai, String sampai) throws Exception {
        boolean pakaiPeriode = mulai != null && !mulai.isEmpty()
                && sampai != null && !sampai.isEmpty();
        String rentangKulakan = pakaiPeriode
                ? " AND date(pp.waktupengadaan) BETWEEN date(?) AND date(?)" : "";
        String rentangPenjualan = pakaiPeriode
                ? " AND date(pb.waktu) BETWEEN date(?) AND date(?)" : "";
        // Mulai dari tabel transaksi yang belum posting, lalu join ke Produk. Bentuk lama
        // memakai dua subquery IN yang memindai seluruh riwayat untuk setiap produk dan dapat
        // melewati timeout proxy pada tenant besar.
        String sql = "SELECT DISTINCT p.id FROM koperasi.produk p INNER JOIN ("
                + "SELECT pp.produk FROM koperasi.pengadaan_produk pp"
                + " WHERE pp.posting_pembelian IS NULL" + rentangKulakan
                + " UNION SELECT pb.produk FROM koperasi.pembelian pb"
                + " WHERE pb.posting_hpp IS NULL AND pb.aktif=true" + rentangPenjualan
                + ") sumber ON sumber.produk=p.id WHERE COALESCE(p.aktif,true)=true"
                + (tokoId == null ? "" : " AND p.toko=?") + " ORDER BY p.id";
        PreparedStatement ps = conn.prepareStatement(sql);
        int parameter = 1;
        if (pakaiPeriode) {
            ps.setString(parameter++, mulai);
            ps.setString(parameter++, sampai);
            ps.setString(parameter++, mulai);
            ps.setString(parameter++, sampai);
        }
        if (tokoId != null) ps.setLong(parameter++, tokoId.longValue());
        ResultSet rs = ps.executeQuery();
        List<Long> keluar = new ArrayList<Long>();
        while (rs.next()) keluar.add(Long.valueOf(rs.getLong(1)));
        rs.close(); ps.close();
        return keluar;
    }

    private static List<Long> penyediaBelumPosting(Connection conn, Long tokoId,
            String mulai, String sampai) throws Exception {
        boolean pakaiPeriode = mulai != null && !mulai.isEmpty()
                && sampai != null && !sampai.isEmpty();
        String sql = "SELECT DISTINCT f.supplier FROM koperasi.pengadaan_faktur f"
                + " WHERE f.supplier IS NOT NULL"
                + (tokoId == null ? "" : " AND f.toko=?")
                + " AND EXISTS (SELECT 1 FROM koperasi.pengadaan_produk pp"
                + " WHERE pp.faktur_pengadaan=f.id AND pp.posting_pembelian IS NULL"
                + (pakaiPeriode
                        ? " AND date(pp.waktupengadaan) BETWEEN date(?) AND date(?)" : "")
                + ") ORDER BY f.supplier";
        PreparedStatement ps = conn.prepareStatement(sql);
        int parameter = 1;
        if (tokoId != null) ps.setLong(parameter++, tokoId.longValue());
        if (pakaiPeriode) {
            ps.setString(parameter++, mulai);
            ps.setString(parameter++, sampai);
        }
        ResultSet rs = ps.executeQuery();
        List<Long> keluar = new ArrayList<Long>();
        while (rs.next()) keluar.add(Long.valueOf(rs.getLong(1)));
        rs.close(); ps.close();
        return keluar;
    }

    /** Normalisasi nama kelompok utk pembandingan: huruf besar, spasi rapat, ASET disamakan AKTIVA. */
    private static String normal(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim().toUpperCase().replaceAll("\\s+", " ");
        v = v.replace("ASET", "AKTIVA");
        return v;
    }

    /** Jenis laporan dari digit pertama kode akar. */
    private static String jenisDariAkar(String kodeAkar) {
        String k = kodeAkar == null ? "" : kodeAkar.trim();
        if (k.length() == 0) {
            return "Neraca";
        }
        char c = k.charAt(0);
        if (c == '4' || c == '5' || c == '6') {
            return "Rugi Laba";
        }
        return "Neraca";
    }

    private static void jalankan(JSONObject payload, JSONObject hasil, boolean terapkan) throws Exception {
        int batas = payload == null ? 0 : payload.optInt("batasContoh", 0);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Connection conn = session.connection();

            // 1) seluruh bagan akun
            Map<Long, Simpul> peta = new LinkedHashMap<Long, Simpul>();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, COALESCE(kode,''), COALESCE(nama,''), parent FROM akunting.akun ORDER BY kode");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Simpul s = new Simpul();
                s.id = rs.getLong(1);
                s.kode = rs.getString(2);
                s.nama = rs.getString(3);
                long p = rs.getLong(4);
                s.parent = rs.wasNull() ? null : Long.valueOf(p);
                peta.put(Long.valueOf(s.id), s);
            }
            rs.close();
            ps.close();

            // 2) akun yang sudah punya kelompok aktif -> tidak disentuh
            Set<Long> sudah = new HashSet<Long>();
            ps = conn.prepareStatement("SELECT DISTINCT p.akun FROM akunting.kelompok_laporan_punya_akun p"
                    + " JOIN akunting.kelompok_laporan k ON k.id = p.kelompok_laporan"
                    + " WHERE (k.aktif IS NULL OR k.aktif)");
            rs = ps.executeQuery();
            while (rs.next()) {
                sudah.add(Long.valueOf(rs.getLong(1)));
            }
            rs.close();
            ps.close();

            // 3) kelompok yang sudah ada, per jenis laporan
            Map<String, Long> kelompokAda = new HashMap<String, Long>();   // "JENIS|NAMA" -> id
            Map<String, Long> jenisAda = new HashMap<String, Long>();      // "NERACA"/"RUGI LABA" -> id
            ps = conn.prepareStatement("SELECT id, COALESCE(keterangan,'') FROM akunting.jenis_laporan");
            rs = ps.executeQuery();
            while (rs.next()) {
                String ket = normal(rs.getString(2));
                if (ket.indexOf("NERACA") >= 0) {
                    jenisAda.put("NERACA", Long.valueOf(rs.getLong(1)));
                } else if (ket.indexOf("RUGI") >= 0 || ket.indexOf("LABA") >= 0) {
                    jenisAda.put("RUGI LABA", Long.valueOf(rs.getLong(1)));
                }
            }
            rs.close();
            ps.close();
            ps = conn.prepareStatement("SELECT k.id, COALESCE(NULLIF(TRIM(COALESCE(k.keterangan,'')),''),"
                    + " COALESCE(m.keterangan,'')), COALESCE(j.keterangan,'')"
                    + " FROM akunting.kelompok_laporan k"
                    + " LEFT JOIN akunting.master_grup_laporan m ON m.id = k.master_grup_laporan"
                    + " LEFT JOIN akunting.jenis_laporan j ON j.id = k.jenis_laporan"
                    + " WHERE (k.aktif IS NULL OR k.aktif)");
            rs = ps.executeQuery();
            while (rs.next()) {
                String nama = normal(rs.getString(2));
                String jenis = normal(rs.getString(3)).indexOf("NERACA") >= 0 ? "NERACA" : "RUGI LABA";
                if (nama.length() == 0) {
                    continue;
                }
                String kunci = jenis + "|" + nama;
                if (!kelompokAda.containsKey(kunci)) {
                    kelompokAda.put(kunci, Long.valueOf(rs.getLong(1)));
                }
            }
            rs.close();
            ps.close();

            // 3b) MasterGrupLaporan yang sudah ada (nama dinormalkan) -- HANYA dipakai utk mencocokkan,
            // tidak pernah dibuat baru di sini (lihat Javadoc kelas: "hanya menambah, tidak menebak").
            Map<String, Long> grupAda = new HashMap<String, Long>();
            ps = conn.prepareStatement("SELECT id, COALESCE(nama,'') FROM akunting.master_grup_laporan");
            rs = ps.executeQuery();
            while (rs.next()) {
                String nama = normal(rs.getString(2));
                if (nama.length() > 0 && !grupAda.containsKey(nama)) {
                    grupAda.put(nama, Long.valueOf(rs.getLong(1)));
                }
            }
            rs.close();
            ps.close();

            // 4) susun usulan
            List<Usul> usulan = new ArrayList<Usul>();
            for (Map.Entry<Long, Simpul> e : peta.entrySet()) {
                Simpul akun = e.getValue();
                if (sudah.contains(Long.valueOf(akun.id))) {
                    continue;
                }
                List<Simpul> rantai = new ArrayList<Simpul>();
                Simpul kursor = akun;
                int aman = 0;
                while (kursor != null && aman++ < 20) {
                    rantai.add(0, kursor);
                    kursor = kursor.parent == null ? null : peta.get(kursor.parent);
                }
                if (rantai.isEmpty()) {
                    continue;
                }
                Simpul akar = rantai.get(0);
                int idx = rantai.size() > 2 ? 2 : (rantai.size() - 1);
                Simpul grup = rantai.get(idx);
                // Akun induk itu sendiri dikelompokkan pada jenjang di atasnya supaya tidak jadi
                // kelompok yang isinya hanya dirinya sendiri.
                if (grup.id == akun.id && rantai.size() > 1) {
                    grup = rantai.get(rantai.size() - 2);
                }
                Usul u = new Usul();
                u.akun = akun;
                u.akar = akar;
                u.jenis = jenisDariAkar(akar.kode);
                u.kelompok = grup.nama == null || grup.nama.trim().length() == 0 ? akar.nama : grup.nama.trim();
                u.kodeKelompok = grup.kode;
                usulan.add(u);
            }

            // 5) ringkasan per kelompok
            Map<String, int[]> ringkas = new LinkedHashMap<String, int[]>();   // "jenis|nama" -> {jumlah, kelompokBaru}
            for (int i = 0; i < usulan.size(); i++) {
                Usul u = usulan.get(i);
                String kunci = u.jenis + "|" + u.kelompok;
                int[] v = ringkas.get(kunci);
                if (v == null) {
                    boolean ada = kelompokAda.containsKey(normal(u.jenis) + "|" + normal(u.kelompok));
                    v = new int[] { 0, ada ? 0 : 1 };
                    ringkas.put(kunci, v);
                }
                v[0]++;
            }

            JSONArray arrRingkas = new JSONArray();
            for (Map.Entry<String, int[]> e : ringkas.entrySet()) {
                String[] bagi = e.getKey().split("\\|", 2);
                JSONObject j = new JSONObject();
                j.put("jenis", bagi[0]);
                j.put("kelompok", bagi.length > 1 ? bagi[1] : "");
                j.put("jumlahAkun", e.getValue()[0]);
                j.put("kelompokBaru", e.getValue()[1] == 1);
                arrRingkas.put(j);
            }

            JSONArray arrContoh = new JSONArray();
            int maxContoh = batas > 0 ? batas : usulan.size();
            for (int i = 0; i < usulan.size() && i < maxContoh; i++) {
                Usul u = usulan.get(i);
                JSONObject j = new JSONObject();
                j.put("kode", u.akun.kode);
                j.put("nama", u.akun.nama);
                j.put("jenis", u.jenis);
                j.put("kelompok", u.kelompok);
                j.put("kodeKelompok", u.kodeKelompok);
                arrContoh.put(j);
            }

            hasil.put("status", "00");
            hasil.put("jumlahBelumDipetakan", usulan.size());
            hasil.put("jumlahKelompok", ringkas.size());
            hasil.put("ringkasan", arrRingkas);
            hasil.put("usulan", arrContoh);

            if (!terapkan) {
                hasil.put("message", usulan.isEmpty()
                        ? "Semua akun sudah dipetakan."
                        : "Pratinjau: " + usulan.size() + " akun akan dipetakan ke " + ringkas.size() + " kelompok.");
                return;
            }

            // Instalasi lama dapat memiliki bagan akun lengkap tetapi tabel Jenis Laporan kosong.
            // Dalam kondisi itu seluruh usulan di atas sebelumnya berakhir sebagai "dilewati" dan
            // Laba Rugi tetap hanya berisi pesan kosong. Karena aksi tulis ini sudah digerbangi hak
            // admin, bootstrap dua jenis fundamental secara idempoten. Nama dan keterangan sengaja
            // sama agar kedua konsumen lama (satu membaca nama, satu membaca keterangan) sepakat.
            int jenisLaporanBaru = 0;
            String[][] jenisStandar = new String[][] {
                    { "NERACA", "Neraca" },
                    { "RUGI LABA", "Rugi Laba" }
            };
            for (int i = 0; i < jenisStandar.length; i++) {
                String kunciJenis = jenisStandar[i][0];
                if (jenisAda.containsKey(kunciJenis)) {
                    continue;
                }
                session.beginTransaction();
                JenisLaporan jl = new JenisLaporan();
                jl.setNama(jenisStandar[i][1]);
                jl.setKeterangan(jenisStandar[i][1]);
                jl.setTampilDiDashboard(Boolean.TRUE);
                session.save(jl);
                session.getTransaction().commit();
                jenisAda.put(kunciJenis, jl.getId());
                jenisLaporanBaru++;
            }

            // 6) terapkan
            int dibuatKelompok = 0;
            int dipetakan = 0;
            JSONArray masalah = new JSONArray();
            Map<String, Long> cache = new HashMap<String, Long>(kelompokAda);
            for (int i = 0; i < usulan.size(); i++) {
                Usul u = usulan.get(i);
                String kunciNorm = normal(u.jenis) + "|" + normal(u.kelompok);
                try {
                    Long idKelompok = cache.get(kunciNorm);
                    if (idKelompok == null) {
                        Long idJenis = jenisAda.get(normal(u.jenis).indexOf("NERACA") >= 0 ? "NERACA" : "RUGI LABA");
                        if (idJenis == null) {
                            masalah.put("Jenis Laporan '" + u.jenis + "' belum ada di master; akun " + u.akun.kode + " dilewati.");
                            continue;
                        }
                        session.beginTransaction();
                        KelompokLaporan kl = new KelompokLaporan();
                        kl.setKeterangan(u.kelompok);
                        kl.setJenisLaporan((JenisLaporan) session.load(JenisLaporan.class, idJenis));
                        kl.setAktif(Boolean.TRUE);
                        kl.setUrut(Double.valueOf(urutDariKode(u.kodeKelompok)));
                        Long idGrup = u.akar == null ? null : grupAda.get(normal(u.akar.nama));
                        if (idGrup != null) {
                            kl.setMasterGrupLaporan((MasterGrupLaporan) session.load(MasterGrupLaporan.class, idGrup));
                        }
                        session.save(kl);
                        session.getTransaction().commit();
                        idKelompok = kl.getId();
                        cache.put(kunciNorm, idKelompok);
                        dibuatKelompok++;
                    }
                    session.beginTransaction();
                    KelompokLaporanPunyaAkun pa = new KelompokLaporanPunyaAkun();
                    pa.setAkun((Akun) session.load(Akun.class, Long.valueOf(u.akun.id)));
                    pa.setKelompokLaporan((KelompokLaporan) session.load(KelompokLaporan.class, idKelompok));
                    session.save(pa);
                    session.getTransaction().commit();
                    dipetakan++;
                } catch (Exception ex) {
                    batalkanDiam(session);
                    ais.common.ErrorAuditUtil.record(ex, "auto-audit PemetaanAkunHelper.terapkan " + u.akun.kode);
                    masalah.put("Akun " + u.akun.kode + " " + u.akun.nama + ": " + ex.getMessage());
                }
            }
            hasil.put("dipetakan", dipetakan);
            hasil.put("jenisLaporanBaru", jenisLaporanBaru);
            hasil.put("kelompokBaru", dibuatKelompok);
            hasil.put("masalah", masalah);
            hasil.put("message", dipetakan + " akun dipetakan (" + dibuatKelompok + " kelompok baru dibuat)"
                    + (masalah.length() > 0 ? ", " + masalah.length() + " baris gagal." : "."));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /** Urutan tampil kelompok mengikuti kode akun induknya (mis. 512.000 -> 512) agar urut seperti bagan akun. */
    private static double urutDariKode(String kode) {
        if (kode == null) {
            return 9999;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kode.length(); i++) {
            char c = kode.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c == '.' || c == '-') {
                break;
            }
        }
        try {
            return sb.length() == 0 ? 9999 : Double.parseDouble(sb.toString());
        } catch (Exception e) {
            return 9999;
        }
    }

    private static void batalkanDiam(Session session) {
        try {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PemetaanAkunHelper.batalkanDiam");
        }
    }
}
