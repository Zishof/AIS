package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;

public class BkdDosenPaHelper {

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdDosenPaHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdDosenPaHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			List<Long> dsns = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.groupProperty("dosen"))
					.createAlias("jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
					.add(Restrictions.isNotNull("dosen")).list();

			List<Dosen> dosens = session.createCriteria(Dosen.class)
					.add(dsns.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", dsns)).list();

			System.out.println("jenjang => " + jenjang + ", banyak " + dosens.size());

			int i = 0;
			int rowCount = dosens.size();
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data pembimbing akademik \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdDosenPaHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
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
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ ConstantValues.AKTIF.getId() + " and tahunakademik = '" + tahunAkademik + "' and semester%2="
					+ (semester.equals(Perkuliahan.GANJIL) ? 1 : 0) + ")";

			List<String> nims = session.createCriteria(KrsMahasiswa.class).add(Restrictions.sqlRestriction(sql))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.sqlRestriction(
							"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")))
					.add(Restrictions.eq("dosenPa", dosen)).createAlias("mahasiswa", "mahasiswa")
					.add(Restrictions.eq("mahasiswa.jenjang", jenjang))
					.setProjection(Projections.groupProperty("mahasiswa.nim")).list();
			Integer qtyBimbinganSkripis = nims.size();
			if (qtyBimbinganSkripis > 0) {

				double jml = qtyBimbinganSkripis.doubleValue();

				Double sks = 0.0;

				String key = "jumlah_sks_pembimbing_akademik_mahasiswa";
				String newKey = key + "_" + jenjang.getId();

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				double maks = 8.0;
				try {
					maks = Double.parseDouble(konfigurasi.getInfo1());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/helper/BkdDosenPaHelper.java:111");

				}

				if (jml > maks) {
					jml = maks;
				}

				try {
					sks = Double.parseDouble(konfigurasi.getNilai()) * jml;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				String keterangan = dosen.getNama() + " sebagai pembimbing akademik sebanyak " + nims.size()
						+ " mahasiswa " + jenjang.getNama() + " yang ber-status aktif, yaitu : " + nims
						+ ". Jumlah total sks yang di dapat sebanyak " + Common.numberFormat.get().format(sks)
						+ ". Maksimal mahasiswa yang bisa dibimbing " + Common.numberFormat.get().format(maks) + ". ";

				// String ketTambahan = "";
				// for (Object[] m : mhs) {
				// ketTambahan += ketTambahan.isEmpty() ? m[0] + "-" + m[1] : ",
				// " +
				// m[0] + "-" + m[1];
				// }
				//
				// keterangan += ketTambahan;

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PEMBIMBING_AKADEMIK))
						.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PEMBIMBING_AKADEMIK);
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
