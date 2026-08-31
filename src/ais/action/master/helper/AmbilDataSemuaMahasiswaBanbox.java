package ais.action.master.helper;

/**
 * Varian {@link AmbilDataMahasiswaBanbox} (komponen bandbox pencari mahasiswa) yang
 * menampilkan <b>seluruh</b> mahasiswa tanpa dibatasi hak akses dosen pembimbing/wali
 * pada sesi login. Perbedaannya hanya pada konstruktor: bendera
 * {@code tidakMelihatDosen} dipaksa {@code true} sehingga filter "hanya mahasiswa
 * bimbingan dosen yang login" pada kelas induk tidak diterapkan.
 */
public class AmbilDataSemuaMahasiswaBanbox extends AmbilDataMahasiswaBanbox {
	private static final long serialVersionUID = 6452461056684904810L;


	/** Membuat bandbox pencari mahasiswa dalam mode "semua mahasiswa" (tanpa filter dosen). */
	public AmbilDataSemuaMahasiswaBanbox() {
		super();
		tidakMelihatDosen = true;
	}
}
