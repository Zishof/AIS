package ais.database.dao;

import ais.database.model.Judisium;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Judisium} (data
 * yudisium/predikat kelulusan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JudisiumDaoImpl extends
		GenericHibernateDao<Judisium, Long, JudisiumDao> implements JudisiumDao {

}
