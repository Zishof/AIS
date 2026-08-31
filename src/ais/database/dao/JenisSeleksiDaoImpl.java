package ais.database.dao;

import ais.database.model.JenisSeleksi;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenisSeleksi} (jenis seleksi
 * penerimaan mahasiswa baru), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JenisSeleksiDaoImpl extends GenericHibernateDao<JenisSeleksi, Long, JenisSeleksiDao> implements JenisSeleksiDao{

}
