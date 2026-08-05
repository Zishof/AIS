package ais.action.master.payroll.helper;

/**
 * Dasbor khusus halaman Pembayaran Gaji (bayar_gaji_pegawai.zul).
 * Hanya menampilkan analisis pembayaran gaji — bukan dasbor gabungan.
 * Seluruh logika dipakai-ulang dari {@link DasborAnalisisPenggajian} dengan fokus GAJI.
 */
public class DasborGajiPegawai extends DasborAnalisisPenggajian {

    private static final long serialVersionUID = 1L;

    public DasborGajiPegawai() {
        super(DasborAnalisisPenggajian.Fokus.GAJI);
    }
}
