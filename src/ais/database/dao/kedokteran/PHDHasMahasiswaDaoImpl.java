package ais.database.dao.kedokteran;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.kedokteran.PHDHasMahasiswa;

/**
 * Implementasi Hibernate {@link PHDHasMahasiswaDao} untuk entitas
 * {@link ais.database.model.kedokteran.PHDHasMahasiswa}. Kelas ini sengaja kosong: seluruh logika
 * CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class PHDHasMahasiswaDaoImpl extends
		GenericHibernateDao<PHDHasMahasiswa, Long, PHDHasMahasiswaDao>
		implements PHDHasMahasiswaDao {

}
