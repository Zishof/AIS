package ais.action.master.payroll.helper;

/**
 * Dasbor khusus halaman Cuti dan Izin (cuti_dan_izin.zul).
 * Hanya menampilkan analisis cuti &amp; izin pegawai — bukan dasbor gabungan.
 * Seluruh logika dipakai-ulang dari {@link DasborAnalisisPenggajian} dengan fokus CUTI.
 */
public class DasborCutiDanIzin extends DasborAnalisisPenggajian {

    private static final long serialVersionUID = 1L;

    public DasborCutiDanIzin() {
        super(DasborAnalisisPenggajian.Fokus.CUTI);
    }
}
