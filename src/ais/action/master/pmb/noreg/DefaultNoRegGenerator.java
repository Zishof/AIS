package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma pembangkit nomor registrasi PMB bawaan (dipakai bila institusi tidak memiliki
 * generator khusus). Format nomor: tahun pendaftaran ({@code biodataCalonMahasiswa.getTahun()})
 * diikuti sejumlah digit urutan (panjang dikontrol konfigurasi
 * {@code jumlah_increments_no_registrasi_pmb}, default 8). Urutan dihitung lewat
 * {@link NoRegGeneratorSupport#nomorUrutBerikutnya} sebagai angka setelah urutan tertinggi yang
 * sudah dipakai di antara nomor registrasi ber-prefix tahun yang sama (mempertimbangkan juga
 * nomor yang sudah dipesan dalam batch berjalan lewat {@code jumlahPengecualian}), lalu dicek
 * ulang keunikannya lewat {@link NoRegGeneratorSupport#nomorSudahDipakai}.
 */
public class DefaultNoRegGenerator implements NoRegGenerator {

	/** @return nomor registrasi baru untuk {@code biodataCalonMahasiswa}, lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi format tahun+urutan (urutan = angka setelah urutan tertinggi
	 * yang sudah dipakai untuk prefix tahun yang sama); bila hasil ternyata sudah dipakai, nomor
	 * tersebut ditambahkan ke {@code jumlahPengecualian} dan method memanggil dirinya sendiri
	 * secara rekursif untuk mencoba urutan berikutnya.
	 *
	 * @param jumlahPengecualian nomor yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @param biodataCalonMahasiswa data calon mahasiswa yang akan diberi nomor registrasi
	 * @return nomor registrasi yang belum pernah dipakai
	 */
	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Session session = HibernateUtil.currentSession();
		Integer jumlahIncrements = 8;
		try {
			jumlahIncrements = Integer
					.parseInt(Common.getKonfigurasi("jumlah_increments_no_registrasi_pmb", "8").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		String prefix = biodataCalonMahasiswa.getTahun() + "";
		long nomorUrut = NoRegGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahIncrements,
				biodataCalonMahasiswa, jumlahPengecualian);
		String noreg = prefix + NoRegGeneratorSupport.leftPadNomor(nomorUrut, jumlahIncrements);

		boolean nomorSudahDipakai = NoRegGeneratorSupport.nomorSudahDipakai(session, noreg, biodataCalonMahasiswa);

		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noreg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		} else {
			return noreg;
		}
	}

}
