package ais.ui.util;

import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 * Basis renderer {@link Row} dual-kompatibel yang dipakai hampir seluruh grid ({@code MyGrid})
 * di AIS. Menyerap perbedaan API {@link RowRenderer} antar versi ZK (versi lama memanggil
 * {@code render} 2 argumen, versi baru memanggil varian 3 argumen dengan indeks baris) dengan
 * mendelegasikan varian 3 argumen ke varian 2 argumen abstrak — subclass hanya perlu
 * mengimplementasikan satu metode. Selain itu, setelah setiap baris selesai dirender, kelas ini
 * SELALU memanggil {@link UIHelper#absorptionKebab(Row)} yang memindahkan tombol-tombol aksi
 * "nyasar" (mis. tombol edit/hapus yang ditempel langsung ke Hbox baris) ke dalam satu popup
 * kebab per baris, menjaga tampilan grid tetap rapi dan konsisten tanpa subclass perlu
 * menanganinya sendiri.
 */
public abstract class MyRowRenderer implements RowRenderer {

    /**
     * Implementasi {@link RowRenderer} versi 3 argumen (dipanggil ZK versi baru): mendelegasikan
     * ke {@link #render(Row, Object)} lalu merapikan tombol aksi baris lewat
     * {@link UIHelper#absorptionKebab(Row)}.
     *
     * @param arg0 baris {@link Row} yang sedang dirender
     * @param arg1 objek data untuk baris tersebut
     * @param arg2 indeks baris, tidak dipakai
     * @throws Exception diteruskan dari implementasi subclass
     */
    public void render(Row arg0, Object arg1, int arg2) throws Exception {
        render(arg0, arg1);
        UIHelper.absorptionKebab(arg0);
    }

    /**
     * Metode yang wajib diimplementasikan subclass untuk mengisi sel-sel satu {@link Row}
     * berdasarkan objek data baris yang diberikan.
     *
     * @param arg0 baris yang akan diisi
     * @param arg1 objek data untuk baris tersebut
     * @throws Exception boleh dilempar bila terjadi kegagalan saat merender
     */
    public abstract void render(Row arg0, Object arg1) throws Exception;

}
