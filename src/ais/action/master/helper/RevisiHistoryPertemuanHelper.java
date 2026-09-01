package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Pertemuan;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link Pertemuan} — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window,
 * alur Envers, dan fitur restore. Kelas ini hanya mengonfigurasi {@link GenericRevisiHelper}
 * generik dengan entitas {@code Pertemuan} dan kolom pencarian {@code topik},
 * {@code keterangan}, dan {@code nilaiHuruf}; tidak ada {@link GenericRevisiHelper.QueryCustomizer}
 * dan tidak ada override hook {@code afterRestoreInTransaction}. Berbeda dari
 * {@link RevisiPertemuanHelper} (yang menyaring per {@link Perkuliahan}), class ini SELALU
 * menampilkan riwayat SELURUH pertemuan tanpa penyaringan — seluruh logika tampil, cari,
 * bandingkan, dan restore revisi diwarisi sepenuhnya dari kelas induk.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryPertemuanHelper extends GenericRevisiHelper<Pertemuan> {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membangun jendela riwayat revisi untuk entitas {@link Pertemuan}, tanpa callback
     * khusus setelah restore, dengan pencarian pada kolom {@code topik},
     * {@code keterangan}, dan {@code nilaiHuruf}.
     *
     * @throws Exception diteruskan dari konstruksi {@link GenericRevisiHelper}
     */
    public RevisiHistoryPertemuanHelper() throws Exception {
        super(Pertemuan.class, "Riwayat Revisi Pertemuan", null, new String[] { "topik", "keterangan", "nilaiHuruf" });
    }
}
