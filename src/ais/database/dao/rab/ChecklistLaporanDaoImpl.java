package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.ChecklistLaporan;

/**
 * Implementasi Hibernate untuk {@link ChecklistLaporanDao}, mengelola entitas
 * {@link ais.database.model.rab.ChecklistLaporan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class ChecklistLaporanDaoImpl extends
		GenericHibernateDao<ChecklistLaporan, Long, ChecklistLaporanDao>
		implements ChecklistLaporanDao {

}
