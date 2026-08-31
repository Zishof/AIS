package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.HasilSatuan;

/**
 * Implementasi Hibernate untuk {@link HasilSatuanDao}, mengelola entitas
 * {@link ais.database.model.rab.HasilSatuan}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class HasilSatuanDaoImpl extends
		GenericHibernateDao<HasilSatuan, Long, HasilSatuanDao> implements
		HasilSatuanDao {

}
