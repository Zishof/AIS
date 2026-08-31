package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.KelompokLaporan;

/**
 * Implementasi Hibernate {@link KelompokLaporanDao} untuk entitas
 * {@link ais.database.model.akunting.KelompokLaporan}. Kelas ini sengaja kosong: seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class KelompokLaporanDaoImpl extends
		GenericHibernateDao<KelompokLaporan, Long, KelompokLaporanDao>
		implements KelompokLaporanDao {

}
