package ais.database.dao.beasiswa;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.beasiswa.BeasiswaPunyaPersyaratan;

/**
 * Implementasi Hibernate {@link BeasiswaPunyaPersyaratanDao} untuk entitas
 * {@link ais.database.model.beasiswa.BeasiswaPunyaPersyaratan}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class BeasiswaPunyaPersyaratanDaoImpl
		extends
		GenericHibernateDao<BeasiswaPunyaPersyaratan, Long, BeasiswaPunyaPersyaratanDao>
		implements BeasiswaPunyaPersyaratanDao {

}
