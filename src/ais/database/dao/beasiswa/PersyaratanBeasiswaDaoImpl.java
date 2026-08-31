package ais.database.dao.beasiswa;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.beasiswa.PersyaratanBeasiswa;

/**
 * Implementasi Hibernate {@link PersyaratanBeasiswaDao} untuk entitas
 * {@link ais.database.model.beasiswa.PersyaratanBeasiswa}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class PersyaratanBeasiswaDaoImpl extends
		GenericHibernateDao<PersyaratanBeasiswa, Long, PersyaratanBeasiswaDao>
		implements PersyaratanBeasiswaDao {

}
