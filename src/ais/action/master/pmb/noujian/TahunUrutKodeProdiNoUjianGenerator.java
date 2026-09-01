package ais.action.master.pmb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
 * Algoritma pembangkit nomor ujian PMB dengan format "tahun.urutan.kodeProdi", sekaligus penetap
 * ruang ujian ({@link RuangPMB}) untuk calon mahasiswa. Alur kerja serupa
 * {@link SttindNoUjianGenerator}, dengan perbedaan: (1) mengembalikan string kosong lebih awal
 * bila calon mahasiswa belum memiliki prodi pilihan pertama ({@code prodi1}); (2) format nomor
 * akhir adalah {@code "<tahun>.<urutan 4 digit>.<kodeProdi>"}, dengan urutan dihitung per
 * kombinasi (prodi, prefix tahun) — bukan per tahun saja seperti pada generator STTIND. Alur
 * lengkap lain (cek pembayaran registrasi, pencarian ruang, retry rekursif saat nomor bentrok,
 * penetapan ruang ujian akhir) identik.
 */
public class TahunUrutKodeProdiNoUjianGenerator implements NoUjianGenerator {

	/** Instans bersama {@link PembayaranUtil} untuk mengecek status lunas pembayaran registrasi. */
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** @return nomor ujian untuk {@code biodataCalonMahasiswa}, lihat {@link #generateNoUjian(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan (atau mengembalikan nomor ujian yang sudah ada) sekaligus menetapkan ruang
	 * ujian calon mahasiswa. Mengembalikan string kosong bila prodi pilihan pertama belum diisi,
	 * pembayaran registrasi belum lunas (untuk gelombang yang mewajibkannya), atau kuota/ruang
	 * ujian tidak tersedia. Lihat javadoc kelas untuk format nomor dan alur lengkap.
	 *
	 * @param biodataCalonMahasiswa data calon mahasiswa yang akan diberi nomor ujian
	 * @param jumlahPengecualian    nomor yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @return nomor ujian yang tersimpan, atau string kosong sesuai kondisi di atas
	 */
	// generate NIM
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> jumlahPengecualian)
			throws Exception {

		if (biodataCalonMahasiswa.getProdi1() == null) {
			return "";
		}

		if (biodataCalonMahasiswa.getNoUjian() != null && !biodataCalonMahasiswa.getNoUjian().trim().isEmpty()) {
			return biodataCalonMahasiswa.getNoUjian().trim();
		}

		if (biodataCalonMahasiswa.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin()) {
			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();

			if (!CommonPMB.isPembayaranRegistrasiTerpenuhi(kegiatan)) {
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

			String digitPertama = biodataCalonMahasiswa.getTahun() + ".";

			Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("prodi1", biodataCalonMahasiswa.getProdi1()))
					.add(Restrictions.ilike("noUjian", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
					.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKedua = "000000000000000" + (jumlah + 1);
			digitKedua = digitKedua.substring(digitKedua.length() - 4) + ".";

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);

			noUjianFinal = digitPertama + digitKedua + biodataCalonMahasiswa.getProdi1().getKode();

		} else {
			MyMessageboxConfig.show("Ruangan " + ruangSelected + " telah melebihi kapasitas", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noUjian", noUjianFinal)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		if (!count.equals(0)) {
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
