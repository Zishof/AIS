package ais.database.dao;


import ais.database.model.Bank;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Bank} (data referensi bank),
 * lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh
 * perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BankDaoImpl extends GenericHibernateDao<Bank, Long, BankDao> implements BankDao {
    


}
