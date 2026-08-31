package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Satuan;

/**
 * Implementasi Hibernate untuk {@link SatuanDao}, mengelola entitas {@link ais.database.model.rab.Satuan}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class SatuanDaoImpl extends
		GenericHibernateDao<Satuan, Long, SatuanDao> implements
		SatuanDao {

}
