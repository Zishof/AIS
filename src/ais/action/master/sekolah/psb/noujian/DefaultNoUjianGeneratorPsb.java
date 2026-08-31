package ais.action.master.sekolah.psb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.CommonPSB;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.ui.util.MyMessageboxConfig;

/**
 * Algoritma pembangkit nomor ujian PSB bawaan, sekaligus penetap ruang ujian
 * ({@link RuangPSB}) untuk calon siswa. Alur kerja: (1) bila calon siswa sudah punya nomor ujian,
 * kembalikan langsung tanpa proses ulang; (2) cari {@link RuangPSB} yang belum penuh pada
 * gelombang pendaftaran calon siswa; (3) bila tidak ada ruang tersedia atau ruang yang dipilih
 * sudah penuh, tampilkan pesan informasi dan kembalikan string kosong; (4) bentuk nomor ujian dari
 * 2 digit terakhir tahun masuk + digit urutan (panjang dari konfigurasi
 * {@code jumlah_increments_no_ujian_psb}, default 8), dihitung lewat
 * {@link NoUjianGeneratorPsbSupport#nomorUrutBerikutnya} sebagai angka setelah urutan tertinggi
 * yang sudah dipakai di antara nomor ujian ber-prefix tahun masuk yang sama, dan langsung
 * menyimpannya ke {@code calonSiswa}; (5) bila nomor ternyata sudah dipakai calon siswa lain
 * (dicek lewat {@link NoUjianGeneratorPsbSupport#nomorSudahDipakai}), coba lagi secara rekursif;
 * (6) setelah berhasil, tetapkan penempatan ruang ujian lewat
 * {@link CommonPSB#dapatkanRuangUjian(CalonSiswa)}.
 */
public class DefaultNoUjianGeneratorPsb implements NoUjianGeneratorPsb {

	/** @return nomor ujian untuk {@code calonSiswa}, lihat {@link #generateNoUjian(CalonSiswa, List)}. */
	@Override
	public String generateNoUjian(CalonSiswa calonSiswa) throws Exception {
		return generateNoUjian(calonSiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan (atau mengembalikan nomor ujian yang sudah ada) sekaligus menetapkan ruang
	 * ujian calon siswa. Lihat javadoc kelas untuk alur lengkap.
	 *
	 * @param calonSiswa          calon siswa yang akan diberi nomor ujian
	 * @param jumlahPengecualian  nomor yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @return nomor ujian yang tersimpan, atau string kosong bila kuota/ruang ujian tidak tersedia
	 */
	@Override
	public String generateNoUjian(CalonSiswa calonSiswa, List<String> jumlahPengecualian) throws Exception {

		if (calonSiswa.getNoUjian() != null && !calonSiswa.getNoUjian().trim().isEmpty()) {
			return calonSiswa.getNoUjian().trim();
		}

		Session session = HibernateUtil.currentSession();
		Long idmin = (Long) session.createCriteria(RuangPSB.class).createAlias("ujianPSB", "ujianPSB")
				.add(Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPSB.gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.setProjection(Projections.min("id")).uniqueResult();

		if (idmin == null) {
			MyMessageboxConfig.show(
					"Kuota / Ruangan ujian untuk gelombang " + calonSiswa.getGelombangPendaftaranPsb()
							+ " tahun penerimaan siswa baru " + calonSiswa.getGelombangPendaftaranPsb()
							+ " tidak ditemukan atau sudah penuh",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		RuangPSB ruangSelected = (RuangPSB) session.createCriteria(RuangPSB.class).add(Restrictions.idEq(idmin))
				.uniqueResult();

		Number t = ((Number) (session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
				.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.noUjian", ""))
				.add(Restrictions.isNotNull("calonSiswa.noUjian")).add(Restrictions.eq("ruangPSB", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult())).intValue();
		int isiRuang = t == null ? 0 : t.intValue();
		String noUjianFinal = "";

		if (isiRuang < ruangSelected.getKapasitasRuangan()) {

			// String noUjian = "00000000000000000000000000000000000000" +
			// calonSiswa.getId();
			String digitSatuDanDua = calonSiswa.getTahunMasuk().toString().substring(2, 4);

			Integer jumlahIncrements = 8;
			try {
				jumlahIncrements = Integer
						.parseInt(Common.getKonfigurasi("jumlah_increments_no_ujian_psb", "8").getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			long nomorUrut = NoUjianGeneratorPsbSupport.nomorUrutBerikutnya(session, digitSatuDanDua,
					jumlahIncrements, calonSiswa, jumlahPengecualian);
			String noUjian = NoUjianGeneratorPsbSupport.leftPadNomor(nomorUrut, jumlahIncrements);
			System.out.println("noUjian => " + noUjian);
			noUjianFinal = digitSatuDanDua + noUjian;

			session.refresh(calonSiswa);
			calonSiswa.setNoUjian(noUjianFinal);
			Common.refreshUpdate(session, calonSiswa);

			System.out.println("noUjianFinal => " + noUjianFinal);

		} else {
			MyMessageboxConfig.show("Kuota / Ruangan " + ruangSelected + " telah penuh", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		boolean nomorSudahDipakai = NoUjianGeneratorPsbSupport.nomorSudahDipakai(session, noUjianFinal, calonSiswa);

		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noUjianFinal);
			return generateNoUjian(calonSiswa, jumlahPengecualian);
		} else {

			CommonPSB.dapatkanRuangUjian(calonSiswa);

			return noUjianFinal;
		}

	}

}
