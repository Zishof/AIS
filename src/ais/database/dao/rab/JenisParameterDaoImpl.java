package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.JenisParameter;

/**
 * Implementasi Hibernate untuk {@link JenisParameterDao}, mengelola entitas
 * {@link ais.database.model.rab.JenisParameter}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisParameterDaoImpl extends
		GenericHibernateDao<JenisParameter, Long, JenisParameterDao> implements
		JenisParameterDao {

}
