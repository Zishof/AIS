package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.Pensiun;

/**
 * Implementasi Hibernate untuk {@link PensiunDao}, mengelola entitas {@link ais.database.model.employ.Pensiun}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class PensiunDaoImpl extends
		GenericHibernateDao<Pensiun, Long, PensiunDao> implements PensiunDao {

}
