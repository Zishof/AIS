package ais.database.dao;


import ais.database.model.MahasiswaDapatKkn;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.MahasiswaDapatKkn} (relasi
 * mahasiswa peserta suatu program KKN), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class MahasiswaDapatKknDaoImpl extends GenericHibernateDao<MahasiswaDapatKkn, Long, MahasiswaDapatKknDao> implements MahasiswaDapatKknDao {
    


}
