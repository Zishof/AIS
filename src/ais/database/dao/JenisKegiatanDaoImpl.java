package ais.database.dao;

import ais.database.model.JenisKegiatan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenisKegiatan} (jenis
 * kegiatan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JenisKegiatanDaoImpl extends GenericHibernateDao<JenisKegiatan, Long, JenisKegiatanDao> implements JenisKegiatanDao {

}
