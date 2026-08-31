package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma penomoran nomor registrasi (pendaftaran, bukan NIM final) khusus institusi "STTIND":
 * menyusun nomor dari kode tahun (2 digit terakhir) dan nomor urut 4 digit dihitung dari jumlah
 * {@link BiodataCalonMahasiswa} aktif yang nomor registrasinya sudah berawalan kode tahun tersebut
 * (dicocokkan dengan {@code ilike ... START}).
 */
public class SttindNoRegGenerator implements NoRegGenerator {

	/** Menghasilkan nomor registrasi tanpa daftar pengecualian awal; lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menyusun nomor registrasi dari kode tahun (2 digit) dan nomor urut 4 digit (jumlah
	 * pendaftar aktif berawalan kode tahun yang sama, ditambah ukuran {@code jumlahPengecualian}).
	 * Bila nomor hasil sudah terpakai, memanggil diri sendiri secara rekursif dengan nomor
	 * tersebut ditambahkan ke {@code jumlahPengecualian}.
	 *
	 * @param jumlahPengecualian     daftar nomor registrasi yang harus dilewati (bertambah saat rekursi)
	 * @param biodataCalonMahasiswa data calon mahasiswa (tahun pendaftaran)
	 * @return nomor registrasi yang belum terpakai
	 */
	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - 4);

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
