package ais.database.dao;

import ais.database.model.JurusanSekolahMahasiswaBaru;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JurusanSekolahMahasiswaBaru}
 * (jurusan sekolah asal mahasiswa baru), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class JurusanSekolahMahasiswaBaruDaoImpl extends GenericHibernateDao<JurusanSekolahMahasiswaBaru, Long, JurusanSekolahMahasiswaBaruDao> implements JurusanSekolahMahasiswaBaruDao{

}
