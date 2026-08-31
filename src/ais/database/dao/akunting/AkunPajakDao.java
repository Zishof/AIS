package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.AkunPajak;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.AkunPajak} — akun pajak yang dipetakan ke
 * transaksi/akun buku besar terkait (modul akunting). Interface ini sengaja kosong: seluruh
 * kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public interface AkunPajakDao extends GenericDao<AkunPajak, Long> {

}
