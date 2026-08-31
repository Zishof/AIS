package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisPimpinan;

/**
 * Implementasi Hibernate untuk {@link JenisPimpinanDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisPimpinan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisPimpinanDaoImpl extends
		GenericHibernateDao<JenisPimpinan, Long, JenisPimpinanDao> implements
		JenisPimpinanDao {

}
