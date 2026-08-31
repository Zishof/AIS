package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.recruitment.CalonPegawai;

/**
 * Implementasi baku algoritma penomoran nomor registrasi calon pegawai ({@link CalonPegawai}) pada
 * modul rekrutmen. Basis nomor diambil dari nomor registrasi terbesar yang sudah tersimpan,
 * bukan dari id data, agar nomor tidak meloncat ketika ada data lain yang tidak ikut format nomor.
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
		Session session = HibernateUtil.openSession();
		try {
			int digit = ambilJumlahDigit();
			long dasar = RecruitmentNumberGeneratorSupport.nomorRegistrasiBerikutnya(session, digit, calonPegawai,
					pengecualian) - 1;
			for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
				String noReg = formatNomor(dasar + pengecualian.size() + attempt + 1, digit);
				if (pengecualian.contains(noReg)) {
					continue;
				}
				if (!RecruitmentNumberGeneratorSupport.nomorRegistrasiSudahDipakai(session, noReg, calonPegawai)) {
					return noReg;
				}
				pengecualian.add(noReg);
			}
			return formatNomor(dasar + pengecualian.size() + 1, digit);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
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
