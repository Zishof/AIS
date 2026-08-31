package ais.database.dao;

import ais.database.model.BiodataPegawai;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BiodataPegawai} (biodata
 * pegawai), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BiodataPegawaiDaoImpl extends GenericHibernateDao<BiodataPegawai, Long, BiodataPegawaiDao> implements BiodataPegawaiDao{

}
