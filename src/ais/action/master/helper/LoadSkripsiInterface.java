package ais.action.master.helper;

/**
 * Callback sederhana untuk memberi tahu pemanggil bahwa data skripsi yang sedang ditampilkan
 * perlu dimuat ulang. Dipakai oleh {@code FormatPenilaianSkripsiHelper#display(Skripsi, MyWindow,
 * LoadSkripsiInterface)} — komponen tersebut menerima implementasi ini dari layar pemanggil dan
 * memanggil {@link #refresh()} setelah aksi (mis. simpan penilaian) selesai, sehingga layar asal
 * dapat menyegarkan tampilannya tanpa perlu tahu detail internal komponen penilaian skripsi.
 */
public interface LoadSkripsiInterface {

	/** Dipanggil oleh komponen penilaian skripsi agar layar pemanggil menyegarkan data/tampilannya. */
	public void refresh();

}
