package ais.database.hibernate;

import org.hibernate.cfg.DefaultNamingStrategy;

/**
 * Tipe khusus untuk my naming strategy. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DefaultNamingStrategy}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code tableName}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see DefaultNamingStrategy
 */
public class MyNamingStrategy extends DefaultNamingStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 483792013046971333L;
	
	

	@Override
	public String tableName(String tableName) {
		if (tableName != null && tableName.trim().equalsIgnoreCase("cicilan_pembayaran")) {
			
		}
		return tableName;
	}

}
