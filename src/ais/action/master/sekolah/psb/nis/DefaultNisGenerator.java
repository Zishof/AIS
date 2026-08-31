package ais.action.master.sekolah.psb.nis;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Implementasi baku (generik, tidak spesifik institusi) algoritma penomoran NIS untuk calon siswa
 * PSB ({@link CalonSiswa}) pada modul sekolah. Berbeda dari
 * {@link ais.action.master.recruitment.helper.DefaultNisGenerator} (untuk pegawai), nomor di sini
 * dihitung per <b>sekolah + tahun masuk</b>: dasar hitungannya adalah jumlah {@link Siswa} aktif
 * (bernama, terdaftar di sekolah) pada sekolah dan tahun masuk yang sama, diformat 5 digit, lalu
 * dicek keunikannya terhadap kolom {@code nomorInduk}. Bila bentrok, method memanggil dirinya
 * sendiri secara rekursif dengan nomor yang bentrok ditambahkan ke daftar pengecualian — TIDAK ada
 * batas jumlah percobaan (berbeda dari varian pegawai yang membatasi {@code MAX_ATTEMPT}), sehingga
 * secara teori dapat rekursi cukup dalam bila banyak nomor bentrok berurutan.
 */
public class DefaultNisGenerator implements NisGenerator {

	/** Seperti {@link #generateNis(CalonSiswa, List)} tanpa daftar nomor yang harus dihindari. */
	@Override
	public String generateNis(CalonSiswa calonSiswa) {
		return generateNis(calonSiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan nomor induk siswa baru untuk {@code calonSiswa}, unik dalam lingkup sekolah dan
	 * tahun masuk yang sama, menghindari nomor pada {@code jumlahPengecualian}. Rekursif: bila
	 * nomor yang dihasilkan sudah terpakai, nomor tersebut ditambahkan ke daftar pengecualian dan
	 * method memanggil dirinya sendiri untuk mencoba nomor berikutnya.
	 *
	 * @param calonSiswa          calon siswa yang akan diberi nomor induk; menentukan sekolah dan
	 *                            tahun masuk sebagai lingkup keunikan
	 * @param jumlahPengecualian  daftar nomor yang harus dihindari (dimutasi langsung oleh method
	 *                            ini saat terjadi bentrokan), tidak boleh {@code null}
	 * @return nomor induk siswa 5 digit yang unik dalam lingkup sekolah + tahun masuk
	 */
	@Override
	public String generateNis(CalonSiswa calonSiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonSiswa.getTahunMasuk();

		Session session = HibernateUtil.currentNativeSession();
		Long jumlah = ((Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
				.add(Restrictions.eq("sekolah", calonSiswa.getSekolah())).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunMasuk", tahun)).setMaxResults(1).uniqueResult()).longValue();

		jumlah += jumlahPengecualian.size();
		String digitKesembilandst = "000000000000" + (jumlah + 1);
		digitKesembilandst = digitKesembilandst.substring(digitKesembilandst.length() - 5);

		String nomorInduk = digitKesembilandst;

		Integer count = ((Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", nomorInduk))
				.setProjection(Projections.count("nomorInduk")).uniqueResult()).intValue();

		HibernateUtil.closeSession();

		if (!count.equals(0)) {
			jumlahPengecualian.add(nomorInduk);
			return generateNis(calonSiswa, jumlahPengecualian);
		}

		return nomorInduk;
	}

}
