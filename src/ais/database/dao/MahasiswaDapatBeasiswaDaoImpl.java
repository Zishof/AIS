package ais.database.dao;


import ais.database.model.MahasiswaDapatBeasiswa;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.MahasiswaDapatBeasiswa}
 * (relasi mahasiswa penerima suatu beasiswa), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class MahasiswaDapatBeasiswaDaoImpl extends GenericHibernateDao<MahasiswaDapatBeasiswa, Long, MahasiswaDapatBeasiswaDao> implements MahasiswaDapatBeasiswaDao {
    


}
