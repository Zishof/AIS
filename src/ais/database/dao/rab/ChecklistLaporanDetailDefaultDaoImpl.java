package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.ChecklistLaporanDetailDefault;

/**
 * Implementasi Hibernate untuk {@link ChecklistLaporanDetailDefaultDao}, mengelola entitas
 * {@link ais.database.model.rab.ChecklistLaporanDetailDefault}. Kosong sesuai desain — seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class ChecklistLaporanDetailDefaultDaoImpl
		extends
		GenericHibernateDao<ChecklistLaporanDetailDefault, Long, ChecklistLaporanDetailDefaultDao>
		implements ChecklistLaporanDetailDefaultDao {

}
