package ais.action.master.feeder.integrator.impor;

import java.io.File;

/**
 * Hasil satu kali pembacaan berkas unggahan Feeder.
 *
 * <p>Impor tidak boleh dilaporkan hanya sebagai "berhasil" atau "gagal". Satu
 * berkas berisi ratusan baris, dan sebagian dapat gagal sementara sisanya
 * tersimpan — persis keadaan yang paling perlu diketahui operator, dan justru
 * yang paling mudah hilang bila jawabannya cuma satu kata. Karena itu tiga hal
 * dibawa pulang sekaligus: jumlah baris yang diproses, ringkasan yang dapat
 * dibaca langsung, dan berkas laporan yang merinci per baris.</p>
 */
public final class HasilImpor {

    /** Jumlah baris data yang diproses. */
    public int baris;

    /**
     * Berkas laporan per baris: mana yang berhasil, mana yang gagal, dan
     * mengapa. Inilah artefak yang diunduh operator setelah impor selesai.
     */
    public File laporan;

    /** Ringkasan singkat untuk ditampilkan langsung tanpa membuka laporan. */
    public String ringkasan = "";

    /**
     * Salinan .xlsx dari data sebagaimana tersimpan.
     *
     * <p>Layar ZK menawarkannya sebagai tautan unduhan di samping laporan.
     * Jalur native menuliskannya juga — supaya perilaku kedua jalur sama — namun
     * yang dijadikan berkas unduhan pekerjaannya adalah {@link #laporan}, karena
     * itulah yang menjawab pertanyaan "apa yang terjadi pada berkas saya".</p>
     */
    public File berkasHasil;
}
