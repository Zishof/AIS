package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.SkalaKegiatanKedosenan;

public class BkdKegiatanDosenHelper {
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {

		Criterion criterion = pegawai == null ? Restrictions.isNotNull("dosen.pegawaiId")
				: Restrictions.eq("dosen.pegawaiId", pegawai.getId());

		List<KegiatanKedosenanPunyaDosen> kegiatanKedosenanPunyaDosens = session
				.createCriteria(KegiatanKedosenanPunyaDosen.class).add(Restrictions.eq("persetujuan", true))
				.add(Restrictions.isNotNull("skalaKegiatanKedosenan"))
				.add(Restrictions.isNotNull("jabatanKegiatanKedosenan")).createAlias("dosen", "dosen").add(criterion)
				.createAlias("kegiatanKedosenan", "kegiatanKedosenan")
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kegiatanKedosenan.tahunAkademik", tahunAkademik))
				.add(semester == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kegiatanKedosenan.jenisSemester", semester))
				.list();

		int rowCount = kegiatanKedosenanPunyaDosens.size();
		int i = 0;
		for (KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen : kegiatanKedosenanPunyaDosens) {
			label.setValue("Memproses data kegiatan dosen \"" + kegiatanKedosenanPunyaDosen + "\" ("
					+ Common.numberFormat.get().format(i * 100.0 / rowCount) + "%)");
			i++;

			if (kegiatanKedosenanPunyaDosen.getDosen().getPegawaiId() != null)
				BkdKegiatanDosenHelper.populate(session, new Pegawai(kegiatanKedosenanPunyaDosen.getDosen()),
						tahunAkademik, semester, kegiatanKedosenanPunyaDosen);

		}

	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen) {

		if (pegawai == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai", pegawai)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("pegawai => " + pegawai + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = kegiatanKedosenanPunyaDosen
					.getKegiatanKedosenan().getDetailKelompokKegiatanKedosenan();
			SkalaKegiatanKedosenan skalaKegiatanKedosenan = kegiatanKedosenanPunyaDosen.getSkalaKegiatanKedosenan();
			JabatanKegiatanKedosenan jabatanKegiatanKedosenan = kegiatanKedosenanPunyaDosen
					.getJabatanKegiatanKedosenan();

			String newKey = "pengaturan_beban_sks_kegiatan_dosen_"
					+ detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getId() + "_"
					+ detailKelompokKegiatanKedosenan.getId() + "_" + skalaKegiatanKedosenan.getId() + "_"
					+ (jabatanKegiatanKedosenan == null ? "" : jabatanKegiatanKedosenan.getId());

			ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

			double sks = 0.0;
			try {
				sks = Double.parseDouble(konfigurasi.getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			String keterangan = pegawai.getNama() + " melakukan kegiatan \""
					+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama() + "\" pada aspek \""
					+ detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getNama() + " dengan rincian "
					+ detailKelompokKegiatanKedosenan.getNama() + "\" tingkat \"" + skalaKegiatanKedosenan.getNama()
					+ "\" sebagai \"" + jabatanKegiatanKedosenan.getNama() + "\" . SKS yang diporeh adalah "
					+ Common.numberFormat.get().format(sks);

			AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session.createCriteria(AsesemenPenilaian.class)
					.add(Restrictions.eq("pegawai", pegawai))
					.add(Restrictions.eq("kegiatanKedosenanPunyaDosen", kegiatanKedosenanPunyaDosen))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("semester", semester))
					.setMaxResults(1).uniqueResult();
			if (asesemenPenilaian == null) {
				asesemenPenilaian = new AsesemenPenilaian();
			}

			asesemenPenilaian.setSpesifikasi(PenilaianAsesor.KEGIATAN_DOSEN);
			asesemenPenilaian.setBidang(detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getJenis());
			asesemenPenilaian.setKeterangan(keterangan);
			asesemenPenilaian.setKegiatanKedosenanPunyaDosen(kegiatanKedosenanPunyaDosen);
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
