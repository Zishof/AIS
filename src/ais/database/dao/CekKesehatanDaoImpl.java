package ais.database.dao;

import ais.database.model.CekKesehatan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.CekKesehatan} (data hasil cek
 * kesehatan mahasiswa baru), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class CekKesehatanDaoImpl extends
		GenericHibernateDao<CekKesehatan, Long, CekKesehatanDao> implements
		CekKesehatanDao {

}
