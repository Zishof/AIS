package ais.database.dao;

import ais.database.model.JenisKegiatanDetail;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenisKegiatanDetail} (detail
 * jenis kegiatan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DetailJenisKegiatanDaoImpl extends GenericHibernateDao<JenisKegiatanDetail, Long, DetailJenisKegiatanDao> implements DetailJenisKegiatanDao{

}
