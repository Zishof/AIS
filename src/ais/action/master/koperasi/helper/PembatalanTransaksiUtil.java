package ais.action.master.koperasi.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SQLQuery;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pembelian;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembatalanTransaksiKantin;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;

/**
 * <h2>Pembatalan transaksi Kantin/POS yang tercatat.</h2>
 *
 * <p>Satu-satunya pintu untuk membatalkan transaksi penjualan yang sudah tersimpan. Alur kerjanya:
 * <b>potret dulu, baru hapus</b> — seluruh isi transaksi (header + rincian item) disalin ke
 * {@link PembatalanTransaksiKantin} bersama alasan pembatalan, baru barisnya dihapus seperti
 * perilaku lama.</p>
 *
 * <p><b>Kenapa tetap menghapus, bukan menandai.</b> Penjelasan lengkap ada di JavaDoc
 * {@link PembatalanTransaksiKantin}. Ringkasnya: {@code koperasi.pembelian} dibaca di sekitar 120
 * tempat dan hanya 34 yang menyaring kolom {@code aktif}; membiarkan baris yang dibatalkan tetap
 * ada akan membuat saldo siswa/anggota salah, stok tidak pulih (stok di modul ini dihitung ulang
 * dari riwayat penjualan, bukan buku besar mutasi), dan gerbang anti-oversell memblokir penjualan
 * ulang. Dengan tetap menghapus, seluruh laporan/stok/saldo berperilaku persis seperti sebelumnya —
 * tidak ada angka yang bergeser diam-diam — dan yang bertambah hanyalah catatan permanen yang bisa
 * dipertanggungjawabkan.</p>
 *
 * <p><b>Yang BUKAN pembatalan.</b> Sengaja tidak dipakai oleh penghapusan teknis internal seperti
 * hapus-lalu-tulis-ulang rincian saat checkout disimpan ulang ({@code KantinHelper}) atau
 * pemrosesan ulang topup ({@code TopupHelper}) — itu bukan tindakan membatalkan transaksi, dan
 * mencatatnya di sini justru akan mengotori arsip sehingga laporan ke pimpinan jadi tidak bermakna.</p>
 */
public final class PembatalanTransaksiUtil {

    private PembatalanTransaksiUtil() {
    }

    /** Panjang maksimal teks rincian yang dipotret, sebagai pengaman terhadap keranjang raksasa. */
    private static final int MAKS_PANJANG_RINCIAN = 20000;

    /**
     * Catat pembatalan lalu hapus transaksinya.
     *
     * <p>Pemanggil bertanggung jawab atas transaksi basis data (commit/rollback) — method ini hanya
     * melakukan {@code save} arsip dan {@code delete} transaksi pada {@link Session} yang diberikan,
     * SENGAJA dalam satu sesi yang sama supaya keduanya berada dalam satu transaksi: mustahil
     * terjadi keadaan "transaksi terhapus tapi arsipnya gagal tersimpan".</p>
     *
     * @param session sesi Hibernate aktif milik pemanggil (harus dalam transaksi)
     * @param trx     transaksi yang akan dibatalkan (tidak boleh null, harus sudah tersimpan)
     * @param alasan  alasan pembatalan; wajib diisi — inilah inti seluruh pencatatan ini
     * @throws IllegalArgumentException bila transaksi belum tersimpan atau alasan kosong
     */
    public static void batalkan(Session session, PembelianAnggotaKoperasi trx, String alasan) {
        if (trx == null || trx.getId() == null) {
            throw new IllegalArgumentException("Transaksi yang akan dibatalkan tidak valid atau belum tersimpan.");
        }
        if (alasan == null || alasan.trim().length() == 0) {
            throw new IllegalArgumentException("Alasan pembatalan wajib diisi.");
        }

        PembatalanTransaksiKantin arsip = new PembatalanTransaksiKantin();
        arsip.setPembelianAnggotaKoperasiId(trx.getId());
        arsip.setKode(potong(trx.getKode(), 100));
        arsip.setToko(trx.getToko());
        arsip.setTotalBiaya(trx.getTotalBiaya());
        arsip.setTanggalTransaksi(trx.getTanggalPembayaran());
        arsip.setNamaKasir(potong(trx.getOleh(), 255));
        arsip.setSudahDiposting(Boolean.valueOf(trx.getPostingHistory() != null));

        try {
            arsip.setTotalDiskon(trx.getTotalDiskon());
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PembatalanTransaksiUtil.totalDiskon");
        }

        AnggotaKoperasi anggota = trx.getAnggotaKoperasi();
        if (anggota != null) {
            arsip.setAnggotaKoperasi(anggota);
            arsip.setNamaAnggota(potong(anggota.getNama(), 255));
        } else {
            arsip.setNamaAnggota("Umum");
        }

        try {
            if (trx.getCaraPembayaranKoperasi() != null) {
                arsip.setCaraPembayaran(potong(trx.getCaraPembayaranKoperasi().getNama(), 255));
            }
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PembatalanTransaksiUtil.caraBayar");
        }

        arsip.setRincian(potret(session, trx));
        arsip.setAlasan(alasan.trim());
        arsip.setTanggalDibatalkan(ais.ui.util.WaktuUtil.getDate());

        try {
            Tbmuser user = Common.getCurrentUser();
            if (user != null) {
                arsip.setDibatalkanOleh(potong(user.getNama(), 255));
                arsip.setDibatalkanOlehId(potong(String.valueOf(user.getId()), 100));
            }
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PembatalanTransaksiUtil.currentUser");
        }

        // Arsip DULU, baru hapus -- urutan ini penting. Bila penyimpanan arsip gagal, exception-nya
        // membatalkan seluruh transaksi basis data dan data aslinya tetap utuh. Header transaksi
        // dilepas dari session sebelum native delete agar Hibernate tidak mencoba auto-flush entity
        // managed yang sama dan memunculkan error "identifier was altered".
        Long idTransaksi = trx.getId();
        session.save(arsip);
        session.flush();
        try {
            session.evict(trx);
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PembatalanTransaksiUtil.evictTrx");
        }

        SQLQuery qBahan = session.createSQLQuery("delete from koperasi.pemakaian_bahan_baku where pembelian_anggota_koperasi = :id");
        qBahan.setLong("id", idTransaksi);
        qBahan.executeUpdate();

        PembelianReferenceCleanupUtil.lepasDraftPembelianLunasUntukHeader(session, idTransaksi);

        SQLQuery qRinci = session.createSQLQuery("delete from koperasi.pembelian where pembelian_anggota_koperasi = :id");
        qRinci.setLong("id", idTransaksi);
        qRinci.executeUpdate();

        SQLQuery qHeader = session.createSQLQuery("delete from koperasi.pembelian_anggota_koperasi where id = :id");
        qHeader.setLong("id", idTransaksi);
        qHeader.executeUpdate();
    }

    /**
     * Potret isi keranjang jadi teks siap baca, satu baris per item. Dibaca dari
     * {@code koperasi.pembelian} SEBELUM transaksinya dihapus. Sengaja teks, bukan tabel rincian
     * tersendiri — kebutuhannya adalah bukti yang tetap terbaca utuh walaupun produknya kelak
     * dihapus atau diganti nama.
     */
    @SuppressWarnings("unchecked")
    private static String potret(Session session, PembelianAnggotaKoperasi trx) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Pembelian> items = session.createCriteria(Pembelian.class)
                    .add(org.hibernate.criterion.Restrictions.eq("pembelianAnggotaKoperasi", trx))
                    .addOrder(org.hibernate.criterion.Order.asc("id")).list();
            for (Pembelian p : items) {
                if (sb.length() > MAKS_PANJANG_RINCIAN) {
                    sb.append("... (rincian dipotong karena terlalu panjang)");
                    break;
                }
                String namaProduk = "-";
                try {
                    if (p.getProduk() != null && p.getProduk().getNama() != null) {
                        namaProduk = p.getProduk().getNama();
                    } else if (p.getNama() != null) {
                        namaProduk = p.getNama();
                    }
                } catch (Exception ignore) {
                    ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PembatalanTransaksiUtil.namaProduk");
                }
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(angka(p.getQty())).append(" x ").append(namaProduk)
                        .append(" @ ").append(angka(p.getHargaJual()))
                        .append(" = ").append(angka(p.getTotal()));
                if (p.getDiskon() != null && p.getDiskon().doubleValue() > 0) {
                    sb.append(" (diskon ").append(angka(p.getDiskon())).append(")");
                }
            }
        } catch (Exception e) {
            // Potret rincian bersifat pelengkap: kegagalan di sini TIDAK boleh menggagalkan
            // pembatalan itu sendiri, tapi tetap harus terlihat jejaknya di arsip.
            ais.common.ErrorAuditUtil.record(e, "PembatalanTransaksiUtil.potret");
            return "(rincian tidak dapat dibaca saat pembatalan)";
        }
        return sb.length() == 0 ? "(tidak ada rincian item)" : sb.toString();
    }

    private static String angka(Double d) {
        double v = d == null ? 0 : d.doubleValue();
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static String potong(String s, int maks) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= maks ? t : t.substring(0, maks);
    }

    // ================================================================= jalur API dasbor draft jurnal

    /**
     * Kriteria dokumen yang SAMA dengan baris "Pembatalan Penjualan Kantin" di dasbor draft
     * jurnal: arsip pembatalan yang transaksi aslinya SUDAH terposting batch Penjualan Kantin
     * ({@code sudah_diposting=true}), bernilai, pada rentang tanggal DIBATALKAN. Pembatalan atas
     * transaksi yang belum terposting tidak butuh jurnal balik (headernya keluar dari kriteria
     * batch sebelum pernah dijurnal) sehingga tidak dihitung.
     */
    private static org.hibernate.Criteria kriteriaPembatalanStatic(Session session,
            java.util.Date mulai, java.util.Date sampai) {
        org.hibernate.Criteria c = session.createCriteria(PembatalanTransaksiKantin.class)
                .add(org.hibernate.criterion.Restrictions.eq("sudahDiposting", Boolean.TRUE))
                .add(org.hibernate.criterion.Restrictions.ne("totalBiaya", 0.0))
                .add(org.hibernate.criterion.Restrictions.isNotNull("totalBiaya"));
        if (mulai != null && sampai != null) {
            c.add(org.hibernate.criterion.Restrictions.sqlRestriction(
                    "date(this_.tanggal_dibatalkan) between date('"
                            + Common.databaseDateFormat.get().format(mulai) + "') and date('"
                            + Common.databaseDateFormat.get().format(sampai) + "')"));
        }
        return c;
    }

    /** Akun pendapatan tujuan jurnal balik, dari konfigurasi {@code akun_pendapatan_pembatalan_kantin_id}. */
    private static ais.database.model.akunting.Akun akunPendapatanBalik() {
        try {
            String id = Common.getKonfigurasi("akun_pendapatan_pembatalan_kantin_id", "").getNilai();
            if (id != null && !id.trim().isEmpty()) {
                return (ais.database.model.akunting.Akun) ais.common.ConstantValues.ambil(
                        ais.database.model.akunting.Akun.class.getName(), Long.parseLong(id.trim()));
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PembatalanTransaksiUtil.akunPendapatanBalik");
        }
        return null;
    }

    /**
     * Posting SEMUA jurnal balik pembatalan pada rentang -- jalur API dasbor Draft Jurnal POS
     * (dok 61 butir C). Jurnal per arsip: Dr akun pendapatan (konfigurasi
     * {@code akun_pendapatan_pembatalan_kantin_id}) / Cr akun kas dari CARA PEMBAYARAN yang
     * terekam arsip (dicari per nama), senilai {@code totalBiaya}, bertanggal DIBATALKAN --
     * kebalikan agregat dari porsi header pada batch Penjualan Kantin yang terlanjur terposting.
     *
     * <p>Batasan yang disengaja (arsip pembatalan hanya menyimpan potret teks): rincian per
     * jenis produk, PPN, dan sisi HPP TIDAK direkonstruksi -- pendapatan dibalik agregat ke satu
     * akun konfigurasi; koreksi HPP barang batal dilakukan lewat batal-mundur + posting ulang
     * periode HPP berjalan atau jurnal penyesuaian. Konfigurasi belum diisi = semua arsip
     * dilewati (tercatat di Error Log dan angka "dilewati" dasbor).</p>
     */
    public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
            java.util.Date tglPosting) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaPembatalanStatic(session, mulai, sampai)
                    .add(org.hibernate.criterion.Restrictions.isNull("postingHistory")).list();
            if (daftar.isEmpty()) {
                return 0;
            }
            ais.database.model.akunting.Akun akunPendapatan = akunPendapatanBalik();
            if (akunPendapatan == null) {
                ais.common.ErrorAuditUtil.record(new IllegalStateException(
                        "Konfigurasi akun_pendapatan_pembatalan_kantin_id belum diisi -- jurnal"
                                + " balik pembatalan kantin tidak dapat diposting."),
                        "PembatalanTransaksiUtil jalur API");
                return 0;
            }

            ais.database.model.akunting.PostingHistory postingHistory =
                    new ais.database.model.akunting.PostingHistory(
                            ais.database.model.akunting.PostingHistory.JENIS_PEMBATALAN_KANTIN);
            postingHistory.setTbmuser(oleh);
            postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
            postingHistory.setTanggalPosting(tglPosting == null ? new java.util.Date() : tglPosting);
            postingHistory.setPosting(true);
            postingHistory.setKeterangan("Posting massal jurnal balik pembatalan kantin dari dasbor jurnal"
                    + (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
                            + " s.d " + Common.dateFormat.get().format(sampai) : ""));
            session.getTransaction().begin();
            session.save(postingHistory);
            session.getTransaction().commit();

            for (Object o : daftar) {
                PembatalanTransaksiKantin arsip = (PembatalanTransaksiKantin) o;
                if (arsip == null) {
                    continue;
                }
                try {
                    ais.database.model.akunting.Akun akunKas = null;
                    if (arsip.getCaraPembayaran() != null && !arsip.getCaraPembayaran().trim().isEmpty()) {
                        ais.database.model.koperasi.CaraPembayaranKoperasi cara =
                                (ais.database.model.koperasi.CaraPembayaranKoperasi) session
                                        .createCriteria(
                                                ais.database.model.koperasi.CaraPembayaranKoperasi.class)
                                        .add(org.hibernate.criterion.Restrictions.eq("nama",
                                                arsip.getCaraPembayaran().trim()))
                                        .setMaxResults(1).uniqueResult();
                        akunKas = cara == null ? null : cara.getAkun();
                    }
                    Double nilai = arsip.getTotalBiaya();
                    if (akunKas == null || nilai == null || nilai == 0.0) {
                        // Cara pembayaran arsip tidak dikenal/ber-akun: dilewati, tetap draf.
                        continue;
                    }

                    String ket = "Jurnal balik pembatalan transaksi kantin \"" + arsip.getKode()
                            + "\" (" + arsip.getNamaAnggota() + ") senilai "
                            + Common.numberFormat.get().format(nilai)
                            + "; alasan: " + arsip.getAlasan();

                    boolean tersimpan;
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    tersimpan = ais.action.master.akunting.util.CommonAkunting.saveTransaksi(
                            new ais.database.model.akunting.Akun[] { akunPendapatan },
                            new ais.database.model.akunting.Akun[] { akunKas }, null, null,
                            postingHistory, true, ket, arsip.getTanggalDibatalkan(),
                            new Double[] { nilai }, new Double[] { nilai }, 0.0, arsip, null, session);
                    if (tersimpan) {
                        arsip.setPostingHistory(postingHistory);
                        session.update(arsip);
                        session.getTransaction().commit();
                        n++;
                    } else {
                        session.getTransaction().rollback();
                    }
                } catch (Exception e) {
                    try {
                        session.getTransaction().rollback();
                    } catch (Exception ex) {
                        // rollback gagal: kegagalan aslinya yang dilaporkan
                    }
                    ais.common.ErrorAuditUtil.record(e, "PembatalanTransaksiUtil jalur API");
                }
            }

            if (n == 0) {
                // Tidak satu arsip pun terjurnal: riwayat kosong tidak ditinggalkan.
                try {
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    session.delete(postingHistory);
                    session.getTransaction().commit();
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "PembatalanTransaksiUtil jalur API");
                }
            }
        } finally {
            try {
                session.disconnect();
                ais.database.hibernate.HibernateUtil.closeSession();
            } catch (Exception e) {
                // penutupan sesi manual: kegagalannya tidak menutupi hasil posting
            }
        }
        return n;
    }

    /**
     * Membatalkan posting SEMUA jurnal balik pada rentang: jurnal turunannya dihapus (baris
     * transaksi dulu, lalu grupnya -- hanya yang belum closing), lalu penandanya dilepas.
     */
    public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaPembatalanStatic(session, mulai, sampai)
                    .add(org.hibernate.criterion.Restrictions.isNotNull("postingHistory")).list();
            for (Object o : daftar) {
                PembatalanTransaksiKantin arsip = (PembatalanTransaksiKantin) o;
                if (arsip == null) {
                    continue;
                }
                try {
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
                            + " (select id from akunting.grup_transaksi where pembatalan_transaksi="
                            + arsip.getId() + " and closing is null)").executeUpdate();
                    session.createSQLQuery(
                            "delete from akunting.grup_transaksi where pembatalan_transaksi="
                                    + arsip.getId() + " and closing is null").executeUpdate();
                    arsip.setPostingHistory(null);
                    session.update(arsip);
                    session.getTransaction().commit();
                    n++;
                } catch (Exception e) {
                    try {
                        session.getTransaction().rollback();
                    } catch (Exception ex) {
                        // rollback gagal: kegagalan aslinya yang dilaporkan
                    }
                    ais.common.ErrorAuditUtil.record(e, "PembatalanTransaksiUtil jalur API");
                }
            }
        } finally {
            try {
                session.disconnect();
                ais.database.hibernate.HibernateUtil.closeSession();
            } catch (Exception e) {
                // penutupan sesi manual: kegagalannya tidak menutupi hasil pembatalan
            }
        }
        return n;
    }

}
