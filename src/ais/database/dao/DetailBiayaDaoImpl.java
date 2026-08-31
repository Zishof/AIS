package ais.database.dao;

import ais.database.model.DetailBiaya;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.DetailBiaya} (detail
 * komponen biaya), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DetailBiayaDaoImpl extends GenericHibernateDao<DetailBiaya, Long, DetailBiayaDao> implements DetailBiayaDao{

}
