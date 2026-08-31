package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.KenaikanPangkat;

/**
 * Implementasi Hibernate untuk {@link KenaikanPangkatDao}, mengelola entitas
 * {@link ais.database.model.employ.KenaikanPangkat}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class KenaikanPangkatDaoImpl extends
		GenericHibernateDao<KenaikanPangkat, Long, KenaikanPangkatDao> implements
		KenaikanPangkatDao {

}