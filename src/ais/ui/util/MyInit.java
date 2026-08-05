package ais.ui.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Initiator;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.RequestContext;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

public class MyInit implements Initiator {

	public void doAfterCompose(Page arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public boolean doCatch(Throwable arg0) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	public void doFinally() throws Exception {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("rawtypes")
	@Override
	public void doInit(Page arg0, Map arg1) throws Exception {
		// TODO Auto-generated method stub
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
			Yayasan yayasan = SekolahUtil.getYayasan(request);
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			boolean[] ptYa = Common.chekPtAtauSekolah();
			boolean ya = ptYa[1];
			String judul = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
			String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_", perguruanTinggi);
			if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty()) {
				logo_PerguruanTinggi = "/img/logo.png";
			}
			if (ya && (sekolah != null && sekolah.getId() != null)) {
				judul = Common.getKonfigurasi("judul_header_sekolah", "eSchool").getNilai();
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + sekolah.getNama());

				String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
						"logo_sekolah_", sekolah);
				if (logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")) {
					logo_PerguruanTinggi = logo_PerguruanTinggi_local;
				}

			} else if (ya && (yayasan != null && yayasan.getId() != null)) {
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition().setTitle(yayasan.getNama());

				String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(request,
						"logo_yayasan_", yayasan);
				if (logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")) {
					logo_PerguruanTinggi = logo_PerguruanTinggi_local;
				}

			} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + perguruanTinggi.getNama());
			}

			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().setAttribute("myFavicon", logo_PerguruanTinggi);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyInit.java:88");
		}

	}
}
