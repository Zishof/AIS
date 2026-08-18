package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswa;

public class JadwalUtil {

	@SuppressWarnings("unchecked")
	public static List<Long> ambilJadwal(Guru guru, KelasSiswa kelasSiswa) {
		Session session = HibernateUtil.currentNativeSession();

		Criterion criterion = Restrictions.or(Restrictions.eq("guru", guru), Restrictions.eq("guru2", guru));

		criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru11", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru12", guru));

		List<Long> mt = session.createCriteria(JadwalPelajaran.class).add(criterion)
				.add(Restrictions.eq("kelas", kelasSiswa)).setProjection(Projections.groupProperty("matapelajaran.id"))
				.list();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		return mt;
	}

}
