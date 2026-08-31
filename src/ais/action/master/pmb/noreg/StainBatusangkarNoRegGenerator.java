package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma pembangkit nomor registrasi PMB khusus institusi STAIN Batusangkar. Format nomor:
 * 4 digit tahun berjalan diikuti 5 digit urutan pendaftar tahun tersebut (mis.
 * {@code "2026" + "00042"}). Urutan dihitung lewat {@link NoRegGeneratorSupport#nomorUrutBerikutnya}
 * sebagai angka setelah urutan tertinggi yang sudah dipakai di antara nomor registrasi ber-prefix
 * tahun berjalan (mempertimbangkan juga nomor yang sudah dipesan dalam batch berjalan lewat
 * {@code jumlahPengecualian}).
 */
public class StainBatusangkarNoRegGenerator implements NoRegGenerator {

	/** @return nomor registrasi baru untuk {@code biodataCalonMahasiswa}, lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi format tahun+urutan; bila hasilnya sudah dipakai
	 * (ditemukan pada data tersimpan), nomor tersebut ditambahkan ke {@code jumlahPengecualian}
	 * dan method memanggil dirinya sendiri secara rekursif untuk mencoba urutan berikutnya.
	 *
	 * @param jumlahPengecualian nomor yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @param biodataCalonMahasiswa data calon mahasiswa yang akan diberi nomor registrasi
	 * @return nomor registrasi yang belum pernah dipakai
	 */
	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		String digitPertama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";

		Session session = HibernateUtil.currentSession();
		long nomorUrut = NoRegGeneratorSupport.nomorUrutBerikutnya(session, digitPertama, 5,
				biodataCalonMahasiswa, jumlahPengecualian);
		String digitKedua = NoRegGeneratorSupport.leftPadNomor(nomorUrut, 5);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		System.out.println("digit kedua (kode tahun) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		boolean nomorSudahDipakai = NoRegGeneratorSupport.nomorSudahDipakai(session, noReg, biodataCalonMahasiswa);
		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
