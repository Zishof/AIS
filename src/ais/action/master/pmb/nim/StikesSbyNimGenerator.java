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
 * Algoritma penomoran NIM khusus institusi STIKES Surabaya: NIM disusun dari
 * {@code 2 digit tahun masuk + kode program studi + 1 digit penanda mahasiswa pindahan
 * ("2") atau baru ("1") + 3 digit nomor urut} pada kombinasi tahun angkatan, jenjang, dan
 * program studi yang sama.
 */
public class StikesSbyNimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM: menghitung jumlah {@link Mahasiswa} aktif pada kombinasi tahun angkatan,
	 * jenjang, dan program studi lulus yang sama (ditambah jumlah kandidat yang sudah ditolak di
	 * {@code jumlahPengecualian}), lalu menyusun NIM dari 2 digit tahun, kode prodi (atau
	 * {@code "--"} bila belum ditentukan), penanda pindahan/baru, dan 3 digit nomor urut. Bila NIM
	 * hasil ternyata sudah dipakai mahasiswa lain, nomor tersebut ditambahkan ke
	 * {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara rekursif.
	 *
	 * @param jumlahPengecualian NIM kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return NIM yang belum dipakai mahasiswa manapun
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa,
			List<String> jumlahPengecualian) {

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String digitKedua = calonMahasiswa == null
				|| calonMahasiswa.getProdiLulus() == null ? "--"
				: calonMahasiswa.getProdiLulus().getKode().trim();

		String kodePesertaDidikBaru = calonMahasiswa.getMerupakanPindahan() ? "2"
				: "1";

		Session session = HibernateUtil.openSession();
		Long jumlah = ((Number) session
				.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jenjang", calonMahasiswa.getJenjang()))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
				.setMaxResults(1).uniqueResult()).longValue();

		jumlah += jumlahPengecualian.size();
		String digitKetiga = "000000000000" + (jumlah + 1);
		digitKetiga = digitKetiga.substring(digitKetiga.length() - 3);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode prodi) = " + digitKedua);
		System.out.println("digit kedua (kodePesertaDidikBaru) = "
				+ kodePesertaDidikBaru);
		System.out.println("digit ketiga (urutan) = " + digitKetiga);

		String nim = digitPertama + digitKedua + kodePesertaDidikBaru
				+ digitKetiga;

		Integer count = ((Number) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.eq("nim", nim))
				.setProjection(Projections.count("nim")).uniqueResult())
				.intValue();

		HibernateUtil.closeSessionQuietly(session);

		if (!count.equals(0)) {
			jumlahPengecualian.add(nim);
			return generateNim(calonMahasiswa, jumlahPengecualian);
		}

		return nim;
	}

}
