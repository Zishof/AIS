package ais.action.master.akunting.helper;

import org.zkoss.zk.ui.event.Event;

import ais.database.model.akunting.Akun;

public class AmbilDataAkunDebetBanbox extends AmbilDataAkunBanbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8602695522298262649L;

	public void onSearchDefault(Event event) {
		debetCredit = Akun.DEBET;
		super.onSearchDefault(event);
	}

}
