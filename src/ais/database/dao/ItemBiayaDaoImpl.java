package ais.database.dao;

import ais.database.model.ItemBiaya;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.ItemBiaya} (item/jenis
 * biaya), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class ItemBiayaDaoImpl extends GenericHibernateDao<ItemBiaya, Long, ItemBiayaDao> implements ItemBiayaDao{

}
