package ais.database.dao;


import ais.database.model.BeasiswaPunyaItemBiayaTambahan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BeasiswaPunyaItemBiayaTambahan}
 * (relasi beasiswa dengan item biaya tambahan yang ditanggungnya), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BeasiswaPunyaItemBiayaTambahanDaoImpl extends GenericHibernateDao<BeasiswaPunyaItemBiayaTambahan, Long, BeasiswaPunyaItemBiayaTambahanDao> implements BeasiswaPunyaItemBiayaTambahanDao {
    


}
