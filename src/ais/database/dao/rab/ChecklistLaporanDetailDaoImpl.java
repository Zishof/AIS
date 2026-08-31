package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.ChecklistLaporanDetail;

/**
 * Implementasi Hibernate untuk {@link ChecklistLaporanDetailDao}, mengelola entitas
 * {@link ais.database.model.rab.ChecklistLaporanDetail}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class ChecklistLaporanDetailDaoImpl
		extends
		GenericHibernateDao<ChecklistLaporanDetail, Long, ChecklistLaporanDetailDao>
		implements ChecklistLaporanDetailDao {

}
