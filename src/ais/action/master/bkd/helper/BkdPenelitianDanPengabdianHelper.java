package ais.action.master.bkd.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.penelitiandanpengabdian.AnggotaPengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanTahapanPelaporanPenelitianDanPengabdian;

public class BkdPenelitianDanPengabdianHelper {

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {

		Criterion criterion = pegawai == null ? Restrictions.isNotNull("tbmuser.pegawai")
				: Restrictions.eq("tbmuser.pegawai", pegawai);

		List<PengajuanTahapanPelaporanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdians = session
				.createCriteria(PengajuanTahapanPelaporanPenelitianDanPengabdian.class)
				.createAlias("tahapanPelaporanPenelitianDanPengabdian", "tahapanPelaporanPenelitianDanPengabdian")
				.createAlias("pengajuanPenelitianDanPengabdian", "pengajuanPenelitianDanPengabdian")
				.createAlias("pengajuanPenelitianDanPengabdian.penelitianDanPengabdian", "penelitianDanPengabdian")
				.createAlias("pengajuanPenelitianDanPengabdian.tbmuser", "tbmuser").add(criterion)
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penelitianDanPengabdian.tahunAkademik", tahunAkademik))
				.add(semester == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penelitianDanPengabdian.semester", semester))
				.add(Restrictions.eq("pengajuanPenelitianDanPengabdian.status",
						PengajuanPenelitianDanPengabdian.DISETUJUI))
				.add(Restrictions.eq("status", PengajuanTahapanPelaporanPenelitianDanPengabdian.DISETUJUI))
				.addOrder(Order.desc("tahapanPelaporanPenelitianDanPengabdian.mulai")).list();

		System.out.println("pengajuanPenelitianDanPengabdians => " + pengajuanPenelitianDanPengabdians);
		List<Long> pengajuan = new ArrayList<Long>();

		if (pengajuanPenelitianDanPengabdians.isEmpty()) {

			List<PengajuanPenelitianDanPengabdian> pengajuans = session
					.createCriteria(PengajuanPenelitianDanPengabdian.class)
					.createAlias("penelitianDanPengabdian", "penelitianDanPengabdian").createAlias("tbmuser", "tbmuser")
					.add(criterion)
					.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("penelitianDanPengabdian.tahunAkademik", tahunAkademik))
					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("penelitianDanPengabdian.semester", semester))
					.add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI)).list();

			System.out.println("pengajuans => " + pengajuans);
			int rowCount = pengajuans.size();
			int i = 0;
			for (PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian : pengajuans) {
				if (pengajuan.contains(pengajuanPenelitianDanPengabdian.getId())) {
					continue;
				}

				pengajuan.add(pengajuanPenelitianDanPengabdian.getId());

				label.setValue("Memproses data \"" + pengajuanPenelitianDanPengabdian + "\" ("
						+ Common.numberFormat.get().format(i * 100.0 / rowCount) + "%)");
				i++;

				List<Pegawai> anggotaPengajuanPenelitianDanPengabdians = session
						.createCriteria(AnggotaPengajuanPenelitianDanPengabdian.class).createAlias("tbmuser", "tbmuser")
						.setProjection(Projections.groupProperty("tbmuser.pegawai"))
						.add(Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian))
						.list();

				System.out.println(
						"anggotaPengajuanPenelitianDanPengabdians => " + anggotaPengajuanPenelitianDanPengabdians);

				BkdPenelitianDanPengabdianHelper.populate(session,
						pengajuanPenelitianDanPengabdian.getTbmuser().getPegawai(), tahunAkademik, semester, null,
						pengajuanPenelitianDanPengabdian, "ketua", anggotaPengajuanPenelitianDanPengabdians.size());

				for (Pegawai p : anggotaPengajuanPenelitianDanPengabdians) {
					System.out.println("p => " + p);
					BkdPenelitianDanPengabdianHelper.populate(session, p, tahunAkademik, semester, null,
							pengajuanPenelitianDanPengabdian, "anggota",
							anggotaPengajuanPenelitianDanPengabdians.size());
				}
			}

		} else {

			int rowCount = pengajuanPenelitianDanPengabdians.size();
			int i = 0;
			for (PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdian : pengajuanPenelitianDanPengabdians) {
				PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = pengajuanTahapanPelaporanPenelitianDanPengabdian
						.getPengajuanPenelitianDanPengabdian();
				if (pengajuan.contains(pengajuanPenelitianDanPengabdian.getId())) {
					continue;
				}

				pengajuan.add(pengajuanPenelitianDanPengabdian.getId());

				label.setValue("Memproses data \"" + pengajuanPenelitianDanPengabdian + "\" ("
						+ Common.numberFormat.get().format(i * 100.0 / rowCount) + "%)");
				i++;

				List<Pegawai> anggotaPengajuanPenelitianDanPengabdians = session
						.createCriteria(AnggotaPengajuanPenelitianDanPengabdian.class).createAlias("tbmuser", "tbmuser")
						.setProjection(Projections.groupProperty("tbmuser.pegawai"))
						.add(Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian))
						.list();

				BkdPenelitianDanPengabdianHelper.populate(session,
						pengajuanPenelitianDanPengabdian.getTbmuser().getPegawai(), tahunAkademik, semester,
						pengajuanTahapanPelaporanPenelitianDanPengabdian, pengajuanPenelitianDanPengabdian, "ketua",
						anggotaPengajuanPenelitianDanPengabdians.size());

				for (Pegawai p : anggotaPengajuanPenelitianDanPengabdians) {
					BkdPenelitianDanPengabdianHelper.populate(session, p, tahunAkademik, semester,
							pengajuanTahapanPelaporanPenelitianDanPengabdian, pengajuanPenelitianDanPengabdian,
							"anggota", anggotaPengajuanPenelitianDanPengabdians.size());
				}

			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdian,
			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian, String jenisAnggotaPublikasi,
			Integer jumlahAnggota) {

		if (pegawai == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).add(Restrictions.eq("pegawai", pegawai))
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)) )
				.setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("pegawai => " + pegawai + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			double sks = 0.0;

			if (pengajuanTahapanPelaporanPenelitianDanPengabdian != null) {
				String key = "pengaturan_beban_sks_penelitian_dan_pengabdian";
				String newKey = key + "_" + pengajuanTahapanPelaporanPenelitianDanPengabdian
						.getPengajuanPenelitianDanPengabdian().getPenelitianDanPengabdian().getId()

						+ "_" + pengajuanTahapanPelaporanPenelitianDanPengabdian
								.getTahapanPelaporanPenelitianDanPengabdian().getId();

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				try {
					sks = Double.parseDouble(konfigurasi.getNilai());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			} else {
				sks = pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getSks();
			}

			String pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian = Common
					.getParameterUmum("pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian", "Dinilai sama")
					.getNilai();

			if (jenisAnggotaPublikasi.equalsIgnoreCase("anggota") && jumlahAnggota > 0) {
				if (pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian.equalsIgnoreCase("50%")) {
					sks = sks / 2.0;
				} else if (pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian.equalsIgnoreCase("Dibagi rata")) {
					sks = sks / (jumlahAnggota.doubleValue() + 1); // ditambah
																	// satu
																	// sebagai
																	// ketua
				} else if (pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian.equalsIgnoreCase(
						"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%")) {
					if (jumlahAnggota > 1) {
						sks = (sks * 0.6) / jumlahAnggota.doubleValue();
					} else if (jumlahAnggota == 1) {
						sks = (sks * 0.4) / jumlahAnggota.doubleValue();
					}
				}
			} else if (jenisAnggotaPublikasi.equalsIgnoreCase("ketua") && jumlahAnggota > 0) {
				if (pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian.equalsIgnoreCase("50%")) {
					sks = sks / 2.0;
				} else if (pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian.equalsIgnoreCase("Dibagi rata")) {
					sks = sks / (jumlahAnggota.doubleValue() + 1); // ditambah
																	// satu
																	// sebagai
																	// ketua
				} else if (pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian.equalsIgnoreCase(
						"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%")) {
					if (jumlahAnggota > 1) {
						sks = (sks * 0.4) / jumlahAnggota.doubleValue();
					} else if (jumlahAnggota == 1) {
						sks = (sks * 0.6) / jumlahAnggota.doubleValue();
					}
				}
			}

			String keterangan = pegawai.getNama() + " telah mem-publikasi "
					+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTipePenelitianDanPengabdian()
							.getIsi()
					+ " dengan judul \"" + pengajuanPenelitianDanPengabdian.getJudul() + "\" "
					+ (jumlahAnggota > 0 ? ("sebagai " + jenisAnggotaPublikasi + " dengan jumlah anggota "
							+ jumlahAnggota + " orang") : "")
					+ " "
					+ (pengajuanTahapanPelaporanPenelitianDanPengabdian == null ? ""
							: pengajuanTahapanPelaporanPenelitianDanPengabdian
									.getTahapanPelaporanPenelitianDanPengabdian().getNama())
					+ "  " + pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getNama()
					+ " . SKS yang diporeh adalah " + Common.numberFormat.get().format(sks) + ", cara penghitungan "
					+ pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian;

			AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session.createCriteria(AsesemenPenilaian.class)
					.add(Restrictions.eq("pegawai", pegawai))
					.add(Restrictions.eq("pengajuanPenelitianDanPengabdian", pengajuanPenelitianDanPengabdian))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("semester", semester))
					.setMaxResults(1).uniqueResult();
			if (asesemenPenilaian == null) {
				asesemenPenilaian = new AsesemenPenilaian();
			}

			asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENELITIAN_ATAU_PENGABDIAN);
			asesemenPenilaian.setBidang(pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian()
					.getTipePenelitianDanPengabdian().getIsi().toLowerCase().contains("penelitian")
							? PenunjangKinerjaDosen.PENELITIAN : PenunjangKinerjaDosen.PENGABDIAN);
			asesemenPenilaian.setKeterangan(keterangan);
			asesemenPenilaian.setPengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdian);
			asesemenPenilaian.setTahunAkademik(tahunAkademik);
			asesemenPenilaian.setSemester(semester);
			asesemenPenilaian.setPegawai(pegawai);
			asesemenPenilaian.setSks(sks);

			session.getTransaction().begin();
			session.saveOrUpdate(asesemenPenilaian);
			session.getTransaction().commit();

			PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
		}
	}
}
