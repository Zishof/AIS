package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.recruitment.CalonPegawai;

/**
 * Implementasi baku (generik, tidak spesifik institusi) algoritma penomoran NIS (nomor induk)
 * untuk calon pegawai ({@link CalonPegawai}) pada modul rekrutmen. Nomor dibentuk dari total baris
 * {@link Pegawai} saat ini ditambah urutan percobaan, diformat 5 digit dengan padding nol di
 * depan, lalu dicek keunikannya terhadap kolom {@code code} pada {@link Pegawai}; bila sudah
 * terpakai, dicoba nomor berikutnya hingga {@link #MAX_ATTEMPT} kali sebelum menyerah dan
 * mengembalikan nomor berbasis estimasi kasar (jumlah + pengecualian + 1) tanpa jaminan unik lagi.
 */
public class DefaultNisGenerator implements NisGenerator {

	/** Batas jumlah percobaan mencari nomor yang belum terpakai sebelum menyerah pada estimasi kasar. */
	private static final int MAX_ATTEMPT = 10000;

	/** Seperti {@link #generateNis(CalonPegawai, List)} tanpa daftar nomor yang harus dihindari. */
	@Override
	public String generateNis(CalonPegawai calonPegawai) {
		return generateNis(calonPegawai, new ArrayList<String>());
	}

	/**
	 * Menghasilkan nomor induk baru untuk {@code calonPegawai} yang belum dipakai pegawai lain,
	 * menghindari nomor-nomor pada {@code jumlahPengecualian} (dipakai saat membuat banyak nomor
	 * sekaligus dalam satu batch sebelum masing-masing tersimpan ke database). Lihat javadoc kelas
	 * untuk algoritma lengkap.
	 *
	 * @param calonPegawai        calon pegawai yang akan diberi nomor induk
	 * @param jumlahPengecualian  daftar nomor yang harus dihindari, boleh {@code null}
	 * @return nomor induk 5 digit yang (dalam batas {@link #MAX_ATTEMPT} percobaan) belum dipakai
	 */
	@Override
	public String generateNis(CalonPegawai calonPegawai, List<String> jumlahPengecualian) {
		List<String> pengecualian = jumlahPengecualian == null ? new ArrayList<String>() : jumlahPengecualian;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Number jumlahData = (Number) session.createCriteria(Pegawai.class).setProjection(Projections.rowCount())
					.setMaxResults(1).uniqueResult();
			long jumlah = jumlahData == null ? 0L : jumlahData.longValue();
			for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
				String nomorInduk = formatNomor(jumlah + pengecualian.size() + attempt + 1, 5);
				if (pengecualian.contains(nomorInduk)) {
					continue;
				}
				Number count = (Number) session.createCriteria(Pegawai.class).add(Restrictions.eq("code", nomorInduk))
						.setProjection(Projections.count("code")).uniqueResult();
				if (count == null || count.intValue() == 0) {
					return nomorInduk;
				}
				pengecualian.add(nomorInduk);
			}
			return formatNomor(jumlah + pengecualian.size() + 1, 5);
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/** Mem-padding {@code nomor} dengan nol di depan hingga {@code digit} digit. */
	private String formatNomor(long nomor, int digit) {
		String hasil = "00000000000000000000" + nomor;
		return hasil.substring(hasil.length() - digit);
	}
}
