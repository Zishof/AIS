package ais.action.master.helper.generic;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;

public class QuickTimePlayerWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7041626862427552460L;

	private String url;

	public QuickTimePlayerWindow(String url) {
		super();
		this.url = url;
		display();
	}

	public QuickTimePlayerWindow(String url, String title, String border,
			boolean closable) {
		super(title, border, closable);
		this.url = url;
		display();
	}

	private void display() {
		
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
		.appendChild(this);
		
		setSizable(true);
		Common.clear(this);
		setPosition("center");
		setHeight("530px");
		setWidth("550px");
		setTitle("Quicktime Player");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);

		Html html = new ais.ui.util.MyHtml();
		center.appendChild(html);
		html.setHeight("100%");
		html.setWidth("100%");

		String str = " <OBJECT CLASSID=\"clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B\"   "
				+ " WIDTH=\"100%\"HEIGHT=\"100%\"    "
				+ " CODEBASE=\"http://www.apple.com/qtactivex/qtplugin.cab\">    "
				+ " <PARAM name=\"SRC\" VALUE=\""
				+ url
				+ "\">    "
				+ " <PARAM name=\"AUTOPLAY\" VALUE=\"true\">    "
				+ " <param NAME=\"type\" VALUE=\"video/quicktime\">   "
				+ " <PARAM name=\"CONTROLLER\" VALUE=\"true\">    "
				+ " <EMBED SRC=\""
				+ url
				+ "\" WIDTH=\"100%\" HEIGHT=\"100%\"    "
				+ " AUTOPLAY=\"false\" CONTROLLER=\"true\" type=\"video/quicktime\"PLUGINSPAGE=\"http://www.apple.com/quicktime/download/\">   "
				+ " </EMBED>    " + " </OBJECT> ";
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
				QuickTimePlayerWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

	}

}
