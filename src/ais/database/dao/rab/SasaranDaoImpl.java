package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Sasaran;

/**
 * Implementasi Hibernate untuk {@link SasaranDao}, mengelola entitas {@link ais.database.model.rab.Sasaran}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class SasaranDaoImpl extends
		GenericHibernateDao<Sasaran, Long, SasaranDao> implements SasaranDao {

}
