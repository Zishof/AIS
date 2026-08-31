package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.MasterGrupLaporan;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.MasterGrupLaporan} — master/induk grup
 * laporan yang menaungi {@link ais.database.model.akunting.KelompokLaporan} (modul akunting).
 * Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface MasterGrupLaporanDao extends
		GenericDao<MasterGrupLaporan, Long> {

}
