package ais.common.listener;

/**
 * Kontrak generik untuk komponen yang perlu diberi tahu ketika sebuah nilai data baru tersedia dan
 * harus dimuat/diproses. Antarmuka ini berperan sebagai kontrak <i>listener</i> tunggal-method
 * (mirip pola <i>observer</i>) yang memungkinkan kode pengirim data ({@code Object value}, dapat
 * berupa entitas hasil query, hasil parsing berkas, atau struktur data lain apa pun) tetap terpisah
 * dari kode penerima yang bertanggung jawab memuat/menampilkan/menyimpan data tersebut.
 *
 * <p>
 * Karena parameter {@link #loadData(Object)} bertipe {@link Object} generik, kontrak ini tidak
 * terikat pada satu bentuk data tertentu — implementasi konkret bertanggung jawab melakukan
 * pengecekan tipe (mis. {@code instanceof}) dan pemeran tipe (<i>casting</i>) sesuai kebutuhan
 * kontekstualnya masing-masing sebelum memproses nilai yang diterima.
 * </p>
 *
 * <p>
 * Pola pemakaian umumnya: komponen pemuat data (mis. loader hasil query per halaman, hasil unggahan
 * berkas, atau hasil callback asinkron lain) memegang referensi ke satu atau lebih implementasi
 * {@link DataLoader} dan memanggil {@link #loadData(Object)} setiap kali data baru siap
 * diserahkan, tanpa perlu tahu apa yang dilakukan penerima terhadap data tersebut.
 * </p>
 */
public interface DataLoader {

	/**
	 * Dipanggil oleh komponen pengirim ketika sebuah nilai data baru siap dimuat/diproses oleh
	 * implementasi antarmuka ini.
	 *
	 * @param value data yang dikirimkan untuk dimuat/diproses; tipe konkretnya bergantung pada
	 *              konteks pemanggilan dan harus divalidasi/di-<i>cast</i> oleh implementasi
	 */
	public void loadData(Object value);

}
