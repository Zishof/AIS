package ais.database.dao;

import ais.database.model.JenisSekolahMahasiswaBaru;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenisSekolahMahasiswaBaru}
 * (jenis sekolah asal mahasiswa baru), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak
 * ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class JenisSekolahMahasiswaBaruDaoImpl extends GenericHibernateDao<JenisSekolahMahasiswaBaru, Long, JenisSekolahMahasiswaBaruDao> implements JenisSekolahMahasiswaBaruDao{

}
