package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

/**
 * Algoritma penomoran NIM khusus institusi "STTIND": menyusun NIM dari kode tahun masuk (2 digit
 * terakhir), prefix tetap {@code "100"} digabung kode prodi (atau {@code "-"} bila prodi tidak
 * diketahui), dan nomor urut 3 digit mahasiswa aktif tahun angkatan &amp; prodi yang sama. Berbeda
 * dari generator lain di paket ini, method ini tidak mengecek {@code prodiLulus == null} sebelum
 * memakainya di query — bila null, query prodi akan gagal alih-alih mengembalikan {@code "-"}.
 */
public class SttindNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian awal; lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menyusun NIM dari tahun (2 digit), prefix+kode prodi, dan nomor urut 3 digit (jumlah
	 * mahasiswa aktif tahun &amp; prodi sama ditambah ukuran {@code jumlahPengecualian}). Bila NIM
	 * hasil sudah terpakai, memanggil diri sendiri secara rekursif dengan NIM tersebut ditambahkan
	 * ke {@code jumlahPengecualian}.
	 *
	 * @param calonMahasiswa     data calon mahasiswa (tahun masuk, prodi lulus)
	 * @param jumlahPengecualian daftar NIM yang harus dilewati (bertambah saat rekursi)
	 * @return NIM yang belum terpakai
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String digitKedua = calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null ? "-"
				: calonMahasiswa.getProdiLulus().getKode();

		digitKedua = "100" + digitKedua;

		Session session = HibernateUtil.openSession();
		Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKetiga = "000000000000" + (jumlah + 1);
		digitKetiga = digitKetiga.substring(digitKetiga.length() - 3);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode prodi) = " + digitKedua);
		System.out.println("digit ketiga (urutan) = " + digitKetiga);

		String nim = digitPertama + digitKedua + digitKetiga;

		Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
				.setProjection(Projections.count("nim")).uniqueResult()).intValue();

		HibernateUtil.closeSessionQuietly(session);

		if (!count.equals(0)) {
			jumlahPengecualian.add(nim);
			return generateNim(calonMahasiswa, jumlahPengecualian);
		}

		return nim;
	}

}
