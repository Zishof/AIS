package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Detailperkuliahan;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link Detailperkuliahan} (baris nilai/detail perkuliahan mahasiswa per matakuliah) — lihat
 * Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur
 * restore. Mengonfigurasi {@link GenericRevisiHelper} generik dengan kolom yang dipantau
 * perubahannya: {@code tahunAkademik}, {@code nilaiHuruf}, dan {@code keterangan} — tiga kolom
 * yang paling relevan untuk menelusuri riwayat perubahan nilai. Tidak ada {@code EventListener}
 * yang diteruskan (selalu {@code null}), tidak ada {@link GenericRevisiHelper.QueryCustomizer}
 * (tanpa penyaringan berdasarkan mahasiswa/perkuliahan tertentu — beda dengan
 * {@link RevisiDetailPerkuliahanHelper}/{@link RevisiDetailPerkuliahanDariMahasiswaHelper} yang
 * menyaring), dan tidak ada override hook {@code afterRestoreInTransaction}; seluruh logika ada
 * pada kelas induk.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryDetailPerkuliahanHelper extends GenericRevisiHelper<Detailperkuliahan> {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuat helper revisi untuk {@link Detailperkuliahan} dengan judul
     * "Riwayat Revisi Detail Perkuliahan".
     *
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiHistoryDetailPerkuliahanHelper() throws Exception {
        super(Detailperkuliahan.class, "Riwayat Revisi Detail Perkuliahan", null, new String[] { "tahunAkademik", "nilaiHuruf", "keterangan" });
    }
}
