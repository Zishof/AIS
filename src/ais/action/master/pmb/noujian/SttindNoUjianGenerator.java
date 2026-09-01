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
 * Algoritma pembangkit nomor ujian PMB khusus institusi STTIND, sekaligus penetap ruang ujian
 * ({@link RuangPMB}) untuk calon mahasiswa. Alur kerja: (1) bila calon mahasiswa sudah punya
 * nomor ujian, kembalikan langsung; (2) bila gelombang pendaftaran mewajibkan pembayaran sebelum
 * login dan pembayaran registrasi belum lunas, tampilkan peringatan dan kembalikan string kosong;
 * (3) cari {@link RuangPMB} yang cocok dengan paket calon mahasiswa dan belum penuh pada gelombang
 * pendaftarannya; (4) bila ruang tidak ditemukan atau sudah melebihi kapasitas, tampilkan pesan
 * informasi dan kembalikan string kosong; (5) bentuk nomor ujian dari 2 digit terakhir tahun +
 * kode prodi pilihan pertama, diikuti 4 digit urutan (dihitung dari jumlah
 * {@link BiodataCalonMahasiswa} aktif yang nomor ujiannya sudah diawali prefix yang sama, ditambah
 * jumlah nomor yang sudah dipesan dalam batch berjalan); (6) bila nomor sudah dipakai, coba lagi
 * secara rekursif; (7) setelah berhasil, simpan nomor ujian ke entitas dan tetapkan penempatan
 * ruang ujian lewat {@link CommonPMB#dapatkanRuangUjian}.
 */
public class SttindNoUjianGenerator implements NoUjianGenerator {

	/** Instans bersama {@link PembayaranUtil} untuk mengecek status lunas pembayaran registrasi. */
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** @return nomor ujian untuk {@code biodataCalonMahasiswa}, lihat {@link #generateNoUjian(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan (atau mengembalikan nomor ujian yang sudah ada) sekaligus menetapkan ruang
	 * ujian calon mahasiswa. Lihat javadoc kelas untuk alur lengkap, termasuk pengecekan
	 * pembayaran registrasi dan kuota ruang.
	 *
	 * @param biodataCalonMahasiswa data calon mahasiswa yang akan diberi nomor ujian
	 * @param jumlahPengecualian    nomor yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @return nomor ujian yang tersimpan, atau string kosong bila pembayaran belum lunas atau kuota/ruang tidak tersedia
	 */
	// generate NIM
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> jumlahPengecualian)
			throws Exception {

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

			Integer tahun = biodataCalonMahasiswa.getTahun();
			String digitPertama = tahun.toString().substring(2)
					+ (biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getKode());

			Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.ilike("noUjian", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
					.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKedua = "000000000000000" + (jumlah + 1);
			digitKedua = digitKedua.substring(digitKedua.length() - 4);

			System.out.println("digit pertama (kode tahun) = " + digitPertama);
			System.out.println("digit kedua (kode urutan) = " + digitKedua);

			noUjianFinal = digitPertama + digitKedua;

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
