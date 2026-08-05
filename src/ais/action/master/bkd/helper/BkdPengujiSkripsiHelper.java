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
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;

public class BkdPengujiSkripsiHelper {

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdPengujiSkripsiHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdPengujiSkripsiHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			String[] cols = new String[] { "penguji1", "penguji2", "penguji3" };

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			for (String col : cols) {
				dosens.addAll(session.createCriteria(Skripsi.class).add(Restrictions.eq("telahSidang", 1))
						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
						.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty(col))
						.add(Restrictions.isNotNull(col)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.in("semester",
								semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
						.list());
			}

			System.out.println("jenjang => " + jenjang + ", banyak " + dosens.size());

			int i = 0;
			int rowCount = dosens.size();
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data penguji skripsi \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdPengujiSkripsiHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, Dosen dosen, Jenjang jenjang, String tahunAkademik, String semester,
			Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)) )
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			Criterion criterion = Restrictions.eq("pembimbing", dosen);

			String[] cols = new String[] { "penguji1", "penguji2", "penguji3" };
			for (String c : cols) {
				criterion = Restrictions.or(criterion, Restrictions.eq(c, dosen));
			}

			List<Skripsi> skripsis = session.createCriteria(Skripsi.class).add(Restrictions.eq("telahSidang", 1))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).add(criterion)
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			Integer qtyBimbinganSkripis = skripsis.size();

			if (qtyBimbinganSkripis > 0) {

				double jml = qtyBimbinganSkripis.doubleValue();

				Double sks = 0.0;

				String key = "jumlah_sks_ujian_tugas_akhir";
				String newKey = key + "_" + jenjang.getId(); 

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				double maks = 8.0;
				try {
					maks = Double.parseDouble(konfigurasi.getInfo1());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/helper/BkdPengujiSkripsiHelper.java:121");

				}

				if (jml > maks) {
					jml = maks;
				}

				try {
					sks = Double.parseDouble(konfigurasi.getNilai()) * jml;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

				String keterangan = dosen.getNama() + " menguji tugas akhir sebanyak " + qtyBimbinganSkripis
						+ " mahasiswa " + jenjang.getNama() + ". Jumlah total sks yang di dapat sebanyak "
						+ Common.numberFormat.get().format(sks) + ". Maksimal mahasiswa yang bisa diuji adalah "
						+ Common.numberFormat.get().format(maks) + ". ";

				for (Skripsi skripsi : skripsis) {
					keterangan += "Mahasiswa \"" + skripsi.getMahasiswa().getNim() + "-"
							+ skripsi.getMahasiswa().getNama() + "\" dengan judul \"" + skripsi.getJudul() + "\". ";
				}

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class).add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PENGUJI_TA))
						.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENGUJI_TA);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai( new Pegawai(dosen));
				asesemenPenilaian.setSks(sks);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);

			}
		}
	}

}
