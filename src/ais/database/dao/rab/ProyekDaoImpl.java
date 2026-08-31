package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Proyek;

/**
 * Implementasi Hibernate untuk {@link ProyekDao}, mengelola entitas {@link ais.database.model.rab.Proyek}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class ProyekDaoImpl extends GenericHibernateDao<Proyek, Long, ProyekDao>
		implements ProyekDao {

}
