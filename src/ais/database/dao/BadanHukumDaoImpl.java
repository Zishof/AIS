package ais.database.dao;

import ais.database.model.BadanHukum;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BadanHukum} (data badan
 * hukum/yayasan penyelenggara), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BadanHukumDaoImpl extends GenericHibernateDao<BadanHukum, Long, BadanHukumDao> implements BadanHukumDao{

}
