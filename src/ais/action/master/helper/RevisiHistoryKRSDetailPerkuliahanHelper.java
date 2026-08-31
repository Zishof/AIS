package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Detailperkuliahan;

/**
 * Jendela riwayat revisi untuk entitas {@link Detailperkuliahan} (baris KRS per mata kuliah,
 * mis. nilai huruf yang diperoleh mahasiswa). Kelas ini hanya mengonfigurasi
 * {@link GenericRevisiHelper} generik dengan entitas, judul, dan kolom pencarian yang relevan
 * ({@code tahunAkademik}, {@code nilaiHuruf}, {@code keterangan}); seluruh logika tampil/cari/
 * bandingkan versi revisi ditangani oleh induknya. Dipakai dari layar admin untuk menelusuri
 * riwayat perubahan nilai KRS mahasiswa (mis. audit perbaikan nilai).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryKRSDetailPerkuliahanHelper extends GenericRevisiHelper<Detailperkuliahan> {

    private static final long serialVersionUID = 6589578552710016753L;

    /** Membuka jendela riwayat revisi {@link Detailperkuliahan} tanpa callback tambahan saat ditutup. */
    public RevisiHistoryKRSDetailPerkuliahanHelper() throws Exception {
        super(Detailperkuliahan.class, "Riwayat Revisi KRS Detail Perkuliahan", null, new String[] { "tahunAkademik", "nilaiHuruf", "keterangan" });
    }
}
