package ais.database.dao.beasiswa;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;

/**
 * Implementasi Hibernate {@link MahasiswaDaftarBeasiswaDao} untuk entitas
 * {@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class MahasiswaDaftarBeasiswaDaoImpl
		extends
		GenericHibernateDao<MahasiswaDaftarBeasiswa, Long, MahasiswaDaftarBeasiswaDao>
		implements MahasiswaDaftarBeasiswaDao {

}
