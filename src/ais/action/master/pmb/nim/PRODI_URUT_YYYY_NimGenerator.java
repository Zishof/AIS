package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

/**
 * Algoritma pembangkit NIM dengan urutan komponen "urutan lalu prodi lalu tahun" (nama kelas
 * mengikuti urutan komponen sumber, {@code PRODI_URUT_YYYY}, tetapi susunan akhir NIM adalah
 * prefix konfigurasi + urutan + kode prodi + tahun — lihat implementasi). Format NIM: prefix dari
 * konfigurasi {@code prefix_pmb} (boleh kosong) + digit urutan (panjang dari konfigurasi
 * {@code jumlah_digit_gen_nim_mahasiswa}, default 4) mahasiswa aktif pada kombinasi (tahun
 * angkatan, prodi) yang sama + kode prodi lulus + 4 digit tahun angkatan. Mengembalikan
 * {@code "-"} bila calon mahasiswa belum memiliki prodi lulus.
 */
public class PRODI_URUT_YYYY_NimGenerator implements NimGenerator {

	/** @return NIM baru untuk {@code calonMahasiswa}, lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM format prefix+urutan+prodi+tahun; mengembalikan {@code "-"} bila
	 * {@code calonMahasiswa} belum memiliki prodi lulus. Bila hasil sudah dipakai, NIM tersebut
	 * ditambahkan ke {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara
	 * rekursif untuk mencoba urutan berikutnya.
	 *
	 * @param calonMahasiswa      data calon mahasiswa yang akan diberi NIM
	 * @param jumlahPengecualian  NIM yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @return NIM baru yang belum pernah dipakai, atau {@code "-"} bila prodi lulus belum diisi
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();
			
			
			
			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString();

			String digitKedua = calonMahasiswa.getProdiLulus().getKode();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
					.longValue();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/PRODI_URUT_YYYY_NimGenerator.java:48");

			}

			jumlah += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (jumlah + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - jumlahDigit);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = Common.getKonfigurasi("prefix_pmb", "").getNilai() + digitKetiga + digitKedua + digitPertama;

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
