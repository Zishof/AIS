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
 * Algoritma penomoran NIM pola "YYYY-FAKULTAS-PRODI-URUT": NIM disusun dari tahun masuk 4 digit
 * penuh, gabungan kode fakultas+kode prodi, dan nomor urut mahasiswa aktif tahun angkatan &amp;
 * prodi yang sama (jumlah digit urut dikonfigurasi lewat {@code jumlah_digit_gen_nim_mahasiswa},
 * default 4), ditambah prefix opsional dari konfigurasi {@code prefix_pmb}. Bila prodi lulus tidak
 * diketahui, mengembalikan {@code "-"}.
 */
public class YYYY_FAKULTAS_PRODI_URUT_NimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian awal; lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menyusun NIM dari tahun, kode fakultas+prodi, dan nomor urut mahasiswa (jumlah mahasiswa aktif
	 * tahun angkatan &amp; prodi sama, ditambah ukuran {@code jumlahPengecualian}, diformat sesuai
	 * jumlah digit terkonfigurasi). Bila NIM hasil sudah terpakai, memanggil diri sendiri secara
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

			String digitPertama = tahun.toString();

			String digitKedua = calonMahasiswa.getProdiLulus().getFakultas().getKode()
					+ calonMahasiswa.getProdiLulus().getKode();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YYYY_FAKULTAS_PRODI_URUT_NimGenerator.java:46");

			}

			jumlah += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (jumlah + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - jumlahDigit);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = Common.getKonfigurasi("prefix_pmb", "").getNilai() + digitPertama + digitKedua + digitKetiga;

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
