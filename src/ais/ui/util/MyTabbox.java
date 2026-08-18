package ais.ui.util;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;

public class MyTabbox extends Tabbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9054954341532370611L;

	public MyTabbox() {
		super();
		init();
	}

	private void init() {

		if (Common.isMobile()) {

			Common.createDefaultTimerNoBusy(new EventListener() {

				@SuppressWarnings({ "unchecked" })
				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Tab> tabs = getTabs().getChildren();
					Vbox vbox = new Vbox();
					Hbox hbox;
					for (int i = 0; i < tabs.size(); i++) {
						if (i == 0 || i % 3 == 0) {
							hbox = new Hbox();
							vbox.appendChild(hbox);
						}
						
					}

				}
			});

		}

	}
}
