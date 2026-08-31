package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;

/**
 * DAO untuk entitas {@link ais.database.model.surat.KlasifikasiSuratKeluarUntuk} — relasi
 * klasifikasi surat keluar dengan tujuan/pihak penerimanya (modul tata naskah dinas/persuratan).
 * Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface KlasifikasiSuratKeluarUntukDao extends
		GenericDao<KlasifikasiSuratKeluarUntuk, Long> {

}
