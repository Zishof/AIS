package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.JenisTugas;

/**
 * Implementasi Hibernate untuk {@link JenisTugasDao}, mengelola entitas
 * {@link ais.database.model.rab.JenisTugas}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisTugasDaoImpl extends
		GenericHibernateDao<JenisTugas, Long, JenisTugasDao> implements
		JenisTugasDao {

}
