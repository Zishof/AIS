package ais.database.dao;


import ais.database.model.Dosen;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Dosen} (data dosen), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DosenDaoImpl extends GenericHibernateDao<Dosen, Long, DosenDao> implements DosenDao {
    


}
