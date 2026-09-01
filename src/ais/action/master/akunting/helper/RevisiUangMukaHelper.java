package ais.action.master.akunting.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.akunting.UangMuka;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link UangMuka} (uang muka/panjar akunting) — lihat Javadoc class tersebut untuk penjelasan
 * lengkap arsitektur window (3 tab), alur Envers, cache count, dan fitur restore (satu revisi
 * maupun massal per tanggal).
 *
 * <p>Kekhasan helper ini: tidak ada {@code QueryCustomizer} tambahan (riwayat mencakup SEMUA
 * data {@link UangMuka}, tidak dibatasi pada satu entity induk), dan pencarian kata kunci hanya
 * menyaring tiga property — {@code kode}, {@code keterangan}, {@code nama} — yang memang satu-
 * satunya field deskriptif pada entity ini. Judul window: "Revisi Uang Muka".</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiUangMukaHelper extends GenericRevisiHelper<UangMuka> {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuat helper revisi uang muka dengan judul "Revisi Uang Muka" dan field yang dapat
     * dicari: {@code kode}, {@code keterangan}, {@code nama}. Tidak ada filter tambahan — seluruh
     * riwayat revisi {@link UangMuka} ditampilkan.
     *
     * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper},
     *                      dipanggil saat ada event window (mis. {@code onDeleteDataIni}).
     */
    public RevisiUangMukaHelper(EventListener eventListener) throws Exception {
        super(UangMuka.class, "Revisi Uang Muka", eventListener, new String[] { "kode", "keterangan", "nama" });
    }
}
