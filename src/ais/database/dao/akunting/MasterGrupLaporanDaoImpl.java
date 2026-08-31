package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.MasterGrupLaporan;

/**
 * Implementasi Hibernate {@link MasterGrupLaporanDao} untuk entitas
 * {@link ais.database.model.akunting.MasterGrupLaporan}. Kelas ini sengaja kosong: seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class MasterGrupLaporanDaoImpl extends
		GenericHibernateDao<MasterGrupLaporan, Long, MasterGrupLaporanDao>
		implements MasterGrupLaporanDao {

}
