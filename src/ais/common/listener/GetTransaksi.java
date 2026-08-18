package ais.common.listener;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Button;

import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TransaksiMedis;

public interface GetTransaksi {
	public TransaksiMedis getTransaksi();

	public Lokasi getLokasi();

	public KelasPerawatan getKelasPerawatan();

	public boolean onSave(Event event) throws Exception;

	public String getSumber();

	public Button getAdd();

	public Button getSimpan();

	public Bandbox getResep();
}
