package ais.action.master.akunting.helper;

import org.zkoss.zk.ui.event.Event;

import ais.database.model.akunting.Akun;

public class AmbilDataAkunKreditBanbox extends AmbilDataAkunBanbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6288697863973153458L;

	public void onSearchDefault(Event event) {
		debetCredit = Akun.CREDIT;
		super.onSearchDefault(event);
	}

}
