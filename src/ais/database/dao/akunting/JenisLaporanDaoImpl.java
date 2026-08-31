package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.JenisLaporan;

/**
 * Implementasi Hibernate {@link JenisLaporanDao} untuk entitas
 * {@link ais.database.model.akunting.JenisLaporan}. Kelas ini sengaja kosong: seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class JenisLaporanDaoImpl extends
		GenericHibernateDao<JenisLaporan, Long, JenisLaporanDao> implements
		JenisLaporanDao {

}
