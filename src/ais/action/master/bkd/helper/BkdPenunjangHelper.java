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
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;

public class BkdPenunjangHelper {

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {

		Criterion criterion = pegawai == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("pegawai", pegawai);

		List<PenunjangKinerjaDosen> penunjangKinerjaDosens = session.createCriteria(PenunjangKinerjaDosen.class)
				.add(criterion)
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semester == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("semester", semester))

				.list();

		int rowCount = penunjangKinerjaDosens.size();
		int i = 0;
		for (PenunjangKinerjaDosen penunjangKinerjaDosen : penunjangKinerjaDosens) {

			List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("asesor", "asesor")
					.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
					.add(Restrictions.eq("pegawai", penunjangKinerjaDosen.getPegawai()))
					.setProjection(Projections.groupProperty("asesor")).list();

			System.out.println("pegawai => " + pegawai + ", asesors => " + asesors);
			if (!asesors.isEmpty()) {

				label.setValue("Memproses data penunjang dan lain-lain\"" + penunjangKinerjaDosen + "\" ("
						+ Common.numberFormat.get().format(i * 100.0 / rowCount) + "%)");
				i++;

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("penunjangKinerjaDosen", penunjangKinerjaDosen)).setMaxResults(1)
						.uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}

				asesemenPenilaian.setPenunjangKinerjaDosen(penunjangKinerjaDosen);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENUNJANG_DAN_LAIN_LAIN);
				asesemenPenilaian.setBidang(penunjangKinerjaDosen.getJenis());
				asesemenPenilaian.setKeterangan(penunjangKinerjaDosen.getNama());
				asesemenPenilaian.setTahunAkademik(penunjangKinerjaDosen.getTahunAkademik());
				asesemenPenilaian.setSemester(penunjangKinerjaDosen.getSemester());
				asesemenPenilaian.setPegawai(penunjangKinerjaDosen.getPegawai());
				asesemenPenilaian.setSks(penunjangKinerjaDosen.getSks());

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
			}
		}

	}

}
