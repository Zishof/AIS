package ais.database.dao.beasiswa;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan;

/**
 * Implementasi Hibernate {@link MahasiswaBeasiswaPersyaratanDao} untuk entitas
 * {@link ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan}. Kelas ini sengaja kosong:
 * seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat
 * Javadoc di sana untuk detail perilaku method.
 */
public class MahasiswaBeasiswaPersyaratanDaoImpl
		extends
		GenericHibernateDao<MahasiswaBeasiswaPersyaratan, Long, MahasiswaBeasiswaPersyaratanDao>
		implements MahasiswaBeasiswaPersyaratanDao {

}
