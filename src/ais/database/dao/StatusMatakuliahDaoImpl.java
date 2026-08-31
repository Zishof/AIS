package ais.database.dao;

import ais.database.model.StatusMatakuliah;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.StatusMatakuliah} (data referensi
 * status mata kuliah). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class StatusMatakuliahDaoImpl extends GenericHibernateDao<StatusMatakuliah, Long, StatusMatakuliahDao> implements StatusMatakuliahDao{

}
