package ais.database.dao;


import ais.database.model.Mahasiswa;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Mahasiswa} (data mahasiswa),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class MahasiswaDaoImpl extends GenericHibernateDao<Mahasiswa, Long, MahasiswaDao> implements MahasiswaDao {
    


}
