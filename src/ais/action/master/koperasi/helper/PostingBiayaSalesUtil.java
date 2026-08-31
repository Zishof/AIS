package ais.action.master.koperasi.helper;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.koperasi.NotaSalesBiaya;

/**
 * <h2>Posting BIAYA SESI SALES lapangan ke buku besar (dok 61 butir E).</h2>
 *
 * <p>Dok 61 semula menduga modul Inventory &amp; Sales memakai "buku terpisah". Penelusuran ulang
 * menunjukkan dugaan itu keliru: layar <i>Kas &amp; Jurnal</i>-nya membaca langsung
 * {@code akunting.transaksi} dan master akunnya adalah {@code akunting.akun} yang sama — bukan
 * bagan akun tandingan. Pembelian sesi pun sekadar TAUTAN ke faktur kulakan yang sudah punya jalur
 * posting sendiri, dan {@link ais.database.model.koperasi.NotaSalesKas} adalah catatan laci kas
 * sesi (kontrol operasional, sejenis sesi kas kasir yang memang tidak dijurnal per sesi).</p>
 *
 * <p>Yang benar-benar tersisa hanyalah <b>biaya sesi sales</b>: pengeluaran nyata di lapangan
 * (bensin, tol, konsumsi, dan sebagainya) yang tidak pernah menyentuh buku besar sama sekali,
 * sehingga beban operasional Laba Rugi lebih kecil daripada yang sesungguhnya terjadi.</p>
 *
 * <p><b>Sumber akun.</b> Sisi BEBAN diambil dari akun pada master {@code KategoriBiayaSales} —
 * satu akun per kategori, supaya rincian beban di buku besar sedetail kategori yang sudah dipakai
 * lapangan. Sisi KAS diambil dari Konfigurasi {@link #KONF_AKUN_KAS_SALES} karena dokumen biaya
 * hanya menyimpan metode pembayaran berupa teks, bukan rujukan akun. Kategori yang belum ber-akun
 * dilewati mesin tetapi tetap terhitung draf sehingga kekurangan setup terlihat di dasbor.</p>
 */
public final class PostingBiayaSalesUtil {

    private PostingBiayaSalesUtil() {
    }

    /** Kunci Konfigurasi akun KAS pemegang uang sesi sales (sisi kredit biaya lapangan). */
    public static final String KONF_AKUN_KAS_SALES = "akun_kas_sesi_sales_id";

    /**
     * Kriteria biaya sesi sales yang layak dijurnal: dokumen masih AKTIF (bukan hasil reversal)
     * dan bernilai. Sama persis dengan yang dipakai dasbor Draft Jurnal.
     */
    public static org.hibernate.Criteria kriteriaBiayaStatic(Session session, Date mulai, Date sampai) {
        org.hibernate.Criteria c = session.createCriteria(NotaSalesBiaya.class)
                .add(Restrictions.or(Restrictions.isNull("statusDok"),
                        Restrictions.eq("statusDok", NotaSalesBiaya.DOK_AKTIF)))
                .add(Restrictions.isNotNull("nilai"))
                .add(Restrictions.ne("nilai", java.math.BigDecimal.ZERO));
        if (mulai != null && sampai != null) {
            c.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
                    + Common.databaseDateFormat.get().format(mulai) + "') and date('"
                    + Common.databaseDateFormat.get().format(sampai) + "')"));
        }
        return c;
    }

    /** Jurnal per dokumen: <b>Dr</b> akun kategori biaya / <b>Cr</b> akun kas sesi sales. */
    public static int postingSemua(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaBiayaStatic(session, mulai, sampai)
                    .add(Restrictions.isNull("postingHistory")).list();
            if (daftar.isEmpty()) {
                return 0;
            }
            Akun akunKas = akunKonfigurasi(KONF_AKUN_KAS_SALES);
            if (akunKas == null) {
                ais.common.ErrorAuditUtil.record(new IllegalStateException(
                        "Konfigurasi " + KONF_AKUN_KAS_SALES + " belum diisi -- jurnal biaya sesi"
                                + " sales tidak dapat diposting."), "PostingBiayaSalesUtil jalur API");
                return 0;
            }

            PostingHistory ph = new PostingHistory(PostingHistory.JENIS_BIAYA_SALES);
            ph.setTbmuser(oleh);
            ph.setTanggal(tglPosting == null ? new Date() : tglPosting);
            ph.setTanggalPosting(tglPosting == null ? new Date() : tglPosting);
            ph.setPosting(true);
            ph.setKeterangan("Posting massal biaya sesi sales dari dasbor jurnal"
                    + (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
                            + " s.d " + Common.dateFormat.get().format(sampai) : ""));
            session.getTransaction().begin();
            session.save(ph);
            session.getTransaction().commit();

            for (Object o : daftar) {
                NotaSalesBiaya biaya = (NotaSalesBiaya) o;
                if (biaya == null) {
                    continue;
                }
                try {
                    Akun akunBeban = biaya.getKategori() == null ? null : biaya.getKategori().getAkun();
                    java.math.BigDecimal nilai = biaya.getNilai();
                    if (akunBeban == null || nilai == null
                            || nilai.compareTo(java.math.BigDecimal.ZERO) == 0) {
                        // Kategori belum ber-akun: dilewati, dokumen tetap draf.
                        continue;
                    }
                    Double angka = Double.valueOf(nilai.doubleValue());
                    String ket = "Biaya sesi sales "
                            + (biaya.getKategori() == null ? "-" : biaya.getKategori().getNama())
                            + " senilai " + Common.numberFormat.get().format(angka)
                            + (biaya.getUraian() == null ? "" : "; " + biaya.getUraian());

                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    boolean tersimpan = ais.action.master.akunting.util.CommonAkunting.saveTransaksi(
                            new Akun[] { akunBeban }, new Akun[] { akunKas }, null, null, ph, true,
                            ket, biaya.getTanggal(), new Double[] { angka }, new Double[] { angka },
                            0.0, biaya, null, session);
                    if (tersimpan) {
                        biaya.setPostingHistory(ph);
                        session.update(biaya);
                        session.getTransaction().commit();
                        n++;
                    } else {
                        session.getTransaction().rollback();
                    }
                } catch (Exception e) {
                    balikkan(session);
                    ais.common.ErrorAuditUtil.record(e, "PostingBiayaSalesUtil jalur API");
                }
            }

            if (n == 0) {
                try {
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    session.delete(ph);
                    session.getTransaction().commit();
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "PostingBiayaSalesUtil hapus riwayat kosong");
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PostingBiayaSalesUtil.postingSemua");
        } finally {
            tutup(session);
        }
        return n;
    }

    public static int batalkanPostingSemua(Date mulai, Date sampai) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaBiayaStatic(session, mulai, sampai)
                    .add(Restrictions.isNotNull("postingHistory")).list();
            for (Object o : daftar) {
                NotaSalesBiaya biaya = (NotaSalesBiaya) o;
                if (biaya == null) {
                    continue;
                }
                try {
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in ("
                            + " select id from akunting.grup_transaksi where nota_sales_biaya = "
                            + biaya.getId() + " and closing is null )").executeUpdate();
                    session.createSQLQuery("delete from akunting.grup_transaksi where nota_sales_biaya = "
                            + biaya.getId() + " and closing is null").executeUpdate();
                    biaya.setPostingHistory(null);
                    session.update(biaya);
                    session.getTransaction().commit();
                    n++;
                } catch (Exception e) {
                    balikkan(session);
                    ais.common.ErrorAuditUtil.record(e, "PostingBiayaSalesUtil batal jalur API");
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PostingBiayaSalesUtil.batalkanPostingSemua");
        } finally {
            tutup(session);
        }
        return n;
    }

    private static Akun akunKonfigurasi(String kunci) {
        try {
            String nilai = Common.getKonfigurasi(kunci, "").getNilai();
            if (nilai == null || nilai.trim().length() == 0) {
                return null;
            }
            return (Akun) ais.common.ConstantValues.ambil(Akun.class.getName(),
                    Long.parseLong(nilai.trim()));
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit PostingBiayaSalesUtil.akunKonfigurasi " + kunci);
            return null;
        }
    }

    private static void balikkan(Session session) {
        try {
            session.getTransaction().rollback();
        } catch (Exception ex) {
            // rollback gagal: kegagalan aslinya yang dilaporkan
        }
    }

    private static void tutup(Session session) {
        try {
            session.disconnect();
            ais.database.hibernate.HibernateUtil.closeSession();
        } catch (Exception e) {
            // penutupan sesi manual: kegagalannya tidak menutupi hasil posting
        }
    }
}
