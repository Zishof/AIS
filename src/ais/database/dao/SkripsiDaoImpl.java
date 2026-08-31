package ais.database.dao;


import ais.database.model.Skripsi;


/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Skripsi} (data skripsi mahasiswa).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class SkripsiDaoImpl extends GenericHibernateDao<Skripsi, Long, SkripsiDao> implements SkripsiDao {
    


}
