package ais.database.dao;

import ais.database.model.StatusLulusCalonMahasiswa;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.StatusLulusCalonMahasiswa} (data
 * status lulus calon mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class StatusLulusCalonMahasiswaDaoImpl extends GenericHibernateDao<StatusLulusCalonMahasiswa, Long, StatusLulusCalonMahasiswaDao> implements StatusLulusCalonMahasiswaDao{

}
