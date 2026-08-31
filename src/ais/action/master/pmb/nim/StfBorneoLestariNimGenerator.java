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
 * Algoritma penomoran NIM khusus institusi "STF Borneo Lestari": menyusun NIM dari kode prodi,
 * kode tahun masuk (2 digit terakhir), digit tetap {@code "2"} (penanda identitas S1), dan nomor
 * urut 3 digit mahasiswa aktif tahun angkatan &amp; prodi yang sama. Bila prodi lulus tidak
 * diketahui, mengembalikan {@code "-"}.
 */
public class StfBorneoLestariNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian awal; lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menyusun NIM dari kode prodi, tahun (2 digit), digit identitas S1 tetap, dan nomor urut 3
	 * digit (jumlah mahasiswa aktif tahun &amp; prodi sama ditambah ukuran
	 * {@code jumlahPengecualian}). Bila NIM hasil sudah terpakai, memanggil diri sendiri secara
	 * rekursif dengan NIM tersebut ditambahkan ke {@code jumlahPengecualian}.
	 *
	 * @param calonMahasiswa     data calon mahasiswa (tahun masuk, prodi lulus)
	 * @param jumlahPengecualian daftar NIM yang harus dilewati (bertambah saat rekursi)
	 * @return NIM yang belum terpakai, atau {@code "-"} bila prodi lulus tidak diketahui
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();
			String digitKedua = tahun.toString().substring(2);

			String digitPertama = calonMahasiswa.getProdiLulus().getKode();

			String digitKetiga = "2";
			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (jumlah + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

			System.out.println("digit pertama (kode angkatan) = " + digitPertama);
			System.out.println("digit kedua (kode tahun masuk) = " + digitKedua);
			System.out.println("digit ketiga (identitas S1) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			nim = digitPertama + digitKedua + digitKetiga + digitKeempat;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
					.setProjection(Projections.count("nim")).uniqueResult()).intValue();

			HibernateUtil.closeSessionQuietly(session);

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
