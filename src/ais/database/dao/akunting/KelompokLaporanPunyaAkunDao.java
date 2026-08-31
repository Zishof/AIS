package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.KelompokLaporanPunyaAkun} — relasi
 * keanggotaan akun buku besar pada suatu {@link ais.database.model.akunting.KelompokLaporan}
 * (modul akunting). Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface KelompokLaporanPunyaAkunDao extends
		GenericDao<KelompokLaporanPunyaAkun, Long> {

}
