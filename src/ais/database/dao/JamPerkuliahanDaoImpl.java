package ais.database.dao;

import ais.database.model.JamPerkuliahan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JamPerkuliahan} (data
 * jam/slot waktu perkuliahan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JamPerkuliahanDaoImpl extends
		GenericHibernateDao<JamPerkuliahan, Long, JamPerkuliahanDao> implements
		JamPerkuliahanDao {

}
