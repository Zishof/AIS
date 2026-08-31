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
 * Algoritma penomoran NIM dengan pola {@code YY-PREFIX-URUT}: NIM disusun dari
 * {@code 2 digit tahun masuk + prefix konfigurasi ({@code prefix_center_pmb}) + nomor urut
 * mahasiswa pada angkatan yang sama}, panjang nomor urut dapat dikonfigurasi lewat
 * {@code jumlah_digit_gen_nim_mahasiswa} (default 4 digit). Prefix bersifat institusi-lebar
 * (tidak membedakan program studi), cocok untuk institusi dengan satu skema penomoran global.
 */
public class YY_PREFIX_URUT_NimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM: menghitung jumlah {@link Mahasiswa} aktif pada angkatan (tahun) yang sama
	 * (ditambah jumlah kandidat yang sudah ditolak di {@code jumlahPengecualian}), lalu menyusun
	 * NIM dari 2 digit tahun, prefix konfigurasi, dan nomor urut yang di-pad nol. Bila NIM hasil
	 * ternyata sudah dipakai mahasiswa lain, nomor tersebut ditambahkan ke
	 * {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara rekursif.
	 * Mengembalikan {@code "-"} bila calon mahasiswa belum punya program studi lulus.
	 *
	 * @param jumlahPengecualian NIM kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return NIM yang belum dipakai mahasiswa manapun, atau {@code "-"} bila prodi lulus belum ditentukan
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);

			String digitKedua = Common.getKonfigurasi("prefix_center_pmb", "").getNilai();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun)).setMaxResults(1).uniqueResult()).longValue();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_PREFIX_URUT_NimGenerator.java:43");

			}

			jumlah += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (jumlah + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - jumlahDigit);

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
