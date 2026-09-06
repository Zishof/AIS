package ais.database.model;

/**
 * Uji kontrak fallback gelombang biaya tanpa koneksi database.
 *
 * <p>
 * Memverifikasi kontrak {@link Mahasiswa#getGelombangPendaftaranUntukBiaya()}:
 * (1) gelombang milik mahasiswa sendiri MENANG bila terisi (dan sumber PMB
 * tidak perlu/tidak boleh dibaca — malas/lazy), (2) bila gelombang mahasiswa
 * kosong, jatuh ke gelombang pendaftaran calon mahasiswa (data PMB) sebagai
 * fallback TANPA memutasi kolom mahasiswa, dan (3) bila tidak ada sumber sama
 * sekali (baik gelombang sendiri maupun data calon), hasilnya tetap {@code null}
 * — bukan "cocok ke semua gelombang". Dijalankan sebagai program {@code main}
 * biasa (bukan JUnit) memakai stub ringan {@link Mahasiswa}/{@link BiodataCalonMahasiswa}
 * tanpa koneksi database, mirip pola {@code test.LazyAssociationGetterSelfTest}.
 * </p>
 */
public final class GelombangBiayaMahasiswaSelfTest {
	/**
	 * Stub {@link Mahasiswa} yang menimpa akses gelombang langsung dan data PMB
	 * agar dapat dikendalikan sepenuhnya di memori (tanpa Hibernate), sekaligus
	 * menghitung berapa kali data PMB benar-benar dibaca ({@link #bacaPmb}).
	 */
	private static final class MahasiswaUji extends Mahasiswa {
		/** Gelombang pendaftaran milik mahasiswa sendiri (skenario yang diuji). */
		private GelombangPendaftaran langsung;
		/** Data calon mahasiswa (sumber fallback PMB) yang diuji. */
		private BiodataCalonMahasiswa calon;
		/** Penghitung berapa kali {@link #getBiodataCalonMahasiswaData()} dipanggil. */
		private int bacaPmb;
		/** @return {@link #langsung}, menggantikan pembacaan kolom Hibernate asli. */
		@Override public GelombangPendaftaran getGelombangPendaftaran() { return langsung; }
		/**
		 * @return {@link #calon}, sekaligus menambah {@link #bacaPmb} agar test dapat
		 *         memverifikasi method ini TIDAK dipanggil ketika tidak perlu (lazy).
		 */
		@Override public BiodataCalonMahasiswa getBiodataCalonMahasiswaData() {
			bacaPmb++;
			return calon;
		}
	}

	/**
	 * Menegakkan satu asersi uji; melempar {@link AssertionError} berisi {@code pesan}
	 * bila {@code benar} bernilai false.
	 *
	 * @param benar hasil kondisi yang diharapkan true.
	 * @param pesan pesan kegagalan bila asersi tidak terpenuhi.
	 */
	private static void cek(boolean benar, String pesan) {
		if (!benar) throw new AssertionError(pesan);
	}

	/**
	 * Menjalankan seluruh skenario uji fallback gelombang biaya secara berurutan;
	 * melempar {@link AssertionError} pada skenario pertama yang gagal, atau
	 * mencetak "PASS ..." ke stdout bila semua skenario lolos.
	 *
	 * @param args tidak dipakai.
	 */
	public static void main(String[] args) {
		final GelombangPendaftaran gelombangPmb = new GelombangPendaftaran();
		GelombangPendaftaran gelombangMahasiswa = new GelombangPendaftaran();
		MahasiswaUji mahasiswa = new MahasiswaUji();
		mahasiswa.calon = new BiodataCalonMahasiswa() {
			@Override public GelombangPendaftaran getGelombangPendaftaran() { return gelombangPmb; }
		};
		mahasiswa.langsung = gelombangMahasiswa;
		cek(mahasiswa.getGelombangPendaftaranUntukBiaya() == gelombangMahasiswa,
				"Gelombang mahasiswa harus menang atas PMB yang berbeda");
		cek(mahasiswa.bacaPmb == 0, "Tidak perlu membaca PMB bila gelombang mahasiswa terisi");
		mahasiswa.langsung = null;
		cek(mahasiswa.getGelombangPendaftaranUntukBiaya() == gelombangPmb,
				"Gelombang kosong harus menggunakan hasil pembacaan PMB");
		cek(mahasiswa.langsung == null, "Fallback tidak boleh mengubah kolom mahasiswa");
		mahasiswa.calon = null;
		cek(mahasiswa.getGelombangPendaftaranUntukBiaya() == null,
				"Tanpa sumber gelombang harus tetap kosong, bukan cocok ke semua gelombang");
		System.out.println("PASS gelombang biaya mahasiswa: prioritas, fallback, tanpa mutasi, tanpa PMB");
	}
}
