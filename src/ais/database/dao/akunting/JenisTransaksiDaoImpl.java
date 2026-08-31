package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.JenisTransaksi;

/**
 * Implementasi Hibernate {@link JenisTransaksiDao} untuk entitas
 * {@link ais.database.model.akunting.JenisTransaksi}. Kelas ini sengaja kosong: seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class JenisTransaksiDaoImpl extends
		GenericHibernateDao<JenisTransaksi, Long, JenisTransaksiDao> implements
		JenisTransaksiDao {

}
