package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.KegiatanSatker;

/**
 * Implementasi Hibernate untuk {@link KegiatanSatkerDao}, mengelola entitas
 * {@link ais.database.model.rab.KegiatanSatker}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class KegiatanSatkerDaoImpl extends
		GenericHibernateDao<KegiatanSatker, Long, KegiatanSatkerDao> implements
		KegiatanSatkerDao {

}
