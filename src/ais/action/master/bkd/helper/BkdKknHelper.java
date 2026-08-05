package ais.action.master.bkd.helper;

import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.KonfigurasiBkdAction;
import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.CommonVO;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;

public class BkdKknHelper {

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdKknHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdKknHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			String[] cols = new String[] { "dosen_pembimbing1", "dosen_pembimbing2", "dosen_pembimbing3",
					"dosen_pembimbing4", "dosen_pembimbing5" };

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			for (String col : cols) {
				dosens.addAll(session.createCriteria(MahasiswaDapatKelompokKkn.class)
						.add(Restrictions.eq("diterima", true)).createAlias("kelompokKkn", "kelompokKkn")
						.createAlias("kelompokKkn.kkn", "kkn").createAlias("mahasiswa", "mahasiswa")
						.createAlias("mahasiswa.jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
						.setProjection(Projections.groupProperty("kelompokKkn." + col))
						.add(Restrictions.isNotNull("kelompokKkn." + col))
						.add(Restrictions.eq("kkn.tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("kkn.semester", semester)).list());
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
				BkdKknHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
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

			String key = "jumlah_sks_pembimbing_kkn";
			String newKey = key + "_" + jenjang.getId();

			ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "1-25=1;26-50=2;51-75=3;76-100=4");

			List<CommonVO> commonVOs = KonfigurasiBkdAction.terjemahkanNilai(konfigurasi.getNilai().trim(),
					konfigurasi.getInfo1().trim());

			System.out.println("commonVOs " + konfigurasi.getNilai().trim() + " => " + commonVOs);

			Criterion criterion = Restrictions.eq("kelompokKkn.dosen_pembimbing1", dosen);

			String[] cols = new String[] { "dosen_pembimbing2", "dosen_pembimbing3", "dosen_pembimbing4",
					"dosen_pembimbing5" };
			for (String c : cols) {
				criterion = Restrictions.or(criterion, Restrictions.eq("kelompokKkn." + c, dosen));
			}

			double jumlahMhs = ((Number) session.createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("diterima", true)).createAlias("kelompokKkn", "kelompokKkn")
					.createAlias("kelompokKkn.kkn", "kkn").createAlias("mahasiswa", "mahasiswa")
					.createAlias("mahasiswa.jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
					.setProjection(Projections.rowCount()).add(criterion)
					.add(Restrictions.eq("kkn.tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("kkn.semester", semester)).uniqueResult()).doubleValue();

			if (jumlahMhs > 0.01) {

				double sks = 0.0;
				String keterangan = dosen.getNama() + " membimbing KKN sebanyak "
						+ Common.numberFormat.get().format(jumlahMhs) + " mahasiswa " + jenjang.getNama() + ".";

				String keteranganTambahan = "";
				CommonVO commonVOPilihan = null;
				for (CommonVO commonVO : commonVOs) {
					if (jumlahMhs >= commonVO.getMulai() && jumlahMhs <= commonVO.getSampai()) {
						sks = commonVO.getNilai();
						commonVOPilihan = commonVO;
						break;
					}
				}

				if (commonVOPilihan != null) {
					keteranganTambahan = "Mulai " + Common.numberFormat.get().format(commonVOPilihan.getMulai()) + " s.d "
							+ Common.numberFormat.get().format(commonVOPilihan.getSampai()) + " "
							+ (commonVOPilihan.getPersen()
									? "persen yang di dapat dari SKS "
											+ Common.numberFormat.get().format(commonVOPilihan.getNilai()) + "%"
									: "nilai yang di dapat adalah "
											+ Common.numberFormat.get().format(commonVOPilihan.getNilai()) + " SKS.")
							+ ", sehingga jumlah SKS akhir yang diperoleh adalah " + Common.numberFormat.get().format(sks);
				}

				keterangan += (" " + keteranganTambahan);

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PEMBIMBING_KKN))
						.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("jenjang", jenjang))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}

				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PEMBIMBING_KKN);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
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
