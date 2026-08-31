package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.CommonPegawai;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai;
import ais.database.model.recruitment.RuangPegawai;
import ais.ui.util.MyMessageboxConfig;

/**
 * Pembangkit nomor ujian rekrutmen pegawai sekaligus penetap ruang ujian bagi calon pegawai.
 * Selain menghasilkan nomor, method utamanya juga mencari ruang ujian yang masih tersedia untuk
 * gelombang pendaftaran calon pegawai, menetapkan nomor ujian pada entitas
 * {@link CalonPegawai}, dan mendaftarkan calon ke ruang tersebut lewat
 * {@code CommonPegawai#dapatkanRuangUjian} — sehingga generator ini memiliki efek samping
 * langsung ke database, tidak sekadar menghitung string.
 *
 * <p>
 * Alur singkat: (1) bila calon sudah punya nomor ujian, dikembalikan apa adanya (idempoten);
 * (2) dicari ruang ujian dengan kuota tersisa ({@code penuh=0}) untuk gelombang pendaftaran
 * calon, gagal bila tidak ada; (3) dipastikan ruang terpilih benar-benar belum penuh lewat
 * penghitungan ulang jumlah peserta yang sudah menempati ({@link
 * #ruangMasihTersedia(Session, RuangPegawai)}); (4) nomor unik dibentuk lewat
 * {@link #buatNomorUjianUnik(Session, List)} dan disimpan.
 * </p>
 */
public class DefaultNoUjianGeneratorPegawai implements NoUjianGeneratorPegawai {

	/** Batas maksimum percobaan pencarian nomor unik pada {@link #buatNomorUjianUnik(Session, List)} sebelum menyerah. */
	private static final int MAX_ATTEMPT = 10000;

	/** Membangkitkan nomor ujian baru untuk {@code calonPegawai} tanpa daftar pengecualian awal. */
	@Override
	public String generateNoUjian(CalonPegawai calonPegawai) throws Exception {
		return generateNoUjian(calonPegawai, new ArrayList<String>());
	}

	/**
	 * Membangkitkan nomor ujian untuk {@code calonPegawai}, mencari ruang ujian yang masih
	 * tersedia untuk gelombang pendaftarannya, lalu menyimpan nomor dan menetapkan ruang ujian
	 * pada calon. Mengembalikan nomor yang sudah ada tanpa proses ulang bila calon sudah
	 * memilikinya (idempoten). Menampilkan pesan informasi dan mengembalikan string kosong bila
	 * tidak ada ruang tersedia/kuota penuh.
	 *
	 * @param calonPegawai       calon pegawai yang akan diberi nomor ujian dan ruang
	 * @param jumlahPengecualian daftar nomor ujian yang harus dihindari, diperbarui di tempat
	 *                           saat terjadi bentrok
	 * @return nomor ujian yang dibangkitkan (atau sudah ada), atau string kosong bila gagal
	 *         (tidak ada ruang tersedia atau pembentukan nomor unik gagal)
	 */
	@Override
	public String generateNoUjian(CalonPegawai calonPegawai, List<String> jumlahPengecualian) throws Exception {
		if (calonPegawai == null) {
			return "";
		}
		if (calonPegawai.getNoUjian() != null && !calonPegawai.getNoUjian().trim().isEmpty()) {
			return calonPegawai.getNoUjian().trim();
		}
		List<String> pengecualian = jumlahPengecualian == null ? new ArrayList<String>() : jumlahPengecualian;
		Session session = HibernateUtil.currentSession();
		Long idmin = (Long) session.createCriteria(RuangPegawai.class).createAlias("ujianPegawai", "ujianPegawai")
				.add(Restrictions.eq("gelombangPendaftaranPegawai", calonPegawai.getGelombangPendaftaranPegawai()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPegawai.gelombangPendaftaranPegawai",
						calonPegawai.getGelombangPendaftaranPegawai()))
				.setProjection(Projections.min("id")).uniqueResult();
		if (idmin == null) {
			MyMessageboxConfig.show(
					"Kuota / Ruangan ujian untuk gelombang " + calonPegawai.getGelombangPendaftaranPegawai()
							+ " tidak ditemukan atau sudah penuh",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}
		RuangPegawai ruangSelected = (RuangPegawai) session.createCriteria(RuangPegawai.class)
				.add(Restrictions.idEq(idmin)).uniqueResult();
		if (ruangSelected == null || !ruangMasihTersedia(session, ruangSelected)) {
			MyMessageboxConfig.show("Kuota / Ruangan ujian telah penuh", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return "";
		}
		String noUjianFinal = buatNomorUjianUnik(session, pengecualian);
		if (noUjianFinal == null || noUjianFinal.trim().isEmpty()) {
			return "";
		}
		session.refresh(calonPegawai);
		calonPegawai.setNoUjian(noUjianFinal);
		Common.refreshUpdate(session, calonPegawai);
		CommonPegawai.dapatkanRuangUjian(calonPegawai);
		return noUjianFinal;
	}

	/** Menghitung ulang jumlah peserta yang sudah menempati {@code ruangSelected} dan membandingkannya dengan kapasitas ruangan. */
	private boolean ruangMasihTersedia(Session session, RuangPegawai ruangSelected) {
		Number total = (Number) session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.noUjian", ""))
				.add(Restrictions.isNotNull("calonPegawai.noUjian")).add(Restrictions.eq("ruangPegawai", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult();
		int isiRuang = total == null ? 0 : total.intValue();
		return isiRuang < ruangSelected.getKapasitasRuangan();
	}

	/**
	 * Membentuk nomor ujian unik berurutan, dimulai dari nomor tertinggi yang sudah tersimpan
	 * (dibaca lewat SQL native {@code to_number(substr(noujian,5),...)}, sehingga hanya bagian
	 * setelah 4 karakter awal nomor yang diperlakukan sebagai angka urut) ditambah jumlah
	 * pengecualian, dipadatkan sejumlah digit sesuai konfigurasi
	 * {@code jumlah_increments_no_ujian_pegawai} (default 8). Mencoba hingga
	 * {@link #MAX_ATTEMPT} kali sebelum menyerah dan mengembalikan string kosong.
	 */
	private String buatNomorUjianUnik(Session session, List<String> pengecualian) {
		int digit = ambilJumlahDigit();
		Number max = (Number) session.createSQLQuery(
				"select max(to_number(substr(noujian,5),'99999999999999')) from calon_pegawai where noujian != '' and noujian is not null and substr(noujian,5)!=''")
				.uniqueResult();
		int dasar = max == null ? 0 : max.intValue();
		for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
			String noUjian = formatNomor(dasar + pengecualian.size() + attempt + 1, digit);
			if (pengecualian.contains(noUjian)) {
				continue;
			}
			Number count = (Number) session.createCriteria(CalonPegawai.class).add(Restrictions.eq("noUjian", noUjian))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (count == null || count.intValue() == 0) {
				return noUjian;
			}
			pengecualian.add(noUjian);
		}
		return "";
	}

	/** Membaca jumlah digit nomor ujian dari konfigurasi {@code jumlah_increments_no_ujian_pegawai}, default/fallback 8 bila tidak terbaca. */
	private int ambilJumlahDigit() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("jumlah_increments_no_ujian_pegawai", "8").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 8;
		}
	}

	/** Memadatkan {@code nomor} dengan nol di depan hingga sepanjang {@code digit} karakter. */
	private String formatNomor(int nomor, int digit) {
		String hasil = "00000000000000000000" + nomor;
		return hasil.substring(hasil.length() - digit);
	}
}
