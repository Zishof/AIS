package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.Matauang;

/**
 * Implementasi Hibernate {@link MataUangDao} untuk entitas {@link ais.database.model.akunting.Matauang}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class MataUangDaoImpl extends
		GenericHibernateDao<Matauang, Long, MataUangDao> implements MataUangDao {

}
