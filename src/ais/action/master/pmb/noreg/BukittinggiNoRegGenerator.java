package ais.action.master.pmb.noreg;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma penomoran Nomor Registrasi calon mahasiswa khusus institusi Bukittinggi, berpola
 * {@code <yyyyMMdd tanggal sekarang><urut 5 digit>}: nomor urut dihitung dari jumlah
 * {@link BiodataCalonMahasiswa} aktif yang nomor registrasinya sudah diawali tanggal hari ini
 * (bukan tanggal daftar calon mahasiswa itu sendiri, melainkan tanggal SAAT generator dipanggil),
 * ditambah 1. Mandiri (tidak memakai {@link NoRegGeneratorSupport}); pengecekan duplikasi langsung
 * lewat query terhadap {@link BiodataCalonMahasiswa#getNoRegistrasi()}.
 */
public class BukittinggiNoRegGenerator implements NoRegGenerator {

	/** Formatter tanggal {@code yyyyMMdd} thread-local untuk segmen tanggal pada nomor registrasi. */
	static final ThreadLocal<SimpleDateFormat> format = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd");
		}
	};

	/** Varian ringkas {@link #generateNoReg(List, BiodataCalonMahasiswa)} tanpa daftar pengecualian awal. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi untuk {@code biodataCalonMahasiswa} sesuai pola kelas ini (lihat
	 * javadoc kelas). Rekursif: bila nomor yang dihasilkan ternyata sudah dipakai, dicoba lagi
	 * dengan nomor tersebut ditambahkan ke {@code jumlahPengecualian}.
	 *
	 * @param jumlahPengecualian daftar nomor registrasi yang harus dianggap sudah terpakai (dimutasi dan diteruskan pada percobaan ulang)
	 * @return nomor registrasi yang dihasilkan
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		String digitPertama = format.get().format(ais.ui.util.WaktuUtil.getDate());

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - 5);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		System.out.println("digit kedua (kode tahun) = " + digitKedua);

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
