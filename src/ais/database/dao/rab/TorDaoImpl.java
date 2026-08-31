package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Tor;

/**
 * Implementasi Hibernate untuk {@link TorDao}, mengelola entitas {@link ais.database.model.rab.Tor}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class TorDaoImpl extends
		GenericHibernateDao<Tor, Long, TorDao> implements
		TorDao {

}
