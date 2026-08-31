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
 * Algoritma penomoran NIM khusus institusi STAHN Palangkaraya, berpola
 * {@code <YY tahun angkatan><kode prodi><urut 3 digit>} (tanpa segmen semester/program seperti
 * generator umum). Nomor urut dihitung dari jumlah {@link Mahasiswa} aktif yang sudah terdaftar pada
 * kombinasi (tahun angkatan, jurusan) yang sama ditambah 1, BUKAN lewat {@link NimGeneratorSupport}
 * — kelas ini mandiri, hanya mengecek duplikasi terhadap tabel {@code mahasiswa} (tidak mengecek
 * {@code biodata_calon_mahasiswa} lain seperti helper umum).
 */
public class StahnPalangkarayaNimGenerator implements NimGenerator {

	/** Varian ringkas {@link #generateNim(BiodataCalonMahasiswa, List)} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM untuk {@code calonMahasiswa} sesuai pola kelas ini (lihat javadoc kelas).
	 * Mengembalikan {@code "-"} bila calon mahasiswa belum punya prodi lulus. Rekursif: bila NIM
	 * yang dihasilkan ternyata sudah dipakai mahasiswa lain, dicoba lagi dengan
	 * {@code jumlahPengecualian} bertambah satu (menggeser nomor urut).
	 *
	 * @param jumlahPengecualian jumlah percobaan gagal sebelumnya (dipakai sebagai offset tambahan nomor urut)
	 * @return NIM yang dihasilkan, atau {@code "-"} bila prodi lulus belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitKedua = "";

			String digitPertama = tahun.toString().substring(2);

			String digitKetiga = calonMahasiswa.getProdiLulus().getKode();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (jumlah + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kosong) = " + digitKedua);
			System.out.println("digit ketiga (kode prodi) = " + digitKetiga);
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
