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
 * Algoritma penomoran nomor ujian (No Ujian) default untuk modul PMB, yang sekaligus menangani
 * penempatan calon mahasiswa ke ruang ujian ({@link RuangPMB}). Alur: (1) bila
 * {@code harusBayarSebelumBisaLogin} aktif pada gelombang pendaftaran, menolak (menampilkan
 * pesan via {@link MyMessageboxConfig}) bila pembayaran registrasi belum lunas; (2) mencari
 * ruang ujian dengan id terkecil yang belum penuh dan cocok dengan paket serta gelombang
 * pendaftaran calon mahasiswa; (3) memeriksa kapasitas ruang tersisa — bila sudah penuh,
 * menampilkan peringatan dan mengembalikan string kosong; (4) membangkitkan nomor ujian
 * berformat {@code 2 digit tahun + nomor urut} (panjang dapat dikonfigurasi lewat
 * {@code jumlah_increments_no_ujian_pmb}, default 8 digit) lewat {@link NoUjianGeneratorSupport};
 * (5) menyimpan nomor ujian ke entitas dan mendaftarkan penempatan ruang lewat
 * {@link CommonPMB#dapatkanRuangUjian}.
 */
public class DefaultNoUjianGenerator implements NoUjianGenerator {

	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Seperti {@link #generateNoUjian(BiodataCalonMahasiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan nomor ujian dan menempatkan calon mahasiswa ke ruang ujian yang tersedia —
	 * lihat alur lengkap pada dokumentasi kelas. Bila nomor ujian sudah pernah ada pada entitas
	 * (proses ulang), nilai yang ada langsung dikembalikan tanpa membangkitkan ulang. Bila nomor
	 * hasil ternyata sudah dipakai calon mahasiswa lain (dicek via
	 * {@link NoUjianGeneratorSupport#nomorSudahDipakai}), nomor tersebut ditambahkan ke
	 * {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara rekursif.
	 *
	 * @param jumlahPengecualian nomor ujian kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return nomor ujian yang dibangkitkan, atau string kosong bila pembayaran belum lunas atau ruang ujian penuh/tidak ditemukan
	 */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> jumlahPengecualian)
			throws Exception {

		if (biodataCalonMahasiswa.getNoUjian() != null && !biodataCalonMahasiswa.getNoUjian().trim().isEmpty()) {
			return biodataCalonMahasiswa.getNoUjian().trim();
		}

		if (biodataCalonMahasiswa.getGelombangPendaftaran()
				.getHarusBayarSebelumBisaLogin()) {
			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();

			if (!CommonPMB.isPembayaranRegistrasiTerpenuhi(kegiatan)) {
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
			String noUjian = NoUjianGeneratorSupport.leftPadNomor(nomorUrut, jumlahIncrements);
			System.out.println("noUjian => " + noUjian);

			noUjianFinal = digitSatuDanDua + noUjian;

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
