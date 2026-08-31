package ais.action.master.bkd.helper;

import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.TahapanAtauCapaianPembelajaran;

/**
 * Helper batch modul BKD (Beban Kerja Dosen) yang menghitung SKS bimbingan tugas akhir/skripsi
 * dosen dan mencatatnya sebagai {@link AsesemenPenilaian} (penilaian asesor, bidang
 * {@link PenunjangKinerjaDosen#PENDIDIKAN}, spesifikasi {@link PenilaianAsesor#PEMBIMBING_TA}) untuk
 * satu tahun akademik/semester. Dipanggil baik untuk satu {@link Pegawai}/{@link Dosen} spesifik
 * maupun untuk seluruh dosen (dengan progres opsional ditulis ke {@link Label} UI, mendukung
 * pemakaian dari proses batch berjalan lama).
 *
 * <p>
 * Perhitungan SKS mengikuti konfigurasi {@link ParameterUmum} berkunci
 * {@code jumlah_sks_bimbingan_tugas_akhir_<idJenjang>_<peran>_<idTahapan>}: jumlah mahasiswa
 * bimbingan (dibatasi maksimum dari {@code info1} konfigurasi) dikalikan bobot SKS per mahasiswa
 * ({@code nilai} konfigurasi), dijumlahkan lintas {@link TahapanAtauCapaianPembelajaran} bertipe
 * {@code TATAPAN_BIMBINGAN}. Peran "Pembimbing Utama" dicocokkan ke kolom {@code dosen1} pada
 * {@link MahasiswaRequestTugasAkhir}, sedangkan "Pembimbing Pendamping" mencakup {@code dosen2}
 * ATAU {@code dosen3}. Setelah {@link AsesemenPenilaian} disimpan,
 * {@code PenilaianAsesorAction.checkPenilaian} dipanggil untuk memastikan baris
 * {@link PenilaianAsesor} terkait tersinkron dengan asesor yang berlaku.
 * </p>
 */
public class BkdBimbinganSkripsiHelper {

	/** Memproses seluruh {@link Jenjang} aktif untuk {@code pegawai} (atau seluruh dosen bila {@code null}) pada tahun akademik/semester tertentu, mendelegasikan ke overload per-jenjang. */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdBimbinganSkripsiHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Untuk satu {@code jenjang}: bila {@code pegawai} diberikan (dan berelasi ke {@link Dosen}),
	 * memproses dosen tersebut langsung sebagai pembimbing utama DAN pendamping. Bila
	 * {@code pegawai} {@code null}, mengumpulkan seluruh dosen yang tercatat sebagai
	 * pembimbing1/2/3 pada {@link MahasiswaRequestTugasAkhir} berstatus aktif/mengulang/seminar/lulus
	 * pada periode tersebut, lalu memproses masing-masing (dosen1 sebagai utama, gabungan dosen2+
	 * dosen3 sebagai pendamping — dideduplikasi lewat {@link TreeSet}).
	 */
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdBimbinganSkripsiHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester,
					"Pembimbing Utama", label);
			BkdBimbinganSkripsiHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester,
					"Pembimbing Pendamping", label);
		} else {

			List<Dosen> dosens1 = session.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty("dosen1"))
					.add(Restrictions.isNotNull("dosen1")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			List<Dosen> dosens2 = session.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty("dosen2"))
					.add(Restrictions.isNotNull("dosen2")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			List<Dosen> dosens3 = session.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty("dosen3"))
					.add(Restrictions.isNotNull("dosen3")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			int i = 0;
			int rowCount = dosens1.size();
			for (Dosen dosen : dosens1) {
				if (label != null) {
					label.setValue("Memproses data bimbingan tugas akhir \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdBimbinganSkripsiHelper.populate(session, dosen, jenjang, tahunAkademik, semester, "Pembimbing Utama",
						label);
			}

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			dosens.addAll(dosens2);
			dosens.addAll(dosens3);

			System.out.println("jenjang => " + jenjang + ", banyak " + dosens.size());

			i = 0;
			rowCount = dosens.size();
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data bimbingan tugas akhir \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdBimbinganSkripsiHelper.populate(session, dosen, jenjang, tahunAkademik, semester,
						"Pembimbing Pendamping", label);
			}
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Implementasi kanonik: menghitung total SKS bimbingan tugas akhir {@code dosen} sebagai
	 * {@code sebagai} ("Pembimbing Utama"/"Pembimbing Pendamping") pada {@code jenjang}/
	 * {@code tahunAkademik}/{@code semester}, lalu menyimpan/memperbarui satu baris
	 * {@link AsesemenPenilaian} berisi keterangan rinci per mahasiswa bimbingan. Tidak melakukan apa
	 * pun bila {@code dosen} {@code null}/tidak berelasi pegawai, atau bila dosen tidak punya
	 * {@link Asesor} aktif, atau bila tidak ada mahasiswa bimbingan sama sekali pada periode
	 * tersebut ({@code jmlMhs == 0}).
	 */
	public static void populate(Session session, Dosen dosen, Jenjang jenjang, String tahunAkademik, String semester,
			String sebagai, Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			List<TahapanAtauCapaianPembelajaran> tahapanAtauCapaianPembelajarans = session
					.createCriteria(TahapanAtauCapaianPembelajaran.class).add(Restrictions.eq("jenjang", jenjang))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("jenis", TahapanAtauCapaianPembelajaran.TATAPAN_BIMBINGAN)).list();

			String keteranganSemua = "";
			Double sksTotal = 0.0;
			Integer jmlMhs = 0;
			for (TahapanAtauCapaianPembelajaran tahapanAtauCapaianPembelajaran : tahapanAtauCapaianPembelajarans) {

				Criterion criterion;
				if (sebagai.equals("Pembimbing Utama")) {
					criterion = Restrictions.eq("dosen1", dosen);
				} else {
					criterion = Restrictions.eq("dosen2", dosen);
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
				}

				List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = session
						.createCriteria(MahasiswaRequestTugasAkhir.class)
						.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
								Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
										Restrictions.or(
												Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
												Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
						.add(Restrictions.eq("jurusan.jenjang", jenjang)).add(criterion)
						.add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.in("semester",
								semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
						.add(Restrictions.eq("tahapanPenyusunanTugasAkhir", tahapanAtauCapaianPembelajaran)).list();

				Integer qtyBimbinganSkripis = mahasiswaRequestTugasAkhirs.size();
				jmlMhs += qtyBimbinganSkripis;
				if (qtyBimbinganSkripis > 0) {

					double jml = qtyBimbinganSkripis.doubleValue();

					Double sks = 0.0;

					String key = "jumlah_sks_bimbingan_tugas_akhir";
					String newKey = key + "_" + jenjang.getId() + "_" + sebagai + "_"
							+ tahapanAtauCapaianPembelajaran.getId();

					ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

					double maks = 6.0;
					try {
						maks = Double.parseDouble(konfigurasi.getInfo1());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/helper/BkdBimbinganSkripsiHelper.java:184");

					}

					if (jml > maks) {
						jml = maks;
					}

					try {
						sks = Double.parseDouble(konfigurasi.getNilai()) * jml;
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					String keterangan = dosen.getNama() + " membimbing tugas akhir sebanyak " + qtyBimbinganSkripis
							+ " mahasiswa (maks : " + Common.numberFormat.get().format(maks) + ") " + jenjang.getNama()
							+ ". Jumlah total sks yang di dapat sebanyak " + Common.numberFormat.get().format(sks)
							+ ", sebagai " + sebagai + ". ";

					for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
						keterangan += "\nMahasiswa \"" + mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + "-"
								+ mahasiswaRequestTugasAkhir.getMahasiswa().getNama()
								+ "\" sampai pada progres penyusunan \""
								+ mahasiswaRequestTugasAkhir.getTahapanPenyusunanTugasAkhir().getNama()
								+ "\" dengan judul \"" + mahasiswaRequestTugasAkhir.getJudul() + "\". ";
					}

					keteranganSemua += keterangan + ".  \n\n";

					sksTotal += sks;
				}
			}

			if (jmlMhs > 0) {
				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PEMBIMBING_TA))
						.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keteranganSemua);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PEMBIMBING_TA);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai(new Pegawai(dosen));
				asesemenPenilaian.setSks(sksTotal);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
			}
		}
	}

}
