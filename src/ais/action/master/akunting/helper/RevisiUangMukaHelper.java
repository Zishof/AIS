package ais.action.master.akunting.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.akunting.UangMuka;

/**
 * Helper revisi untuk entitas {@link UangMuka} (uang muka/panjar akunting), memakai kerangka kerja
 * revisi generik {@link GenericRevisiHelper}. Membatasi field yang dapat direvisi hanya pada
 * {@code kode}, {@code keterangan}, dan {@code nama}; seluruh logika tampilan dan penyimpanan
 * revisi diwarisi dari kelas induk generik.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiUangMukaHelper extends GenericRevisiHelper<UangMuka> {

    private static final long serialVersionUID = 6589578552710016753L;

    /** Membuat helper revisi uang muka dengan judul "Revisi Uang Muka" dan field yang dapat direvisi: kode, keterangan, nama. */
    public RevisiUangMukaHelper(EventListener eventListener) throws Exception {
        super(UangMuka.class, "Revisi Uang Muka", eventListener, new String[] { "kode", "keterangan", "nama" });
    }
}
