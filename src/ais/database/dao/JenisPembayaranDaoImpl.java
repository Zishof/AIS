package ais.database.dao;

import ais.database.model.JenisPembayaran;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenisPembayaran} (jenis
 * pembayaran), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JenisPembayaranDaoImpl extends GenericHibernateDao<JenisPembayaran, Long, JenisPembayaranDao> implements JenisPembayaranDao{

}
