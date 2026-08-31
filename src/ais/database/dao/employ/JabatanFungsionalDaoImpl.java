package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JabatanFungsional;

/**
 * Implementasi Hibernate untuk {@link JabatanFungsionalDao}, mengelola entitas
 * {@link ais.database.model.employ.JabatanFungsional}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JabatanFungsionalDaoImpl extends
		GenericHibernateDao<JabatanFungsional, Long, JabatanFungsionalDao>
		implements JabatanFungsionalDao {

}
 