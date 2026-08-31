package ais.database.dao;

import ais.database.model.Jabatan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Jabatan} (data jabatan),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JabatanDaoImpl extends GenericHibernateDao<Jabatan, Long, JabatanDao> implements JabatanDao{

}
