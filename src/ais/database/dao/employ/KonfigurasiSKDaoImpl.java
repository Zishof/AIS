package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.KonfigurasiSK;

/**
 * Implementasi Hibernate untuk {@link KonfigurasiSKDao}, mengelola entitas
 * {@link ais.database.model.employ.KonfigurasiSK}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class KonfigurasiSKDaoImpl extends
		GenericHibernateDao<KonfigurasiSK, Long, KonfigurasiSKDao> implements
		KonfigurasiSKDao {

}
