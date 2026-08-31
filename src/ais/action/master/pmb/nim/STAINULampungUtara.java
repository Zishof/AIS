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
 * Implementasi {@link NimGenerator} khusus institusi STAINU Lampung Utara: NIM disusun dari 2 digit
 * terakhir tahun masuk, kode program studi kelulusan, kode perguruan tinggi, lalu 3 digit nomor urut
 * sekuensial dihitung dari jumlah mahasiswa aktif pada kombinasi (tahun angkatan, jenjang, jurusan)
 * yang sama. Mengembalikan string kosong bila calon mahasiswa belum punya program studi kelulusan.
 * Keunikan diverifikasi ulang terhadap {@link Mahasiswa} dan tabrakan ditangani rekursif via daftar
 * pengecualian, sama seperti generator NIM lain di paket ini.
 */
public class STAINULampungUtara implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM berformat {@code [2 digit tahun][kode prodi][kode perguruan tinggi][3 digit
	 * urut]} untuk calon mahasiswa yang sudah memiliki program studi kelulusan; mengembalikan string
	 * kosong bila belum.
	 *
	 * @param calonMahasiswa      data calon mahasiswa, sumber tahun masuk, jenjang, dan program studi kelulusan
	 * @param jumlahPengecualian  daftar NIM yang harus dihindari, dimodifikasi di tempat saat rekursi
	 * @return NIM yang belum terpakai, atau string kosong bila program studi kelulusan belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		if (calonMahasiswa.getProdiLulus() == null) {
			return "";
		}

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String kodeIain = calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null ? "--"
				: calonMahasiswa.getProdiLulus().getKode().trim();//
		String digitKetiga = calonMahasiswa.getProdiLulus().getFakultas().getPerguruanTinggi().getKodePerguruanTinggi();

		Session session = HibernateUtil.openSession();
		Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jenjang", calonMahasiswa.getJenjang()))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKeempat = "000000000000" + (jumlah + 1);
		digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		// System.out.println("digit kedua (kode tahun) = " + digitKedua);
		System.out.println("digit kedua (kode prodi) = " + kodeIain);
		System.out.println("digit ketiga (kode institusi) = " + digitKetiga);
		System.out.println("digit keempat (urutan) = " + digitKeempat);

		String nim = digitPertama + /* digitKedua + */ kodeIain + digitKetiga + digitKeempat;

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
