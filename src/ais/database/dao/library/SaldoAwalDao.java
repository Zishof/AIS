package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.SaldoAwal;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.SaldoAwal} pada modul perpustakaan
 * — transaksi (header) saldo awal stok/koleksi item pustaka. Interface ini sengaja kosong:
 * seluruh operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface SaldoAwalDao extends GenericDao<SaldoAwal, Long>{

}
