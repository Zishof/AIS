package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Skripsi;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSkripsiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiSkripsiHelper(EventListener eventListener) throws Exception {
        super(Skripsi.class, "Revisi Skripsi", eventListener, new String[] { "judul", "nama", "keterangan" });
    }

    /**
     * Membuka riwayat untuk satu data skripsi saja. Filter ID membuat operator
     * dapat menelusuri perubahan nilai mahasiswa yang sedang dilihat tanpa harus
     * mencari di seluruh revisi skripsi kampus.
     */
    public RevisiSkripsiHelper(Skripsi skripsi, EventListener eventListener) throws Exception {
        super(Skripsi.class,
                "Riwayat Nilai Skripsi"
                        + (skripsi == null || skripsi.getMahasiswa() == null ? ""
                                : " - " + skripsi.getMahasiswa().getNim() + " "
                                        + skripsi.getMahasiswa().getNama()),
                eventListener,
                new String[] { "judul", "nama", "keterangan" },
                new GenericRevisiHelper.EntityIdFilter(skripsi == null ? null : skripsi.getId()));
    }
}
