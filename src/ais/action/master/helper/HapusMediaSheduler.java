package ais.action.master.helper;

import java.util.TimerTask;

public class HapusMediaSheduler extends TimerTask {

	public static void check() {
//		if (Common.REAL_PATH != null && !Common.REAL_PATH.trim().isEmpty()) {
//			try {
//				File folder = CommonMedia.getMediaDirectory();
//				System.out.println(
//						"sebelum hapus directory " + folder.getAbsolutePath() + ", exist -> " + folder.exists());
//				FileUtils.deleteDirectory(folder);
//				System.out.println(
//						"setelah hapus directory " + folder.getAbsolutePath() + ", exist -> " + folder.exists());
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HapusMediaSheduler.java:16");
//				e.printStackTrace();
//			}
//
//		}
	}

	@Override
	public void run() {
		check();
	}

}
