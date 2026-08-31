package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Tugas;

/**
 * Implementasi Hibernate untuk {@link TugasDao}, mengelola entitas {@link ais.database.model.rab.Tugas}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class TugasDaoImpl extends
		GenericHibernateDao<Tugas, Long, TugasDao> implements
		TugasDao {

}
