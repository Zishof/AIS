package ais.database.dao;

import ais.database.model.Kelas;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Kelas} (data kelas
 * perkuliahan), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KelasDaoImpl extends GenericHibernateDao<Kelas, Long, KelasDao> implements KelasDao{

}
