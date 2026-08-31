package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisPelatihan;

/**
 * Implementasi Hibernate untuk {@link JenisPelatihanDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisPelatihan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisPelatihanDaoImpl extends
		GenericHibernateDao<JenisPelatihan, Long, JenisPelatihanDao> implements
		JenisPelatihanDao {

}
