package ais.database.dao;

import ais.database.model.Agama;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Agama} (data referensi agama),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class AgamaDaoImpl extends GenericHibernateDao<Agama, Long, AgamaDao> implements AgamaDao{

}
