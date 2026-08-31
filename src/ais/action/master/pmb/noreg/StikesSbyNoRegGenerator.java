package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma nomor registrasi PMB khusus STIKES Surabaya. Format nomor registrasi:
 * {@code [prefix konfigurasi][4 digit tahun berjalan][5 digit nomor urut]}, mis. dengan prefix
 * default {@code 77883} menghasilkan {@code 778832026000123}. Prefix diambil dari konfigurasi
 * {@code prefix_no_registrasi} (default {@code "77883"}) sehingga dapat disesuaikan tanpa mengubah
 * kode. Nomor urut dihitung dari jumlah {@link BiodataCalonMahasiswa} aktif yang nomor
 * registrasinya (tanpa prefix) sudah berawalan tahun berjalan, ditambah jumlah kandidat yang sudah
 * dicoba tapi bentrok pada pemanggilan rekursif, lalu ditambah 1 dan dipad nol ke kiri menjadi 5
 * digit. Bila hasil gabungan (dengan prefix) ternyata sudah dipakai calon mahasiswa aktif lain,
 * nomor tersebut dicatat sebagai pengecualian dan method memanggil dirinya sendiri untuk mencoba
 * nomor berikutnya.
 */
public class StikesSbyNoRegGenerator implements NoRegGenerator {

	/** Menghasilkan nomor registrasi baru tanpa daftar pengecualian awal — lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi berformat {@code [prefix][tahun][5 digit urut]}, menghindari
	 * nomor yang ada di {@code jumlahPengecualian} maupun yang sudah dipakai calon mahasiswa aktif
	 * lain di database; mencoba ulang secara rekursif bila terjadi bentrok.
	 *
	 * @param jumlahPengecualian nomor-nomor yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @param biodataCalonMahasiswa calon mahasiswa target (tidak dipakai langsung dalam perhitungan
	 *                           nomor, hanya diteruskan untuk kompatibilitas kontrak)
	 * @return nomor registrasi baru yang belum pernah dipakai
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		String digitPertama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";

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

		String prefixNoRegistrasi = Common.getKonfigurasi("prefix_no_registrasi", "77883").getNilai().trim();
		noReg = prefixNoRegistrasi + noReg;

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
