package ais.database.dao;


import ais.database.model.Program;


/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Program} (data program studi).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class ProgramDaoImpl extends GenericHibernateDao<Program, Long, ProgramDao> implements ProgramDao {
    


}
