package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.Akun;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.Akun} — akun buku besar (chart of accounts)
 * modul akunting. Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface AkunDao extends GenericDao<Akun, Long> {

}
