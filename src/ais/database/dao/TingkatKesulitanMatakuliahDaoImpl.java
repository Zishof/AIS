package ais.database.dao;

import ais.database.model.TingkatKesulitanMatakuliah;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TingkatKesulitanMatakuliah} (data
 * referensi tingkat kesulitan mata kuliah). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TingkatKesulitanMatakuliahDaoImpl extends GenericHibernateDao<TingkatKesulitanMatakuliah, Long, TingkatKesulitanMatakuliahDao> implements TingkatKesulitanMatakuliahDao{

}
