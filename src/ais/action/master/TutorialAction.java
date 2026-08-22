package ais.action.master;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.ui.util.MyToolbarbuttonConfig;

public class TutorialAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tbmuser users;
	private Textbox searchnama;

	private MyGrid grid;

	private String lokasi_file_video_tutorial;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		users = Common.getCurrentFromSpringUser();
		lokasi_file_video_tutorial = Common.getKonfigurasi("lokasi_directory_video_tutorial", "/opt/videos").getNilai();

		onSearchDefault(null);

	}

	class TutorialRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final File video = new File(arg1.toString());

			// HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl
			// .getCurrent().getNativeRequest();
			// final String url = "http"
			// + (Common.isSecure(request) ? "s" : "")
			// + "://"
			// + request.getServerName()
			// + ":"
			// + request.getServerPort()
			// + request.getContextPath()
			// + "/AmbilFileServer?file="
			// + URLEncoder.encode(lokasi_file_video_tutorial + "/"
			// + video.getName(), "UTF-8");

			Label a = new Label();
			a.setValue(video.getName());
			// a.setHref(url);
			// a.setTarget("_blank");
			a.setParent(arg0);

			Hbox toolbar = new Hbox();
			// MyToolbarbuttonConfig toolbarbutton = new
			// MyToolbarbuttonConfig("",
			// "/img/flash.png");
			//
			// toolbarbutton.setOrient("vertical");
			// toolbarbutton.setParent(toolbar);
			// toolbarbutton.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			//
			// FlowPlayerWindow flowPlayerWindow = new FlowPlayerWindow(
			// url);
			// flowPlayerWindow.setHeight("470px");
			// flowPlayerWindow.setWidth("850px");
			// flowPlayerWindow.onModal();
			//
			// }
			//
			// });
			//
			// toolbarbutton = new MyToolbarbuttonConfig("", "/img/quick.png");
			// toolbarbutton.setOrient("vertical");
			// toolbarbutton.setParent(toolbar);
			// toolbarbutton.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			//
			// HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl
			// .getCurrent().getNativeRequest();
			//
			// QuickTimePlayerWindow flowPlayerWindow = new
			// QuickTimePlayerWindow(
			// url);
			// flowPlayerWindow.setHeight("470px");
			// flowPlayerWindow.setWidth("850px");
			// flowPlayerWindow.onModal();
			//
			// }
			//
			// });
			//
			// toolbarbutton = new MyToolbarbuttonConfig("",
			// "/img/media_player.png");
			// toolbarbutton.setOrient("vertical");
			// toolbarbutton.setParent(toolbar);
			// toolbarbutton.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			//
			// WindowsPlayerWindow flowPlayerWindow = new WindowsPlayerWindow(
			// url);
			// flowPlayerWindow.setHeight("470px");
			// flowPlayerWindow.setWidth("850px");
			// flowPlayerWindow.onModal();
			//
			// }
			//
			// });

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", FileFoto.icon(video.getName()));

			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Filedownload.save(video, "video/avi");

				}

			});

			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onSearchDefault(Event event) {

		File[] videos = new File(lokasi_file_video_tutorial).listFiles();
		List<String> files = new ArrayList<String>();
		if (videos != null) {

			for (File v : videos) {
				if (v.getName().toLowerCase().contains(searchnama.getValue().trim().toLowerCase())) {

					if (users.hakAkses() != null && v.getName().toLowerCase().trim()
							.contains("-" + users.hakAkses().getRoleId().trim().toLowerCase() + "-")) {
						files.add(v.getAbsolutePath());
					}
				}
			}

			if (files.isEmpty()) {
				for (File v : videos) {
					if (v.getName().toLowerCase().contains(searchnama.getValue().trim().toLowerCase())) {
						files.add(v.getAbsolutePath());
					}
				}
			}

			Collections.sort(files);
		}

		ListModel strset = new SimpleListModel(files);
		grid.setRowRenderer(new TutorialRenderer());
		grid.setModelCheckMobile(strset);

	}

}
