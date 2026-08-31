package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.Devisi;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.Devisi} — divisi/unit organisasi yang
 * dipakai sebagai dimensi pencatatan transaksi (modul akunting). Interface ini sengaja kosong:
 * seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public interface DevisiDao extends GenericDao<Devisi, Long>{

}
