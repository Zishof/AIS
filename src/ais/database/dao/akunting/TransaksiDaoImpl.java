package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.Transaksi;

/**
 * Implementasi Hibernate {@link TransaksiDao} untuk entitas {@link ais.database.model.akunting.Transaksi}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class TransaksiDaoImpl extends
		GenericHibernateDao<Transaksi, Long, TransaksiDao> implements
		TransaksiDao {

}
