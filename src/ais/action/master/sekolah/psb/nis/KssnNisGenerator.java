package ais.action.master.sekolah.psb.nis;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Algoritma penomoran Nomor Induk Siswa (NIS) khusus institusi KSSN: NIS dibentuk dari NSS
 * (Nomor Statistik Sekolah) sekolah tujuan digabung 3 digit urut siswa baru pada tahun masuk yang
 * sama, dengan pengecekan tabrakan dan percobaan ulang rekursif.
 */
public class KssnNisGenerator implements NisGenerator {

	/** Seperti {@link #generateNis(CalonSiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNis(CalonSiswa calonSiswa) {
		return generateNis(calonSiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIS: menghitung jumlah {@link Siswa} aktif (bernama, punya sekolah) di sekolah
	 * yang sama pada tahun masuk yang sama (ditambah jumlah kandidat yang sudah ditolak di
	 * {@code jumlahPengecualian}), lalu menyusun NIS sebagai {@code NSS sekolah + 3 digit urut}
	 * (dipad nol di depan). Bila NIS hasil ternyata sudah dipakai siswa lain, nomor tersebut
	 * ditambahkan ke {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara
	 * rekursif untuk mencoba nomor urut berikutnya.
	 *
	 * @param jumlahPengecualian NIS kandidat yang sudah terbukti bentrok pada percobaan sebelumnya, ikut menggeser nomor urut berikutnya
	 * @return NIS yang belum dipakai siswa manapun
	 */
	@Override
	public String generateNis(CalonSiswa calonSiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonSiswa.getTahunMasuk();

		Session session = HibernateUtil.currentNativeSession();
		Long jumlah = ((Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
				.add(Restrictions.eq("sekolah", calonSiswa.getSekolah())).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunMasuk", tahun)).setMaxResults(1).uniqueResult()).longValue();

		jumlah += jumlahPengecualian.size();
		String digitKesembilandst = "000000000000" + (jumlah + 1);
		digitKesembilandst = digitKesembilandst.substring(digitKesembilandst.length() - 3);

		String nomorInduk = calonSiswa.getSekolah().getNss() + digitKesembilandst;

		Integer count = ((Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", nomorInduk))
				.setProjection(Projections.count("nomorInduk")).uniqueResult()).intValue();

		HibernateUtil.closeSession();

		if (!count.equals(0)) {
			jumlahPengecualian.add(nomorInduk);
			return generateNis(calonSiswa, jumlahPengecualian);
		}

		return nomorInduk;
	}

}
