package ais.common.listener;

import java.util.Date;

import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;

public interface TransaksiListener {

	public void onBebas(Boolean checked) throws Exception;

	public void onBerubah(Boolean bebas, Pendaftaran pendaftaran, Pasien pasien, String nama, Date tanggalTransaksi,
			KelasPerawatan kelasPerawatan, String keterangan) throws Exception;

}
