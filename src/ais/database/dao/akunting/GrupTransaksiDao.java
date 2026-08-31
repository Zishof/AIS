package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.GrupTransaksi;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.GrupTransaksi} — grup/kelompok jenis
 * transaksi akunting (modul akunting). Interface ini sengaja kosong: seluruh kontrak CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku
 * method.
 */
public interface GrupTransaksiDao extends GenericDao<GrupTransaksi, Long>{

}
