package ais.action.report.format1.akunting;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;

public class LaporanAkuntingSaldoBulan_1 extends LaporanAkuntingSaldoBulanMaster {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7998751630422451915L;

	public LaporanAkuntingSaldoBulan_1() {
		super();
		super.jenisLaporan.setSelectedIndex(0);
		super.jenisLaporan.setDisabled(true);
		try {
			super.eventListener2.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akunting/LaporanAkuntingSaldoBulan_1.java:23");
		}
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(null);
			}
		});
	}

	public LaporanAkuntingSaldoBulan_1(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		super.jenisLaporan.setSelectedIndex(0);
		super.jenisLaporan.setDisabled(true);
		try {
			super.eventListener2.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akunting/LaporanAkuntingSaldoBulan_1.java:42");
		}
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(null);
			}
		});
	}
}
