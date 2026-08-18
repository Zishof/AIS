package ais.action.master.helper;

import java.util.TimerTask;

public class PembersihDataScheduller extends TimerTask {

	public static void check() {

		try {
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembersihDataScheduller.java:11");
		}
	}

	@Override
	public void run() {
		check();
	}

}
