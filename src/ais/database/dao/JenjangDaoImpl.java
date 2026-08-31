package ais.database.dao;

import ais.database.model.Jenjang;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Jenjang} (data jenjang
 * pendidikan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JenjangDaoImpl extends
		GenericHibernateDao<Jenjang, Long, JenjangDao> implements JenjangDao {

}
