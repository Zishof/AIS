package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;

public class KonfigurasiLogoAction extends KonfigurasiNewAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onTampil();

	}

	public void onTampil() {

		Rows rows = (createSpan("Logo"));

		rows.appendChild(createRowActiveWithDefault("Aktifkan Header", "aktifkan_header", "", Konfigurasi.TIDAK_AKTIF,
				createComboActive(true)));

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		
		Groupbox groupbox = new Groupbox();
		groupbox.setParent(row);
		groupbox.appendChild(new Caption("Logo"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, LampiranLain.LOGO, LampiranLain.LOGO_STR, "Logo", false, null);
		hbox.setParent(groupbox);
		rows.appendChild(createRowNilai("Judul Utama", "label_judul_utama", ""));
		rows.appendChild(createRowNilai("Sub Judul Utama", "label_sub_judul_utama", ""));
		rows.appendChild(createRowNilai("Tinggi Header", "tinggi_judul_utama", "70px"));

	}
}
