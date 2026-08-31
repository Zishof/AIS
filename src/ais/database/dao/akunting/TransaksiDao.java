package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.Transaksi;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.Transaksi} — transaksi/jurnal akunting,
 * entitas inti modul akunting. Interface ini sengaja kosong: seluruh kontrak CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku
 * method.
 */
public interface TransaksiDao extends GenericDao<Transaksi, Long> {

}
