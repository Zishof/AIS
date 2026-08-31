package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.JenisTandaJasa;

/**
 * Implementasi Hibernate untuk {@link JenisTandaJasaDao}, mengelola entitas
 * {@link ais.database.model.employ.JenisTandaJasa}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisTandaJasaDaoImpl extends
		GenericHibernateDao<JenisTandaJasa, Long, JenisTandaJasaDao> implements
		JenisTandaJasaDao {

}
