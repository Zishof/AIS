package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.Keluarga;

/**
 * Implementasi Hibernate untuk {@link KeluargaPegawaiDao}, mengelola entitas
 * {@link ais.database.model.employ.Keluarga}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class KeluargaPegawaiDaoImpl extends
		GenericHibernateDao<Keluarga, Long, KeluargaPegawaiDao> implements
		KeluargaPegawaiDao {

}
