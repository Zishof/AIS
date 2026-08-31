package ais.database.dao;


import ais.database.model.Jurusan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Jurusan} (data jurusan),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JurusanDaoImpl extends GenericHibernateDao<Jurusan, Long, JurusanDao> implements JurusanDao {
    


}
