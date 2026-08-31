package ais.database.dao;


import ais.database.model.Perkuliahan;


/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Perkuliahan} (data perkuliahan).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class PerkuliahanDaoImpl extends GenericHibernateDao<Perkuliahan, Long, PerkuliahanDao> implements PerkuliahanDao {
    


}
