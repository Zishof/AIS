package ais.database.dao;


import ais.database.model.Beasiswa;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Beasiswa} (data beasiswa),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BeasiswaDaoImpl extends GenericHibernateDao<Beasiswa, Long, BeasiswaDao> implements BeasiswaDao {
    


}
