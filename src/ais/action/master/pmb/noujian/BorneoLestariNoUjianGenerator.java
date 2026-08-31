package ais.action.master.pmb.noujian;

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
 * Algoritma penomoran Nomor Ujian calon mahasiswa khusus institusi Borneo Lestari, berpola
 * {@code <kode fakultas prodi pilihan 1><YY tahun angkatan><urut 3 digit>}. Nomor urut dihitung dari
 * jumlah {@link BiodataCalonMahasiswa} aktif yang {@code noRegistrasi}-nya sudah diawali prefix yang
 * sama, ditambah 1.
 *
 * <p>
 * <b>Catatan:</b> meski method ini bernama "NoUjian", nilai yang dihasilkan dihitung dan dicek
 * duplikasinya terhadap kolom {@link BiodataCalonMahasiswa#getNoRegistrasi()} (nomor registrasi),
 * BUKAN kolom nomor ujian terpisah — perilaku ini dipertahankan apa adanya sesuai cakupan
 * dokumentasi ini.
 * </p>
 */
public class BorneoLestariNoUjianGenerator implements NoUjianGenerator {

	/** Varian ringkas {@link #generateNoUjian(BiodataCalonMahasiswa, List)} tanpa daftar pengecualian awal. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan nomor ujian untuk {@code biodataCalonMahasiswa} sesuai pola kelas ini (lihat
	 * javadoc kelas). Rekursif: bila nomor yang dihasilkan ternyata sudah terpakai (dicek terhadap
	 * {@code noRegistrasi}), dicoba lagi dengan nomor tersebut ditambahkan ke {@code noRegPengecualian}.
	 *
	 * @param noRegPengecualian daftar nomor yang harus dianggap sudah terpakai (dimutasi dan diteruskan pada percobaan ulang)
	 * @return nomor ujian yang dihasilkan
	 */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> noRegPengecualian)
			throws Exception {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		Jurusan jurusan = biodataCalonMahasiswa.getProdi1();
		String digitPertama = (jurusan == null ? "" : jurusan.getFakultas().getKode()) + tahun.toString().substring(2);

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += noRegPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - 3);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode urutan) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noRegistrasi", noReg)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
		if (!count.equals(0)) {
			noRegPengecualian.add(noReg);
			return generateNoUjian(biodataCalonMahasiswa, noRegPengecualian);
		}

		return noReg;
	}

}
