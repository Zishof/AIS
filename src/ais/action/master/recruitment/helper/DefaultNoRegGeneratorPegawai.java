package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.recruitment.CalonPegawai;

/**
 * Implementasi baku algoritma penomoran nomor registrasi calon pegawai ({@link CalonPegawai}) pada
 * modul rekrutmen. Basis nomor diambil dari id maksimum {@link CalonPegawai} saat ini, jumlah
 * digit dapat dikonfigurasi lewat {@code jumlah_increments_no_registrasi_pegawai} (default 5), dan
 * keunikan dicek terhadap kolom {@code nomorInduk}. Bila bentrok, dicoba nomor berikutnya hingga
 * {@link #MAX_ATTEMPT} kali sebelum menyerah dan mengembalikan nomor berbasis estimasi kasar.
 */
public class DefaultNoRegGeneratorPegawai implements NoRegGeneratorPegawai {

	/** Batas jumlah percobaan mencari nomor yang belum terpakai sebelum menyerah pada estimasi kasar. */
	private static final int MAX_ATTEMPT = 10000;

	/** Seperti {@link #generateNoReg(List, CalonPegawai)} tanpa daftar nomor yang harus dihindari. */
	@Override
	public String generateNoReg(CalonPegawai calonPegawai) {
		return generateNoReg(new ArrayList<String>(), calonPegawai);
	}

	/**
	 * Menghasilkan nomor registrasi baru untuk {@code calonPegawai} yang belum dipakai calon
	 * pegawai lain, menghindari nomor pada {@code jumlahPengecualian}. Lihat javadoc kelas untuk
	 * algoritma lengkap.
	 *
	 * @param jumlahPengecualian daftar nomor yang harus dihindari, boleh {@code null}
	 * @param calonPegawai       calon pegawai yang akan diberi nomor registrasi
	 * @return nomor registrasi (jumlah digit sesuai konfigurasi) yang, dalam batas
	 *         {@link #MAX_ATTEMPT} percobaan, belum dipakai
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, CalonPegawai calonPegawai) {
		List<String> pengecualian = jumlahPengecualian == null ? new ArrayList<String>() : jumlahPengecualian;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Number maxId = (Number) session.createCriteria(CalonPegawai.class).setProjection(Projections.max("id"))
					.uniqueResult();
			long dasar = maxId == null ? 0L : maxId.longValue();
			int digit = ambilJumlahDigit();
			for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
				String noReg = formatNomor(dasar + pengecualian.size() + attempt + 1, digit);
				if (pengecualian.contains(noReg)) {
					continue;
				}
				Number count = (Number) session.createCriteria(CalonPegawai.class)
						.add(Restrictions.eq("nomorInduk", noReg)).setProjection(Projections.rowCount()).uniqueResult();
				if (count == null || count.intValue() == 0) {
					return noReg;
				}
				pengecualian.add(noReg);
			}
			return formatNomor(dasar + pengecualian.size() + 1, digit);
		} finally {
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	/** Membaca jumlah digit nomor registrasi dari konfigurasi {@code jumlah_increments_no_registrasi_pegawai}, default 5 bila konfigurasi kosong/tidak valid. */
	private int ambilJumlahDigit() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("jumlah_increments_no_registrasi_pegawai", "5").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 5;
		}
	}

	/** Mem-padding {@code nomor} dengan nol di depan hingga {@code digit} digit. */
	private String formatNomor(long nomor, int digit) {
		String hasil = "00000000000000000000000000000000000000" + nomor;
		return hasil.substring(hasil.length() - digit);
	}
}
