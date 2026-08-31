package ais.action.master.pmb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Kegiatan;
import ais.database.model.RuangPMB;
import ais.database.model.RuangPaketPMB;
import ais.ui.util.MyMessageboxConfig;

/**
 * Varian kedua algoritma penomoran nomor ujian PMB, berbeda dari {@link PrefixNoUjianGenerator}
 * karena sekaligus menangani <b>penempatan ruang ujian</b> (bukan cuma penomoran). Alurnya: (1)
 * bila nomor ujian sudah pernah diberikan, kembalikan apa adanya (idempoten); (2) bila gelombang
 * pendaftaran mensyaratkan pembayaran sebelum login, verifikasi status lunas pembayaran registrasi
 * — bila belum lunas, tampilkan peringatan dan batalkan; (3) cari ruang ujian ({@link RuangPMB})
 * dengan id terkecil yang belum penuh dan cocok paket + gelombang pendaftaran; (4) hitung sisa
 * kapasitas ruang tersebut, dan bila masih ada slot, bentuk nomor ujian dari 2 digit tahun +
 * nomor urut (jumlah digit dapat dikonfigurasi lewat {@code jumlah_increments_no_ujian_pmb},
 * default 8), simpan ke entitas, lalu daftarkan penempatan ruang lewat
 * {@link CommonPMB#dapatkanRuangUjian}; (5) bila ruang sudah penuh saat proses berjalan, tampilkan
 * peringatan dan batalkan.
 */
public class DefaultNoUjianGenerator2 implements NoUjianGenerator {

	/** Singleton utilitas pembayaran PMB, dipakai untuk pengecekan status lunas registrasi. */
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Seperti {@link #generateNoUjian(BiodataCalonMahasiswa, List)} tanpa daftar nomor yang harus dihindari. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan (atau mengembalikan nomor ujian yang sudah ada bagi) {@code biodataCalonMahasiswa},
	 * sekaligus menempatkannya ke ruang ujian yang tersedia. Lihat javadoc kelas untuk alur lengkap.
	 * Rekursif bila nomor yang dihasilkan bentrok dengan {@code jumlahPengecualian}.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang akan diberi nomor ujian dan ruang
	 * @param jumlahPengecualian    daftar nomor yang harus dihindari, dimutasi langsung saat bentrokan
	 * @return nomor ujian final, atau string kosong bila pembayaran registrasi belum lunas atau
	 *         seluruh ruang ujian yang cocok sudah penuh (kedua kasus menampilkan pesan peringatan
	 *         ke pengguna, bukan melempar exception)
	 * @throws Exception diteruskan dari kegagalan akses konfigurasi/database
	 */
	// generate NIM
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> jumlahPengecualian)
			throws Exception {

		if (biodataCalonMahasiswa.getNoUjian() != null && !biodataCalonMahasiswa.getNoUjian().trim().isEmpty()) {
			return biodataCalonMahasiswa.getNoUjian().trim();
		}

		if (biodataCalonMahasiswa.getGelombangPendaftaran()
				.getHarusBayarSebelumBisaLogin()) {
			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();

			if (kegiatan == null || kegiatan.getId() == null || !kegiatan.getLunas()) {
				String infoBelumbayarSaatLogincalonMahasiswa = Common
						.getKonfigurasi("infoBelumbayarSaatProsescalonMahasiswa",
								"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan proses pembayaran.")
						.getNilai();
				infoBelumbayarSaatLogincalonMahasiswa = org.apache.commons.lang.StringUtils
						.replace(infoBelumbayarSaatLogincalonMahasiswa, "[noreg]", biodataCalonMahasiswa.getNoRegistrasi());
				MyMessageboxConfig.show(infoBelumbayarSaatLogincalonMahasiswa, "PERINGATAN",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return "";
			}
		}

		Session session = HibernateUtil.currentSession();
		Long idmin = (Long) session.createCriteria(RuangPMB.class).createAlias("ujianPMB", "ujianPMB")
				.add(Restrictions.or(Restrictions.isNull("paket"),
						Restrictions.eq("paket", biodataCalonMahasiswa.getPaket())))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPMB.gelombangPendaftaran", biodataCalonMahasiswa.getGelombangPendaftaran()))
				.setProjection(Projections.min("id")).uniqueResult();

		if (idmin == null) {
			MyMessageboxConfig.show(
					"Ruangan ujian untuk paket " + biodataCalonMahasiswa.getPaket()
							+ " tahun penerimaan mahasiswa baru " + biodataCalonMahasiswa.getGelombangPendaftaran()
							+ " tidak ditemukan atau sudah penuh",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		RuangPMB ruangSelected = (RuangPMB) session.createCriteria(RuangPMB.class).add(Restrictions.idEq(idmin))
				.uniqueResult();

		Number s = ((Number) (session.createCriteria(RuangPaketPMB.class)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
				.add(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""))
				.add(Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
				.add(Restrictions.eq("ruangPMB", ruangSelected)).setProjection(Projections.rowCount()).uniqueResult()));
		
		Integer isiRuang = s==null?0:s.intValue();

		String noUjianFinal = "";

		if (isiRuang < ruangSelected.getKapasitasRuangan()) {

			String digitSatuDanDua = biodataCalonMahasiswa.getTahun().toString().substring(2, 4);

			Integer jumlahIncrements = 8;
			try {
				jumlahIncrements = Integer
						.parseInt(Common.getKonfigurasi("jumlah_increments_no_ujian_pmb", "8").getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			long nomorUrut = NoUjianGeneratorSupport.nomorUrutBerikutnya(session, digitSatuDanDua,
					jumlahIncrements, biodataCalonMahasiswa, jumlahPengecualian);
			String kodeRegistratsi = NoUjianGeneratorSupport.leftPadNomor(nomorUrut, jumlahIncrements);

			System.out.println("noUjian => " + kodeRegistratsi);

			noUjianFinal = digitSatuDanDua + kodeRegistratsi;

			session.refresh(biodataCalonMahasiswa);
			biodataCalonMahasiswa.setNoUjian(noUjianFinal);
			Common.refreshUpdate(session, biodataCalonMahasiswa);

		} else {
			MyMessageboxConfig.show("Ruangan " + ruangSelected + " telah melebihi kapasitas", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		boolean nomorSudahDipakai = NoUjianGeneratorSupport.nomorSudahDipakai(session, noUjianFinal,
				biodataCalonMahasiswa);

		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noUjianFinal);
			return generateNoUjian(biodataCalonMahasiswa, jumlahPengecualian);
		} else {

			CommonPMB.dapatkanRuangUjian(biodataCalonMahasiswa);

			return noUjianFinal;
		}

	}

}
