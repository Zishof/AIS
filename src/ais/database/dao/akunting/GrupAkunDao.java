package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.GrupAkun;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.GrupAkun} — grup/kelompok pengelompokan
 * akun buku besar (modul akunting). Interface ini sengaja kosong: seluruh kontrak CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku
 * method.
 */
public interface GrupAkunDao extends GenericDao<GrupAkun, Long>{

}