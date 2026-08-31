package ais.ui.util;

import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;

/**
 * Basis renderer {@link Treeitem} dual-kompatibel, mengikuti pola yang sama dengan {@link
 * MyRowRenderer} dan {@link MyComboitemRenderer}: API {@link TreeitemRenderer} versi lama (ZK 5)
 * memanggil {@code render} dengan 2 argumen, sedangkan versi ZK yang lebih baru memanggil
 * varian 3 argumen (menyertakan indeks baris). Kelas ini menyerap perbedaan tersebut dengan
 * mengimplementasikan varian 3 argumen dari {@link TreeitemRenderer} dan mendelegasikannya ke
 * varian 2 argumen abstrak, sehingga subclass cukup mengimplementasikan satu metode saja tanpa
 * peduli versi ZK yang sedang berjalan.
 */
public abstract class MyTreeitemRenderer implements TreeitemRenderer {

	/**
	 * Implementasi {@link TreeitemRenderer} versi 3 argumen (dipanggil oleh ZK versi baru);
	 * indeks baris ({@code arg2}) diabaikan dan pemanggilan didelegasikan ke {@link
	 * #render(Treeitem, Object)}.
	 *
	 * @param arg0 komponen {@link Treeitem} yang sedang dirender
	 * @param arg1 objek data untuk baris/node pohon tersebut
	 * @param arg2 indeks baris/node, tidak dipakai
	 * @throws Exception diteruskan dari implementasi subclass
	 */
	public void render(Treeitem arg0, Object arg1, int arg2) throws Exception {
		render(arg0, arg1);
	}

	/**
	 * Metode yang wajib diimplementasikan subclass untuk mengisi tampilan satu {@link Treeitem}
	 * berdasarkan objek data yang diberikan.
	 *
	 * @param arg0 komponen {@link Treeitem} yang akan diisi
	 * @param arg1 objek data untuk node tersebut
	 * @throws Exception boleh dilempar bila terjadi kegagalan saat merender
	 */
	public abstract void render(Treeitem arg0, Object arg1) throws Exception;
}
