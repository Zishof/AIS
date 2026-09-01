package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Detailperkuliahan;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link Detailperkuliahan} (baris KRS per mata kuliah, mis. nilai huruf yang diperoleh
 * mahasiswa) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur
 * Envers, dan fitur restore. Kelas ini hanya mengonfigurasi {@link GenericRevisiHelper} generik
 * dengan entitas, judul, dan kolom pencarian yang relevan ({@code tahunAkademik},
 * {@code nilaiHuruf}, {@code keterangan}); tidak ada {@link GenericRevisiHelper.QueryCustomizer}
 * dan tidak ada override hook {@code afterRestoreInTransaction} — seluruh logika tampil/cari/
 * bandingkan versi revisi ditangani oleh induknya. Dipakai dari layar admin untuk menelusuri
 * riwayat perubahan nilai KRS mahasiswa (mis. audit perbaikan nilai); mirip
 * {@link RevisiHistoryDetailPerkuliahanHelper} namun dengan judul window dan konteks pemakaian
 * yang berbeda (KRS vs. riwayat detail perkuliahan umum).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryKRSDetailPerkuliahanHelper extends GenericRevisiHelper<Detailperkuliahan> {

    private static final long serialVersionUID = 6589578552710016753L;

    /** Membuka jendela riwayat revisi {@link Detailperkuliahan} tanpa callback tambahan saat ditutup. */
    public RevisiHistoryKRSDetailPerkuliahanHelper() throws Exception {
        super(Detailperkuliahan.class, "Riwayat Revisi KRS Detail Perkuliahan", null, new String[] { "tahunAkademik", "nilaiHuruf", "keterangan" });
    }
}
