package ais.database.dao;

import ais.database.model.Ujian;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Ujian} (data ujian). Kelas ini
 * murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class UjianDaoImpl extends GenericHibernateDao<Ujian, Long, UjianDao>
		implements UjianDao {

}
