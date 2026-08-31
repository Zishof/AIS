package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.Peraturan;

/**
 * Implementasi Hibernate untuk {@link PeraturanDao}, mengelola entitas
 * {@link ais.database.model.employ.Peraturan}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class PeraturanDaoImpl extends GenericHibernateDao<Peraturan, Long, PeraturanDao>
		implements PeraturanDao {

}
