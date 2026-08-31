package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma pembangkit nomor registrasi PMB khusus Poltekkes Medan. Format nomor: kode
 * {@code gelombangPendaftaran} + 4 digit tahun berjalan + kode jumlah program studi yang boleh
 * diambil (dari {@code paket.jumlahProdiYgBolehDiambil}, default {@code "1"} bila paket tidak
 * ada) + 5 digit urutan pendaftar tahun berjalan. Mengembalikan {@code "-"} bila calon mahasiswa
 * belum memiliki gelombang pendaftaran (nomor tidak dapat dibentuk).
 */
public class PoltekkesMedanNoRegGenerator implements NoRegGenerator {

	/** @return nomor registrasi baru untuk {@code biodataCalonMahasiswa}, lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi format gelombang+tahun+kodeProdi+urutan; mengembalikan
	 * {@code "-"} bila {@code biodataCalonMahasiswa} belum memiliki gelombang pendaftaran. Bila
	 * hasil sudah dipakai, nomor tersebut ditambahkan ke {@code jumlahPengecualian} dan method
	 * memanggil dirinya sendiri secara rekursif untuk mencoba urutan berikutnya.
	 *
	 * @param jumlahPengecualian nomor yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @param biodataCalonMahasiswa data calon mahasiswa yang akan diberi nomor registrasi
	 * @return nomor registrasi yang belum pernah dipakai, atau {@code "-"} bila gelombang belum diisi
	 */
	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {

		if (biodataCalonMahasiswa.getGelombangPendaftaran() == null) {
			return "-";
		}

		String digitPertama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
						.longValue();

		String digitKedua = biodataCalonMahasiswa == null || biodataCalonMahasiswa.getPaket() == null ? "1"
				: biodataCalonMahasiswa.getPaket().getJumlahProdiYgBolehDiambil().toString();

		jumlah += jumlahPengecualian.size();
		String digitKetiga = "000000000000000" + (jumlah + 1);
		digitKetiga = digitKetiga.substring(digitKetiga.length() - 5);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode jumlah pilihan prodi) = " + digitKedua);
		System.out.println("digit ketiga (kode increment) = " + digitKetiga);

		String noReg = biodataCalonMahasiswa.getGelombangPendaftaran().getKode() + digitPertama + digitKedua
				+ digitKetiga;

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
