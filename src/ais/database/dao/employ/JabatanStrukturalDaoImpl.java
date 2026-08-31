package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JabatanStruktural;

/**
 * Implementasi Hibernate untuk {@link JabatanStrukturalDao}, mengelola entitas
 * {@link ais.database.model.employ.JabatanStruktural}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JabatanStrukturalDaoImpl extends
		GenericHibernateDao<JabatanStruktural, Long, JabatanStrukturalDao>
		implements JabatanStrukturalDao {

}
