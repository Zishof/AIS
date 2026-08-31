package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Mitra;

/**
 * Implementasi Hibernate untuk {@link MitraDao}, mengelola entitas {@link ais.database.model.rab.Mitra}.
 * Kosong sesuai desain — seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class MitraDaoImpl extends
		GenericHibernateDao<Mitra, Long, MitraDao> implements
		MitraDao {

}
