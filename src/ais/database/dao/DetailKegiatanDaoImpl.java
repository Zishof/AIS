package ais.database.dao;

import ais.database.model.DetailKegiatan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.DetailKegiatan} (detail
 * kegiatan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DetailKegiatanDaoImpl extends GenericHibernateDao<DetailKegiatan, Long, DetailKegiatanDao> implements DetailKegiatanDao{

}
