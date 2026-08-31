package ais.database.dao;

import ais.database.model.Kegiatan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Kegiatan} (data kegiatan),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KegiatanDaoImpl extends GenericHibernateDao<Kegiatan, Long, KegiatanDao> implements KegiatanDao{

}
