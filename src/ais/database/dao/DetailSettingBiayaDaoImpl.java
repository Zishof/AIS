package ais.database.dao;

import ais.database.model.DetailSettingBiaya;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.DetailSettingBiaya} (detail
 * setting/aturan biaya), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DetailSettingBiayaDaoImpl extends GenericHibernateDao<DetailSettingBiaya, Long, DetailSettingBiayaDao> implements DetailSettingBiayaDao{

}
