package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisPensiun;

/**
 * Implementasi Hibernate untuk {@link JenisPensiunDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisPensiun}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisPensiunDaoImpl extends
		GenericHibernateDao<JenisPensiun, Long, JenisPensiunDao> implements
		JenisPensiunDao {

}
