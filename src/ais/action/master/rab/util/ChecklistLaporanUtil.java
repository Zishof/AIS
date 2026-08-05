package ais.action.master.rab.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.ChecklistLaporan;
import ais.database.model.rab.ChecklistLaporanDetail;
import ais.database.model.rab.ChecklistLaporanDetailDefault;

public class ChecklistLaporanUtil {

	@SuppressWarnings("unchecked")
	public static void importChecklistDefault(
			ChecklistLaporan checklistLaporan) {
		Session session = HibernateUtil.currentNativeSession();
		List<ChecklistLaporanDetailDefault> checklistLaporanDetailDefaults = session
				.createCriteria(ChecklistLaporanDetailDefault.class)
				.addOrder(Order.asc("id")).list();

		Long nilaiMin = Long.MIN_VALUE + 2000000000000000000L;
		Long nilaiMax = nilaiMin + 1000000000000000000L;

		Long nilaiMinInDb = (Long) session
				.createCriteria(ChecklistLaporanDetail.class)
				.add(Restrictions.gt("id", nilaiMin))
				.add(Restrictions.lt("id", nilaiMax))
				.setProjection(Projections.max("id")).uniqueResult();

		if (nilaiMinInDb == null) {
			nilaiMinInDb = nilaiMin;
		}
		nilaiMinInDb += 10000;

		session.getTransaction().begin();
		List<ChecklistLaporanDetail> checklistLaporanDetails = new ArrayList<ChecklistLaporanDetail>();
		for (ChecklistLaporanDetailDefault checklistLaporanDetailDefault : checklistLaporanDetailDefaults) {
			ChecklistLaporanDetail checklistLaporanDetail = new ChecklistLaporanDetail();
			checklistLaporanDetail.setAda(false);
			checklistLaporanDetail.setChecklistLaporan(checklistLaporan);
			checklistLaporanDetail.setDeep(null);
			checklistLaporanDetail.setDiperlukan(false);
			checklistLaporanDetail.setKeterangan(checklistLaporanDetailDefault
					.getKeterangan());
			checklistLaporanDetail.setKode(checklistLaporanDetailDefault
					.getKode());
			checklistLaporanDetail.setNama(checklistLaporanDetailDefault
					.getNama());
			checklistLaporanDetail.setCopyDefault(checklistLaporanDetailDefault
					.getId());
			checklistLaporanDetail
					.setCopyParentDefault(checklistLaporanDetailDefault
							.getParent() == null ? null
							: checklistLaporanDetailDefault.getParent().getId());
			session.save(checklistLaporanDetail);
			checklistLaporanDetails.add(checklistLaporanDetail);
		}
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		for (ChecklistLaporanDetail checklistLaporanDetail : checklistLaporanDetails) {
			Long parent = checklistLaporanDetail.getCopyParentDefault();
			Long id = checklistLaporanDetail.getCopyDefault();
			id = nilaiMinInDb + id;
			if (parent != null) {
				parent = nilaiMinInDb + parent;
			}
			String sql = "update rab.checklist_laporan_detail set parent = "
					+ parent + ", id = " + id + " where id = "
					+ checklistLaporanDetail.getId();
			// System.out.println(sql);
			session.createSQLQuery(sql).executeUpdate();
		}
		session.getTransaction().commit();
		HibernateUtil.closeSession();
	}

}
