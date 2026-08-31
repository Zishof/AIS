package ais.database.dao;


import ais.database.model.KurikulumPunyaMatakuliah;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.KurikulumPunyaMatakuliah}
 * (relasi kurikulum dengan mata kuliah yang menjadi anggotanya), lewat
 * {@link ais.database.dao.GenericHibernateDao} -- implementasi interface
 * {@link ais.database.dao.KurikulumPunyaMatakuliahDao} (perhatikan perbedaan kapitalisasi nama
 * berkas ini, {@code KurikulumPunyaMataKuliahDaoImpl}, dengan interface yang diimplementasikannya).
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class KurikulumPunyaMataKuliahDaoImpl extends GenericHibernateDao<KurikulumPunyaMatakuliah, Long, KurikulumPunyaMatakuliahDao> implements KurikulumPunyaMatakuliahDao{

}
