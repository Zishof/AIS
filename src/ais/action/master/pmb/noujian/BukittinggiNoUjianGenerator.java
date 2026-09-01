package ais.action.master.pmb.noujian;

import java.text.SimpleDateFormat;
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
 * Algoritma penomoran nomor ujian (No Ujian) khusus institusi Bukittinggi, dengan alur penempatan
 * ruang ujian yang sama dengan {@link DefaultNoUjianGenerator} (cek pembayaran registrasi, cari
 * ruang belum penuh, cek kapasitas), namun format nomor berbeda: {@code 4 digit tahun berjalan +
 * kode paket ujian + 5 digit nomor urut} yang dihitung dari jumlah calon mahasiswa aktif dengan
 * nomor ujian berawalan (prefix) tahun+paket yang sama.
 */
public class BukittinggiNoUjianGenerator implements NoUjianGenerator {

	static final ThreadLocal<SimpleDateFormat> format = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy");
		}
	};
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Seperti {@link #generateNoUjian(BiodataCalonMahasiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan nomor ujian dan menempatkan calon mahasiswa ke ruang ujian yang tersedia —
	 * lihat penjelasan format dan alur pada dokumentasi kelas (identik dengan
	 * {@link DefaultNoUjianGenerator} kecuali format nomor). Bila nomor ujian sudah ada pada
	 * entitas, nilai yang ada langsung dikembalikan. Bila nomor hasil ternyata sudah dipakai calon
	 * mahasiswa lain, nomor tersebut ditambahkan ke {@code jumlahPengecualian} dan method
	 * memanggil dirinya sendiri secara rekursif.
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

		if (biodataCalonMahasiswa.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin()) {
			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();

			if (!CommonPMB.isPembayaranRegistrasiTerpenuhi(kegiatan)) {
				String infoBelumbayarSaatLogincalonMahasiswa = Common.getKonfigurasi(
						"infoBelumbayarSaatProsescalonMahasiswa",
						"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan proses pembayaran.")
						.getNilai();
				infoBelumbayarSaatLogincalonMahasiswa = org.apache.commons.lang.StringUtils.replace(
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

			String digitPertama = format.get().format(ais.ui.util.WaktuUtil.getDate())
					+ biodataCalonMahasiswa.getPaket().getKode();

			Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.ilike("noUjian", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
					.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKedua = "000000000000000" + (jumlah + 1);
			digitKedua = digitKedua.substring(digitKedua.length() - 5);

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);

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
