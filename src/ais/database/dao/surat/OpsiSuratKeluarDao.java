package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.OpsiSuratKeluar;

/**
 * DAO untuk entitas {@link ais.database.model.surat.OpsiSuratKeluar} — opsi/pengaturan tambahan
 * yang dapat dipilih pada surat keluar (modul tata naskah dinas/persuratan). Interface ini sengaja
 * kosong: seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat
 * Javadoc di sana untuk detail perilaku method.
 */
public interface OpsiSuratKeluarDao extends GenericDao<OpsiSuratKeluar, Long> {

}
