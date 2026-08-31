package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Acara;

/**
 * Implementasi Hibernate untuk {@link AcaraDao}, mengelola entitas {@link ais.database.model.rab.Acara}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class AcaraDaoImpl extends
		GenericHibernateDao<Acara, Long, AcaraDao> implements
		AcaraDao {

}
