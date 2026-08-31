package ais.database.dao;


import ais.database.model.Detailperkuliahan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Detailperkuliahan} (detail
 * pertemuan/perkuliahan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DetailperkuliahanDaoImpl extends GenericHibernateDao<Detailperkuliahan, Long, DetailperkuliahanDao> implements DetailperkuliahanDao {
    


}
