package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.JenisLaporan;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.JenisLaporan} — jenis laporan akunting yang
 * tersedia (mis. neraca, laba-rugi). Interface ini sengaja kosong: seluruh kontrak CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku
 * method.
 */
public interface JenisLaporanDao extends GenericDao<JenisLaporan, Long> {

}
