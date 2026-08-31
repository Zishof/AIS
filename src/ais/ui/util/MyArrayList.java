package ais.ui.util;

import java.util.ArrayList;

/**
 * Tipe khusus untuk my array list. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * ArrayList}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code maxSize}; operasi lokal: {@code add}().
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see ArrayList
 */
public class MyArrayList<E> extends ArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1111111111111111L;
	private int maxSize;

	public MyArrayList(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public boolean add(E e) {
		if (this.size() < maxSize && !contains(e)) {
			return super.add(e);
		}
		return false;
	}
}
