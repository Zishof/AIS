package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.KelompokLaporan;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.KelompokLaporan} — kelompok baris pada
 * struktur laporan akunting (modul akunting). Interface ini sengaja kosong: seluruh kontrak CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail
 * perilaku method.
 */
public interface KelompokLaporanDao extends GenericDao<KelompokLaporan, Long> {

}
