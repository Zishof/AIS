package ais.database.dao;

import ais.database.model.BankHost;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BankHost} (data host/koneksi
 * integrasi layanan bank), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BankHostDaoImpl extends GenericHibernateDao<BankHost, Long, BankHostDao> implements BankHostDao{

}
