package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.AkunPajak;

/**
 * Implementasi Hibernate {@link AkunPajakDao} untuk entitas {@link ais.database.model.akunting.AkunPajak}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class AkunPajakDaoImpl extends
		GenericHibernateDao<AkunPajak, Long, AkunPajakDao> implements
		AkunPajakDao {

}
