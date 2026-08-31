package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JabatanFungsionalTambahan;

/**
 * Implementasi Hibernate untuk {@link JabatanFungsionalTambahanDao}, mengelola entitas
 * {@link ais.database.model.employ.JabatanFungsionalTambahan}. Kosong sesuai desain — seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JabatanFungsionalTambahanDaoImpl
		extends
		GenericHibernateDao<JabatanFungsionalTambahan, Long, JabatanFungsionalTambahanDao>
		implements JabatanFungsionalTambahanDao {

}
