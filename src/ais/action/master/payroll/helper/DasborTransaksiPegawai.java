package ais.action.master.payroll.helper;

/**
 * Dasbor khusus halaman Transaksi Pegawai (transaksi_pegawai.zul).
 * Hanya menampilkan analisis transaksi pegawai (tunjangan/potongan) — bukan dasbor gabungan.
 * Seluruh logika dipakai-ulang dari {@link DasborAnalisisPenggajian} dengan fokus TRANSAKSI.
 */
public class DasborTransaksiPegawai extends DasborAnalisisPenggajian {

    private static final long serialVersionUID = 1L;

    public DasborTransaksiPegawai() {
        super(DasborAnalisisPenggajian.Fokus.TRANSAKSI);
    }
}
