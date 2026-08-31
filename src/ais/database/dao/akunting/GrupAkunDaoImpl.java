package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.GrupAkun;

/**
 * Implementasi Hibernate {@link GrupAkunDao} untuk entitas {@link ais.database.model.akunting.GrupAkun}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class GrupAkunDaoImpl extends
		GenericHibernateDao<GrupAkun, Long, GrupAkunDao> implements GrupAkunDao {

}
