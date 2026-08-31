package ais.database.dao;

import ais.database.model.BaypassPembayaranMahasiswa;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BaypassPembayaranMahasiswa}
 * (data bypass/pengecualian kewajiban pembayaran mahasiswa), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BaypassPembayaranMahasiswaDaoImpl extends GenericHibernateDao<BaypassPembayaranMahasiswa, Long, BaypassPembayaranMahasiswaDao> implements BaypassPembayaranMahasiswaDao{

}
