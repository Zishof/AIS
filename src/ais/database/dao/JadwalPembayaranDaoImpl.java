package ais.database.dao;

import ais.database.model.JadwalPembayaran;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JadwalPembayaran} (jadwal
 * pembayaran), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JadwalPembayaranDaoImpl extends GenericHibernateDao<JadwalPembayaran, Long, JadwalPembayaranDao> implements JadwalPembayaranDao{

}
