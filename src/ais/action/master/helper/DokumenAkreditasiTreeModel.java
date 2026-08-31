package ais.action.master.helper;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;

/**
 * Model tree ZK untuk menampilkan struktur berjenjang {@link DokumenAkreditasi} milik satu
 * {@link Akreditasi} (dokumen akreditasi beserta sub-dokumen/turunannya). Kelas ini hanya
 * menyediakan konstruktor pass-through ke {@link ais.action.master.helper.util.DokumenAkreditasiTreeModel}
 * (paket {@code util}) tempat seluruh logika pembentukan node/anak pohon sebenarnya berada; empat
 * varian konstruktor di sini mencerminkan titik awal pohon yang berbeda: dari akar
 * {@link Akreditasi} (dengan atau tanpa {@link AmbilDataSatuanKerjaBanbox} untuk data satuan
 * kerja), kosong, atau dari satu node induk {@link DokumenAkreditasi} tertentu (sub-pohon).
 */
public class DokumenAkreditasiTreeModel extends ais.action.master.helper.util.DokumenAkreditasiTreeModel {

    private static final long serialVersionUID = -5115651721345571411L;

    /**
     * Membentuk pohon dokumen akreditasi lengkap untuk satu {@link Akreditasi}, dengan data satuan
     * kerja tambahan dari {@code ambilDataSatuanKerjaBanbox} (dipakai untuk menyaring/melengkapi
     * tampilan pohon berdasarkan satuan kerja terkait).
     *
     * @param akreditasi                 akreditasi akar pohon
     * @param ambilDataSatuanKerjaBanbox sumber data satuan kerja pendukung tampilan pohon
     */
    public DokumenAkreditasiTreeModel(Akreditasi akreditasi, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox) {
        super(akreditasi, ambilDataSatuanKerjaBanbox);
    }

    /**
     * Membentuk pohon dokumen akreditasi lengkap untuk satu {@link Akreditasi}, tanpa data satuan
     * kerja tambahan.
     *
     * @param akreditasi akreditasi akar pohon
     */
    public DokumenAkreditasiTreeModel(Akreditasi akreditasi) {
        super(akreditasi);
    }

    /** Membentuk model pohon kosong (tanpa akar); dipakai saat data akan diisi belakangan. */
    public DokumenAkreditasiTreeModel() {
        super();
    }

    /**
     * Membentuk sub-pohon yang berakar pada satu {@link DokumenAkreditasi} induk tertentu, alih-alih
     * dari {@link Akreditasi} penuh.
     *
     * @param indukDokumenAkreditasi node dokumen akreditasi yang menjadi akar sub-pohon
     */
    public DokumenAkreditasiTreeModel(DokumenAkreditasi indukDokumenAkreditasi) {
        super(indukDokumenAkreditasi);
    }
}
