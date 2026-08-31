package ais.database.dao;


import ais.database.model.Ruang;


/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Ruang} (data referensi ruang
 * kuliah). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class RuangDaoImpl extends GenericHibernateDao<Ruang, Long, RuangDao> implements RuangDao {
    


}
