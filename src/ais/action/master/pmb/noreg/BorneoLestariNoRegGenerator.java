package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;

/**
 * Implementasi {@link NoRegGenerator} khusus institusi Borneo Lestari: nomor registrasi PMB disusun
 * dari kode fakultas program studi pilihan pertama + 2 digit terakhir tahun masuk (digit pertama),
 * diikuti 3 digit nomor urut sekuensial (digit kedua) yang dihitung dari jumlah calon mahasiswa
 * aktif yang nomor registrasinya sudah berawalan prefix yang sama. Sama seperti generator NIM/NoReg
 * lain di paket ini, keunikan diverifikasi ulang terhadap data tersimpan dan, bila bertabrakan,
 * nomor tersebut ditambahkan ke daftar pengecualian lalu method dipanggil ulang secara rekursif.
 */
public class BorneoLestariNoRegGenerator implements NoRegGenerator {

	/** Menghasilkan nomor registrasi tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi berformat {@code [kode fakultas][2 digit tahun][3 digit urut]}
	 * untuk calon mahasiswa, menghindari nilai dalam {@code jumlahPengecualian} maupun yang sudah
	 * tersimpan di database (rekursif bila bertabrakan).
	 *
	 * @param jumlahPengecualian daftar nomor registrasi yang harus dihindari, dimodifikasi di tempat saat rekursi
	 * @param biodataCalonMahasiswa data calon mahasiswa, sumber tahun masuk dan program studi pilihan pertama
	 * @return nomor registrasi yang belum terpakai
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		Jurusan jurusan = biodataCalonMahasiswa.getProdi1();
		String digitPertama = (jurusan == null ? "" : jurusan.getFakultas().getKode()) + tahun.toString().substring(2);

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - 3);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode urutan) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noRegistrasi", noReg)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
		if (!count.equals(0)) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
