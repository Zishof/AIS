package ais.action.master.koperasi.helper;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.PencairanDiskon;

/**
 * <h2>Posting keluarga DANA ANGGOTA koperasi ke buku besar (dok 61 butir B).</h2>
 *
 * <p>Sebelum ini seluruh perputaran dana anggota — topup saldo lewat Virtual Account bank dan
 * pencairan saldo cashback — bergerak tanpa menyentuh buku besar sama sekali: uangnya nyata masuk
 * dan keluar rekening, kewajiban koperasi kepada anggota berubah, tetapi tidak ada satu baris
 * jurnal pun. Akibatnya saldo kas di Neraca lebih kecil dari kas sesungguhnya dan kewajiban kepada
 * anggota tidak pernah muncul.</p>
 *
 * <p><b>Akun sengaja dibuat DAPAT DIATUR, bukan ditebak.</b> Pilihan akun untuk saldo anggota
 * (kewajiban koperasi kepada anggota) dan untuk beban pencairan cashback adalah keputusan
 * akuntansi lembaga, bukan keputusan teknis. Karena itu keduanya dibaca dari Konfigurasi; selama
 * belum diisi mesin ini <b>tidak menjurnal apa pun</b> dan mencatat alasannya ke ErrorAudit —
 * dokumennya tetap tampil sebagai draf di dasbor Draft Jurnal sehingga kekurangan setup terlihat,
 * bukan menghasilkan jurnal ke akun yang salah.</p>
 *
 * <p>Akun kas/bank TIDAK dikonfigurasi karena sudah ada datanya: diambil dari
 * {@link CaraPembayaranKoperasi#getAkun()} milik dokumen — pola yang sama dengan jurnal balik
 * pembatalan kantin (dok 64).</p>
 */
public final class PostingDanaAnggotaUtil {

    private PostingDanaAnggotaUtil() {
    }

    /** Kunci Konfigurasi akun KEWAJIBAN saldo/deposit anggota (utang koperasi kepada anggota). */
    public static final String KONF_AKUN_SALDO_ANGGOTA = "akun_kewajiban_saldo_anggota_id";

    /** Kunci Konfigurasi akun BEBAN pencairan diskon/cashback anggota. */
    public static final String KONF_AKUN_BEBAN_PENCAIRAN = "akun_beban_pencairan_diskon_id";

    /** Ambil satu Akun dari Konfigurasi; null bila kunci belum diisi atau akunnya tidak ada. */
    private static Akun akunKonfigurasi(String kunci) {
        try {
            String nilai = Common.getKonfigurasi(kunci, "").getNilai();
            if (nilai == null || nilai.trim().length() == 0) {
                return null;
            }
            return (Akun) ais.common.ConstantValues.ambil(Akun.class.getName(),
                    Long.parseLong(nilai.trim()));
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit PostingDanaAnggotaUtil.akunKonfigurasi " + kunci);
            return null;
        }
    }

    private static void laporkanKonfigurasiKosong(String kunci, String namaJurnal) {
        ais.common.ErrorAuditUtil.record(new IllegalStateException(
                "Konfigurasi " + kunci + " belum diisi -- " + namaJurnal
                        + " tidak dapat diposting."), "PostingDanaAnggotaUtil jalur API");
    }

    private static PostingHistory buatRiwayat(Session session, String jenis, Tbmuser oleh,
            Date tglPosting, Date mulai, Date sampai, String keterangan) {
        PostingHistory ph = new PostingHistory(jenis);
        ph.setTbmuser(oleh);
        ph.setTanggal(tglPosting == null ? new Date() : tglPosting);
        ph.setTanggalPosting(tglPosting == null ? new Date() : tglPosting);
        ph.setPosting(true);
        ph.setKeterangan(keterangan
                + (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
                        + " s.d " + Common.dateFormat.get().format(sampai) : ""));
        session.getTransaction().begin();
        session.save(ph);
        session.getTransaction().commit();
        return ph;
    }

    /** Hapus riwayat yang tidak jadi dipakai supaya dasbor tidak menampilkan posting kosong. */
    private static void hapusRiwayatKosong(PostingHistory ph) {
        try {
            Session s = ais.database.hibernate.HibernateUtil.currentNativeSession();
            s.getTransaction().begin();
            s.delete(ph);
            s.getTransaction().commit();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit PostingDanaAnggotaUtil.hapusRiwayatKosong");
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

    // ==================================================================== TOPUP SALDO ANGGOTA

    /**
     * Kriteria dokumen topup saldo anggota yang layak dijurnal — HARUS sama persis dengan yang
     * dipakai dasbor Draft Jurnal, agar angka draf dan yang benar-benar diproses tidak berselisih.
     */
    public static org.hibernate.Criteria kriteriaTopupStatic(Session session, Date mulai, Date sampai) {
        org.hibernate.Criteria c = session.createCriteria(PembayaranAnggotaKoperasi.class)
                .add(Restrictions.isNotNull("tanggalBayar"))
                .add(Restrictions.isNotNull("nominal"))
                .add(Restrictions.ne("nominal", 0.0));
        batasiTanggal(c, "this_.tanggal_bayar", mulai, sampai);
        return c;
    }

    /** Batasan rentang tanggal, dipisah agar bentuknya sama di kriteria mana pun. */
    private static void batasiTanggal(org.hibernate.Criteria c, String kolom, Date mulai, Date sampai) {
        if (mulai != null && sampai != null) {
            c.add(Restrictions.sqlRestriction("date(" + kolom + ") between date('"
                    + Common.databaseDateFormat.get().format(mulai) + "') and date('"
                    + Common.databaseDateFormat.get().format(sampai) + "')"));
        }
    }

    public static int postingSemua(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaTopupStatic(session, mulai, sampai)
                    .add(Restrictions.isNull("postingHistory")).list();
            if (daftar.isEmpty()) {
                return 0;
            }
            Akun akunSaldo = akunKonfigurasi(KONF_AKUN_SALDO_ANGGOTA);
            if (akunSaldo == null) {
                laporkanKonfigurasiKosong(KONF_AKUN_SALDO_ANGGOTA, "jurnal topup saldo anggota");
                return 0;
            }

            PostingHistory ph = buatRiwayat(session, PostingHistory.JENIS_TOPUP_SALDO_ANGGOTA, oleh,
                    tglPosting, mulai, sampai, "Posting massal topup saldo anggota dari dasbor jurnal");

            for (Object o : daftar) {
                PembayaranAnggotaKoperasi bayar = (PembayaranAnggotaKoperasi) o;
                if (bayar == null) {
                    continue;
                }
                try {
                    Akun akunKas = null;
                    CaraPembayaranKoperasi cara = bayar.getCaraPembayaranKoperasi();
                    if (cara != null) {
                        akunKas = cara.getAkun();
                    }
                    Double nilai = bayar.getNominal();
                    if (akunKas == null || nilai == null || nilai == 0.0) {
                        // Cara pembayaran belum ber-akun: dilewati, dokumen tetap draf.
                        continue;
                    }
                    String ket = "Topup saldo anggota "
                            + (bayar.getAnggotaKoperasi() == null ? "-" : bayar.getAnggotaKoperasi().getNama())
                            + " senilai " + Common.numberFormat.get().format(nilai)
                            + (bayar.getInquiryPembayaran() == null ? ""
                                    : " (ref " + bayar.getInquiryPembayaran() + ")");

                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    boolean tersimpan = ais.action.master.akunting.util.CommonAkunting.saveTransaksi(
                            new Akun[] { akunKas }, new Akun[] { akunSaldo }, null, null, ph, true,
                            ket, bayar.getTanggalBayar(), new Double[] { nilai },
                            new Double[] { nilai }, 0.0, bayar, null, session);
                    if (tersimpan) {
                        bayar.setPostingHistory(ph);
                        session.update(bayar);
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
                    ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil topup jalur API");
                }
            }

            if (n == 0) {
                hapusRiwayatKosong(ph);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil.postingSemua topup");
        } finally {
            tutup(session);
        }
        return n;
    }

    /**
     * Membatalkan posting SEMUA topup terposting pada rentang: baris jurnalnya dihapus (anak
     * {@code akunting.transaksi} dulu, lalu {@code grup_transaksi} -- hanya yang belum closing),
     * lalu cap {@code postingHistory} dilepas sehingga dokumen kembali menjadi draf.
     */
    public static int batalkanPostingSemua(Date mulai, Date sampai) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaTopupStatic(session, mulai, sampai)
                    .add(Restrictions.isNotNull("postingHistory")).list();
            for (Object o : daftar) {
                PembayaranAnggotaKoperasi bayar = (PembayaranAnggotaKoperasi) o;
                if (bayar == null) {
                    continue;
                }
                try {
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    hapusJurnal(session, "pembayaran_anggota_koperasi", bayar.getId());
                    bayar.setPostingHistory(null);
                    session.update(bayar);
                    session.getTransaction().commit();
                    n++;
                } catch (Exception e) {
                    balikkan(session);
                    ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil batal topup jalur API");
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil.batalkanPostingSemua topup");
        } finally {
            tutup(session);
        }
        return n;
    }

    // ====================================================================== PENCAIRAN DISKON

    /** Status pencairan yang berarti uangnya benar-benar keluar. */
    public static final String STATUS_BERHASIL = "BERHASIL";

    public static org.hibernate.Criteria kriteriaPencairanStatic(Session session, Date mulai, Date sampai) {
        org.hibernate.Criteria c = session.createCriteria(PencairanDiskon.class)
                .add(Restrictions.eq("status", STATUS_BERHASIL))
                .add(Restrictions.isNotNull("nominalCair"))
                .add(Restrictions.ne("nominalCair", 0.0));
        batasiTanggal(c, "this_.waktu_pencairan", mulai, sampai);
        return c;
    }

    public static int postingSemuaPencairan(Date mulai, Date sampai, Tbmuser oleh, Date tglPosting) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaPencairanStatic(session, mulai, sampai)
                    .add(Restrictions.isNull("postingHistory")).list();
            if (daftar.isEmpty()) {
                return 0;
            }
            Akun akunBeban = akunKonfigurasi(KONF_AKUN_BEBAN_PENCAIRAN);
            if (akunBeban == null) {
                laporkanKonfigurasiKosong(KONF_AKUN_BEBAN_PENCAIRAN, "jurnal pencairan diskon anggota");
                return 0;
            }

            PostingHistory ph = buatRiwayat(session, PostingHistory.JENIS_PENCAIRAN_DISKON, oleh,
                    tglPosting, mulai, sampai,
                    "Posting massal pencairan diskon anggota dari dasbor jurnal");

            for (Object o : daftar) {
                PencairanDiskon cair = (PencairanDiskon) o;
                if (cair == null) {
                    continue;
                }
                try {
                    Akun akunKas = null;
                    CaraPembayaranKoperasi cara = cair.getCaraPembayaran();
                    if (cara != null) {
                        akunKas = cara.getAkun();
                    }
                    Double nilai = cair.getNominalCair();
                    if (akunKas == null || nilai == null || nilai == 0.0) {
                        continue;
                    }
                    String ket = "Pencairan diskon/cashback "
                            + (cair.getAnggotaKoperasi() == null ? "-" : cair.getAnggotaKoperasi().getNama())
                            + " senilai " + Common.numberFormat.get().format(nilai)
                            + (cair.getKodePencairan() == null ? "" : " (" + cair.getKodePencairan() + ")");

                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    boolean tersimpan = ais.action.master.akunting.util.CommonAkunting.saveTransaksi(
                            new Akun[] { akunBeban }, new Akun[] { akunKas }, null, null, ph, true,
                            ket, cair.getWaktuPencairan(), new Double[] { nilai },
                            new Double[] { nilai }, 0.0, cair, null, session);
                    if (tersimpan) {
                        cair.setPostingHistory(ph);
                        session.update(cair);
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
                    ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil pencairan jalur API");
                }
            }

            if (n == 0) {
                hapusRiwayatKosong(ph);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil.postingSemua pencairan");
        } finally {
            tutup(session);
        }
        return n;
    }

    /** Pembatalan posting pencairan diskon; cerminan {@link #batalkanPostingSemua(Date, Date)}. */
    public static int batalkanPostingSemuaPencairan(Date mulai, Date sampai) {
        int n = 0;
        Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        try {
            List<?> daftar = kriteriaPencairanStatic(session, mulai, sampai)
                    .add(Restrictions.isNotNull("postingHistory")).list();
            for (Object o : daftar) {
                PencairanDiskon cair = (PencairanDiskon) o;
                if (cair == null) {
                    continue;
                }
                try {
                    session = ais.database.hibernate.HibernateUtil.currentNativeSession();
                    session.getTransaction().begin();
                    hapusJurnal(session, "pencairan_diskon", cair.getId());
                    cair.setPostingHistory(null);
                    session.update(cair);
                    session.getTransaction().commit();
                    n++;
                } catch (Exception e) {
                    balikkan(session);
                    ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil batal pencairan jalur API");
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PostingDanaAnggotaUtil.batalkanPostingSemua pencairan");
        } finally {
            tutup(session);
        }
        return n;
    }

    /**
     * Hapus jurnal satu dokumen: anak {@code akunting.transaksi} lebih dulu karena
     * {@code grup_transaksi} adalah induknya; baris yang sudah tutup buku ({@code closing})
     * sengaja tidak disentuh.
     */
    private static void hapusJurnal(Session session, String kolomReferensi, Long id) {
        session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in ("
                + " select id from akunting.grup_transaksi where " + kolomReferensi
                + " = " + id + " and closing is null )").executeUpdate();
        session.createSQLQuery("delete from akunting.grup_transaksi where " + kolomReferensi
                + " = " + id + " and closing is null").executeUpdate();
    }

    private static void balikkan(Session session) {
        try {
            session.getTransaction().rollback();
        } catch (Exception ex) {
            // rollback gagal: kegagalan aslinya yang dilaporkan
        }
    }
}
