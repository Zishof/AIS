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
 * Implementasi {@link NimGenerator} khusus institusi Bina Insani: NIM disusun dari tahun masuk penuh
 * (4 digit), bagian kedua kode program studi kelulusan (dipisah tanda {@code "-"}, mis.
 * {@code "S1-TI"} menjadi {@code "TI"}), lalu nomor urut sekuensial yang panjangnya dapat
 * dikonfigurasi lewat {@code jumlah_digit_gen_nim_mahasiswa} (default 4 digit), dihitung dari jumlah
 * mahasiswa aktif pada kombinasi (tahun angkatan, jurusan) yang sama. Mengembalikan string kosong
 * bila calon mahasiswa belum punya {@code prodiLulus}. Keunikan diverifikasi ulang terhadap
 * {@link Mahasiswa} dan tabrakan ditangani rekursif via daftar pengecualian.
 */
public class BinaInsaniNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM berformat {@code [4 digit tahun][bagian kedua kode prodi][N digit urut]}
	 * (N dari konfigurasi {@code jumlah_digit_gen_nim_mahasiswa}) untuk calon mahasiswa yang sudah
	 * memiliki program studi kelulusan; mengembalikan string kosong bila belum.
	 *
	 * @param calonMahasiswa      data calon mahasiswa, sumber tahun masuk dan program studi kelulusan
	 * @param jumlahPengecualian  daftar NIM yang harus dihindari, dimodifikasi di tempat saat rekursi
	 * @return NIM yang belum terpakai, atau string kosong bila {@code prodiLulus} belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {
		if (calonMahasiswa.getProdiLulus() != null) {
			Integer tahun = calonMahasiswa.getTahun();
			String digitPertama = tahun.toString();

			String digitKedua = calonMahasiswa.getProdiLulus().getKode();
			try {
				digitKedua = digitKedua.split("-")[1];
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/BinaInsaniNimGenerator.java:32");

			}
			
			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/BinaInsaniNimGenerator.java:39");

			}

			Session session = HibernateUtil.openSession();
			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (jumlah + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - jumlahDigit);

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
		} else {
			return "";
		}
	}

}
