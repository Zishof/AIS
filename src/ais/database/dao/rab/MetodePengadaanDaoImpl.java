package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.MetodePengadaan;

/**
 * Implementasi Hibernate untuk {@link MetodePengadaanDao}, mengelola entitas
 * {@link ais.database.model.rab.MetodePengadaan}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class MetodePengadaanDaoImpl extends
		GenericHibernateDao<MetodePengadaan, Long, MetodePengadaanDao> implements
		MetodePengadaanDao {

}
