package ais.action.master.pmb;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Tabpanel;

import ais.common.Common;

public class StatistikAction extends GenericAutowireComposer {

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
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onPendaftarSpmb(arg0);
			}
		});
	}

	private Tabpanel pendaftarSPMB;

	public void onPendaftarSpmb(Event event) {
		if (pendaftarSPMB.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/pmb/statistik/rekap_pendaftar_spmb.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			pendaftarSPMB.appendChild(halaman);
		}
	}

	private Tabpanel jenisPendidikan;

	public void onJenisPendidikan(Event event) {
		if (jenisPendidikan.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/pmb/statistik/rekap_pendaftar_spmb_per_jenis_sekolah.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			jenisPendidikan.appendChild(halaman);
		}
	}

	private Tabpanel propinsi;

	public void onPropinsi(Event event) {
		if (propinsi.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/pmb/statistik/rekap_pendaftar_spmb_propinsi.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			propinsi.appendChild(halaman);
		}
	}
	
	private Tabpanel perHari;

	public void onPerHari(Event event) {
		if (perHari.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/pmb/statistik/rekap_pendaftar_spmb_per_hari.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			perHari.appendChild(halaman);
		}
	}
	
}
