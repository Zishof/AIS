package ais.database.dao;

import ais.database.model.AngkatanKurikulum;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.AngkatanKurikulum} (data
 * angkatan/tahun berlaku suatu kurikulum), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class AngkatanKurikulumDaoImpl extends GenericHibernateDao<AngkatanKurikulum, Long, AngkatanKurikulumDao> implements AngkatanKurikulumDao{

}
