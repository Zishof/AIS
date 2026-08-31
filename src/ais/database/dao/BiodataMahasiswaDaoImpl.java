package ais.database.dao;

import ais.database.model.BiodataMahasiswa;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BiodataMahasiswa} (biodata
 * mahasiswa), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BiodataMahasiswaDaoImpl extends GenericHibernateDao<BiodataMahasiswa, Long, BiodataMahasiswaDao> implements BiodataMahasiswaDao{

}
