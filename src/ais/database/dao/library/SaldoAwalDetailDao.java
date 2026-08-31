package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.SaldoAwalDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.SaldoAwalDetail} pada modul
 * perpustakaan — baris detail saldo awal item pustaka (rincian per item dari
 * {@link ais.database.model.library.SaldoAwal}). Interface ini sengaja kosong: seluruh operasi
 * CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc di sana
 * untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface SaldoAwalDetailDao extends GenericDao<SaldoAwalDetail, Long>{

}
