package ais.database.dao;

import ais.database.model.DendaPembayaranNominal;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.DendaPembayaranNominal} (data
 * nominal denda pembayaran), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DendaPembayaranNominalDaoImpl extends
		GenericHibernateDao<DendaPembayaranNominal, Long, DendaPembayaranNominalDao>
		implements DendaPembayaranNominalDao {

}
