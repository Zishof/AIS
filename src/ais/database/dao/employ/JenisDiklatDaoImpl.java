package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisDiklat;

/**
 * Implementasi Hibernate untuk {@link JenisDiklatDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisDiklat}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisDiklatDaoImpl extends
		GenericHibernateDao<JenisDiklat, Long, JenisDiklatDao> implements
		JenisDiklatDao {

}
