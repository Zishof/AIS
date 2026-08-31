package ais.database.dao;


import ais.database.model.Pkl;


/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Pkl} (data praktik kerja lapangan).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class PklDaoImpl extends GenericHibernateDao<Pkl, Long, PklDao> implements PklDao {
    


}
