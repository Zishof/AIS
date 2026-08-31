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
 * Algoritma penomoran NIM khusus institusi "Akfarsam": menyusun NIM dari kode tahun masuk (2 digit
 * terakhir), kode institusi/prodi tetap {@code "114054"}, dan nomor urut 4 digit yang diturunkan
 * dari 3 digit terakhir NIM tertinggi ({@code MAX(nim)}) mahasiswa aktif prodi yang sama
 * (berbeda dari generator lain yang menghitung jumlah baris — pendekatan ini mengasumsikan NIM
 * lama selalu berurutan naik). Bila prodi lulus tidak diketahui, mengembalikan {@code "-"}.
 */
public class AkfarsamNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian awal; lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menyusun NIM dari tahun (2 digit), kode institusi tetap, dan nomor urut 4 digit yang diambil
	 * dari 3 digit terakhir NIM tertinggi milik prodi yang sama ditambah 1 (plus ukuran
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

			String digitPertama = tahun.toString().substring(2);

			String digitKedua = "114054";

			String maxNim = ((String) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.max("nim"))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult());
			Integer n = Integer.parseInt(maxNim == null ? "0" : maxNim.trim().substring(maxNim.length() - 3));

			n += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (n + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - 4);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = digitPertama + digitKedua + digitKetiga;

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
