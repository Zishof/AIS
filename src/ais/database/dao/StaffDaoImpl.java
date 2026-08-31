package ais.database.dao;

import ais.database.model.Staff;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Staff} (data staff). Kelas ini
 * murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class StaffDaoImpl extends GenericHibernateDao<Staff, Long, StaffDao>
		implements StaffDao {

}
