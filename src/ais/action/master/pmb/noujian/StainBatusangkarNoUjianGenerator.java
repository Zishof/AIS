package ais.action.master.pmb.noujian;

import java.util.ArrayList;
import java.util.Calendar;
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
 * Pembangkit nomor ujian PMB (Penerimaan Mahasiswa Baru) khusus institusi STAIN Batusangkar,
 * sekaligus penetap ruang ujian bagi calon mahasiswa. Format nomor: {@code YY+KODEPAKET+URUT},
 * mis. {@code "26TI0007"}, dengan {@code URUT} dihitung lewat helper bersama
 * {@link NoUjianGeneratorSupport}.
 *
 * <p>
 * Alur: (1) bila calon sudah punya nomor ujian, dikembalikan apa adanya (idempoten); (2) bila
 * gelombang pendaftaran mengharuskan pembayaran sebelum login ({@code
 * harusBayarSebelumBisaLogin}) dan pembayaran registrasi belum lunas, proses dihentikan dengan
 * pesan peringatan; (3) dicari ruang ujian yang cocok dengan paket calon dan gelombang
 * pendaftarannya, dengan kuota tersisa; (4) dipastikan ruang belum melebihi kapasitas lewat
 * penghitungan ulang peserta yang sudah menempati; (5) nomor dibentuk dan disimpan, ruang ujian
 * ditetapkan lewat {@code CommonPMB#dapatkanRuangUjian}. Bila nomor hasil bentrok, dibangkitkan
 * ulang secara rekursif.
 * </p>
 */
public class StainBatusangkarNoUjianGenerator implements NoUjianGenerator {

	/** Instance {@link PembayaranUtil} bersama (singleton) untuk keperluan pengecekan status pembayaran registrasi. */
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Membangkitkan nomor ujian baru untuk {@code biodataCalonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan nomor ujian berformat {@code YY+KODEPAKET+URUT}, memverifikasi status
	 * pembayaran registrasi (bila diwajibkan gelombang pendaftaran) dan ketersediaan ruang
	 * ujian, lalu menyimpan nomor dan menetapkan ruang pada calon mahasiswa. Menampilkan pesan
	 * peringatan/informasi dan mengembalikan string kosong bila pembayaran belum lunas atau
	 * ruang tidak tersedia/penuh. Mengulang secara rekursif bila nomor hasil bentrok.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang akan diberi nomor ujian dan ruang
	 * @param jumlahPengecualian    daftar nomor ujian yang harus dihindari, diperbarui di tempat
	 *                              saat terjadi bentrok
	 * @return nomor ujian yang dibangkitkan (atau sudah ada), atau string kosong bila gagal
	 */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> jumlahPengecualian)
			throws Exception {

		if (biodataCalonMahasiswa.getNoUjian() != null && !biodataCalonMahasiswa.getNoUjian().trim().isEmpty()) {
			return biodataCalonMahasiswa.getNoUjian().trim();
		}

		if (biodataCalonMahasiswa.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin()) {

			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();

			if (kegiatan == null || kegiatan.getId() == null || !kegiatan.getLunas()) {
				String infoBelumbayarSaatLogincalonMahasiswa = Common.getKonfigurasi(
						"infoBelumbayarSaatProsescalonMahasiswa",
						"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan proses pembayaran.")
						.getNilai();
				infoBelumbayarSaatLogincalonMahasiswa = org.apache.commons.lang3.StringUtils.replace(
						infoBelumbayarSaatLogincalonMahasiswa, "[noreg]", biodataCalonMahasiswa.getNoRegistrasi());
				MyMessageboxConfig.show(infoBelumbayarSaatLogincalonMahasiswa, "PERINGATAN", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
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

		Integer isiRuang = s == null ? 0 : s.intValue();

		String noUjianFinal = "";

		if (isiRuang < ruangSelected.getKapasitasRuangan()) {

			String digitPertama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";
			digitPertama = digitPertama.substring(3) + biodataCalonMahasiswa.getPaket().getKode();

			long nomorUrut = NoUjianGeneratorSupport.nomorUrutBerikutnya(session, digitPertama, 4,
					biodataCalonMahasiswa, jumlahPengecualian);
			String digitKedua = NoUjianGeneratorSupport.leftPadNomor(nomorUrut, 4);

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);

			noUjianFinal = digitPertama + digitKedua;

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

			try {

				session.refresh(biodataCalonMahasiswa);
				biodataCalonMahasiswa.setNoUjian(noUjianFinal);
				Common.refreshUpdate(session, biodataCalonMahasiswa);

				CommonPMB.dapatkanRuangUjian(biodataCalonMahasiswa);

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			return noUjianFinal;
		}

	}

}
