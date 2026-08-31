package ais.database.dao;

import ais.database.model.Komentar;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Komentar} (data komentar),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KomentarDaoImpl extends GenericHibernateDao<Komentar, Long, KomentarDao> implements KomentarDao{

}
