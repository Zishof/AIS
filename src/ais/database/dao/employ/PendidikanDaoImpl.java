package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.Pendidikan;

/**
 * Implementasi Hibernate untuk {@link PendidikanDao}, mengelola entitas
 * {@link ais.database.model.employ.Pendidikan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class PendidikanDaoImpl extends GenericHibernateDao<Pendidikan, Long, PendidikanDao>
		implements PendidikanDao {

}
