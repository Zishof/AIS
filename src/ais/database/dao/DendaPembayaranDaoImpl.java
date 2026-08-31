package ais.database.dao;

import ais.database.model.DendaPembayaran;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.DendaPembayaran} (data denda
 * pembayaran), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DendaPembayaranDaoImpl extends
		GenericHibernateDao<DendaPembayaran, Long, DendaPembayaranDao>
		implements DendaPembayaranDao {

}
