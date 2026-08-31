package ais.ui.util;

import java.util.HashMap;

/**
 * Tipe khusus untuk my hash map. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * HashMap}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code MAX}; operasi lokal: {@code put}(). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see HashMap
 */
public class MyHashMap<K, V> extends HashMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 12323333333333333L;
	private int MAX;

	public MyHashMap(int MAX) {
		this.MAX = MAX;
	}

	public V put(K key, V value) {
		if (this.size() < MAX) {
			return super.put(key, value);
		}
		return null;
	}

}
