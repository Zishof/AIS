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

/**
 * Helper penghitung dan penyimpan penilaian beban kinerja dosen (BKD) untuk peran
 * <b>penguji proposal tugas akhir</b>: menghitung jumlah mahasiswa (dengan status pengajuan tugas
 * akhir Mengulang/Seminar/Lulus/Aktif) yang diuji seorang dosen pada posisi dosen4/5/6
 * (penguji) dari {@link MahasiswaRequestTugasAkhir}, mengonversinya menjadi SKS penunjang kinerja
 * lewat konfigurasi {@code jumlah_sks_proposal_ujian_tugas_akhir[_jenjangId]} (dengan batas
 * maksimal mahasiswa yang dihitung, dari {@code info1} konfigurasi tersebut, default 8), lalu
 * menyimpannya sebagai {@link AsesemenPenilaian} dan memicu pemeriksaan status penilaian asesor
 * terkait. Berpola sama dengan {@link BkdKknHelper}: tiga overload {@code populate} membentuk
 * hierarki cakupan seluruh jenjang -> satu jenjang -> satu dosen tertentu.
 */
public class BkdPengujiProposalSkripsiHelper {

	/**
	 * Memproses seluruh {@link Jenjang} aktif untuk {@code pegawai} (atau, bila {@code pegawai}
	 * bukan dosen, untuk seluruh dosen penguji proposal yang ditemukan) pada
	 * {@code tahunAkademik}/{@code semester}, mendelegasikan per jenjang ke
	 * {@link #populate(Session, Pegawai, Jenjang, String, String, Label)}.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       pegawai yang diproses; bila bukan dosen, seluruh dosen penguji diproses
	 * @param tahunAkademik tahun akademik yang dinilai
	 * @param semester      semester yang dinilai
	 * @param label         komponen label UI untuk progres (boleh {@code null})
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdPengujiProposalSkripsiHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	/**
	 * Memproses satu {@code jenjang} untuk {@code pegawai}: bila {@code pegawai} memiliki data
	 * dosen, mendelegasikan langsung ke {@link #populate(Session, Dosen, Jenjang, String, String, Label)};
	 * bila tidak, method mengumpulkan seluruh dosen yang tercatat di posisi penguji (dosen4/5/6)
	 * pada {@link MahasiswaRequestTugasAkhir} dengan status relevan untuk jenjang/tahun
	 * akademik/semester tersebut, lalu memproses masing-masing dosen satu per satu.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       pegawai yang diproses, atau {@code null}/non-dosen untuk memproses seluruh dosen penguji
	 * @param jenjang       jenjang pendidikan yang diproses
	 * @param tahunAkademik tahun akademik yang dinilai
	 * @param semester      semester yang dinilai
	 * @param label         komponen label UI untuk progres (boleh {@code null})
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdPengujiProposalSkripsiHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester,
					label);
		} else {

			List<Dosen> dosens1 = session.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty("dosen4"))
					.add(Restrictions.isNotNull("dosen4")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			List<Dosen> dosens2 = session.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty("dosen5"))
					.add(Restrictions.isNotNull("dosen5")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			List<Dosen> dosens3 = session.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty("dosen6"))
					.add(Restrictions.isNotNull("dosen6")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			dosens.addAll(dosens1);
			dosens.addAll(dosens2);
			dosens.addAll(dosens3);

			System.out.println("jenjang => " + jenjang + ", banyak " + dosens.size());

			int i = 0;
			int rowCount = dosens.size();
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data penguji proposal tugas akhir \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdPengujiProposalSkripsiHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	/**
	 * Menghitung dan menyimpan penilaian penguji proposal tugas akhir untuk satu {@code dosen}
	 * tertentu. Tidak melakukan apa pun bila dosen tidak memiliki asesor aktif atau tidak menguji
	 * mahasiswa relevan pada jenjang/tahun akademik/semester tersebut. Bila memenuhi syarat:
	 * jumlah mahasiswa yang diuji (dibatasi maksimal dari konfigurasi) dikalikan nilai SKS per
	 * mahasiswa dari konfigurasi {@code jumlah_sks_proposal_ujian_tugas_akhir[_jenjangId]},
	 * disimpan/diperbarui sebagai {@link AsesemenPenilaian} (spesifikasi
	 * {@link PenilaianAsesor#PENGUJI_PROPOSAL_TA}) dalam transaksi Hibernate sendiri, dan status
	 * penilaian asesor terkait diperbarui lewat {@link PenilaianAsesorAction#checkPenilaian}.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param dosen         dosen yang dinilai
	 * @param jenjang       jenjang pendidikan yang diproses
	 * @param tahunAkademik tahun akademik yang dinilai
	 * @param semester      semester yang dinilai
	 * @param label         komponen label UI untuk progres (tidak dipakai pada overload ini)
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, Dosen dosen, Jenjang jenjang, String tahunAkademik, String semester,
			Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			Criterion criterion = Restrictions.eq("dosen4", dosen);
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));

			List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = session
					.createCriteria(MahasiswaRequestTugasAkhir.class)
					.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
							Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).add(criterion)
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			Integer qtyBimbinganSkripis = mahasiswaRequestTugasAkhirs.size();

			if (qtyBimbinganSkripis > 0) {

				double jml = qtyBimbinganSkripis.doubleValue();

				Double sks = 0.0;

				String key = "jumlah_sks_proposal_ujian_tugas_akhir";
				String newKey = key + "_" + jenjang.getId();

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				double maks = 8.0;
				try {
					maks = Double.parseDouble(konfigurasi.getInfo1());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/helper/BkdPengujiProposalSkripsiHelper.java:152");

				}

				if (jml > maks) {
					jml = maks;
				}

				try {
					sks = Double.parseDouble(konfigurasi.getNilai()) * jml;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				String keterangan = dosen.getNama() + " menguji proposal tugas akhir sebanyak " + qtyBimbinganSkripis
						+ " mahasiswa " + jenjang.getNama() + ". Jumlah total sks yang di dapat sebanyak "
						+ Common.numberFormat.get().format(sks) + ". Maksimal mahasiswa yang bisa diuji proposalnya "
						+ Common.numberFormat.get().format(maks) + ". ";

				for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
					keterangan += "Mahasiswa \"" + mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + "-"
							+ mahasiswaRequestTugasAkhir.getMahasiswa().getNama() + "\" dengan judul \""
							+ mahasiswaRequestTugasAkhir.getJudul() + "\". ";
				}

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PENGUJI_PROPOSAL_TA))
						.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENGUJI_PROPOSAL_TA);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai(new Pegawai(dosen));
				asesemenPenilaian.setSks(sks);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
			}
		}
	}

}
