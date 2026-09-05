package ais.database.model;

/** Uji kontrak fallback gelombang biaya tanpa koneksi database. */
public final class GelombangBiayaMahasiswaSelfTest {
	private static final class MahasiswaUji extends Mahasiswa {
		private GelombangPendaftaran langsung;
		private BiodataCalonMahasiswa calon;
		private int bacaPmb;
		@Override public GelombangPendaftaran getGelombangPendaftaran() { return langsung; }
		@Override public BiodataCalonMahasiswa getBiodataCalonMahasiswaData() {
			bacaPmb++;
			return calon;
		}
	}

	private static void cek(boolean benar, String pesan) {
		if (!benar) throw new AssertionError(pesan);
	}

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
