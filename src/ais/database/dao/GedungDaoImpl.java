package ais.database.dao;

import ais.database.model.Gedung;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Gedung} (data gedung), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class GedungDaoImpl extends GenericHibernateDao<Gedung, Long, GedungDao> implements GedungDao{

}
