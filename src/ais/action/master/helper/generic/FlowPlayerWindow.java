package ais.action.master.helper.generic;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FlowPlayerWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7041626862427552460L;

	private String url;

	public FlowPlayerWindow(String url) {
		super();
		this.url = url;
		display();
	}

	public FlowPlayerWindow(String url, String title, String border, boolean closable) {
		super(title, border, closable);
		this.url = url;
		display();
	}

	private void display() {

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(this);

		setSizable(true);
		Common.clear(this);
		setPosition("center");
		setHeight("530px");
		setWidth("550px");
		setTitle("Shockwave Player");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);

		Html html = new ais.ui.util.MyHtml();
		center.appendChild(html);
		html.setHeight("100%");
		html.setWidth("100%");

		// String str = " <script type=\"text/javascript\" src=\""
		// + Executions.getCurrent().getContextPath()
		// +
		// "/component/flowplayer/example/flowplayer-3.2.6.min.js\"></script>\n";
		// str += "<a " + " href=\"" + url + "\" "
		// + " style=\"display:block;width:100%;height:100%\" "
		// + " id=\"player\"> " + "</a>";
		//
		// str += "<script> " + " flowplayer(\"player\", \""
		// + Executions.getCurrent().getContextPath()
		// + "/component/flowplayer/flowplayer-3.2.7.swf\"); "
		// + "</script>";

		String str = "<video style=\"display:block;width:100%;height:100%\" controls autoplay>" + "<source src=\"" + url
				+ "#t=00:00:03\" type=\"video/mp4\">" + "Your browser does not support the video tag." + "</video>";

		System.out.println("str = " + str);

		html.setContent(str);

		South south = new South();
		south.setParent(borderlayout);

		Toolbar hbox = new Toolbar();
		hbox.setHeight("30px");
		hbox.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				FlowPlayerWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

	}

}
