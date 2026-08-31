package ais.database.dao;


import ais.database.model.Fakultas;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Fakultas} (data fakultas),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class FakultasDaoImpl extends GenericHibernateDao<Fakultas, Long, FakultasDao> implements FakultasDao {
    


}
