package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.Matauang;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.Matauang} — mata uang yang dipakai pada
 * transaksi akunting multi-currency (modul akunting). Interface ini sengaja kosong: seluruh
 * kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public interface MataUangDao extends GenericDao<Matauang, Long> {

}
