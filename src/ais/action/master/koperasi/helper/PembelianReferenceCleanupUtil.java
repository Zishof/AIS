package ais.action.master.koperasi.helper;

import org.hibernate.Session;

/**
 * Membersihkan referensi yang menunjuk ke baris {@code koperasi.pembelian}
 * sebelum rincian pembelian dihapus.
 */
public final class PembelianReferenceCleanupUtil {

    private PembelianReferenceCleanupUtil() {
    }

    public static int lepasDraftPembelianLunas(Session session, Long pembelianId) {
        if (session == null || pembelianId == null) {
            return 0;
        }
        return session.createSQLQuery("update koperasi.draft_pembelian set lunas = null where lunas = :pembelianId")
                .setLong("pembelianId", pembelianId.longValue()).executeUpdate();
    }

    public static int lepasDraftPembelianLunasUntukHeader(Session session, Long pembelianAnggotaKoperasiId) {
        if (session == null || pembelianAnggotaKoperasiId == null) {
            return 0;
        }
        int jumlah = session.createSQLQuery(
                "update koperasi.draft_pembelian set lunas = null where lunas in "
                        + "(select id from koperasi.pembelian where pembelian_anggota_koperasi = :headerId)")
                .setLong("headerId", pembelianAnggotaKoperasiId.longValue()).executeUpdate();
        jumlah += session.createSQLQuery(
                "update koperasi.draft_pembelian_anggota_koperasi set lunas = null where lunas = :headerId")
                .setLong("headerId", pembelianAnggotaKoperasiId.longValue()).executeUpdate();
        return jumlah;
    }

    public static int lepasDraftPembelianLunasUntukKodePembayaranOnline(Session session, Long kodePembayaranOnlineId) {
        if (session == null || kodePembayaranOnlineId == null) {
            return 0;
        }
        return session.createSQLQuery(
                "update koperasi.draft_pembelian set lunas = null where lunas in "
                        + "(select id from koperasi.pembelian where kode_pembayaran_online = :kodeId)")
                .setLong("kodeId", kodePembayaranOnlineId.longValue()).executeUpdate();
    }
}
