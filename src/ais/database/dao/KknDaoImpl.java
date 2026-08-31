package ais.database.dao;


import ais.database.model.Kkn;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Kkn} (data program KKN /
 * Kuliah Kerja Nyata), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KknDaoImpl extends GenericHibernateDao<Kkn, Long, KknDao> implements KknDao {
    


}
