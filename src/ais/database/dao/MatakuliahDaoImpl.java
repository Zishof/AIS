package ais.database.dao;


import ais.database.model.Matakuliah;


/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Matakuliah} (data mata kuliah).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class MatakuliahDaoImpl extends GenericHibernateDao<Matakuliah, Long, MatakuliahDao> implements MatakuliahDao {
    


}
