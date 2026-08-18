package ais.action.report.format1.akunting;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;

public class LaporanAkuntingSaldoBulan extends LaporanAkuntingSaldoBulanMaster {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7998751630422451915L;

	public LaporanAkuntingSaldoBulan() {
		super();
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(null);
			}
		});
	}

	public LaporanAkuntingSaldoBulan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(null);
			}
		});
	}
}
