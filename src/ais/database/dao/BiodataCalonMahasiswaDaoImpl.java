package ais.database.dao;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BiodataCalonMahasiswa}
 * (biodata calon mahasiswa/pendaftar), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak
 * ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class BiodataCalonMahasiswaDaoImpl extends GenericHibernateDao<BiodataCalonMahasiswa, Long, BiodataCalonMahasiswaDao> implements BiodataCalonMahasiswaDao{

}
