package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Textbox;

import ais.common.Common;

public class MyTextboxAngka extends Textbox {

	private EventListener eventListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setValue(MyTextboxAngka.this.getValue());
				}
			}, "", false, 500);
		}
	};

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyTextboxAngka() {
		super();
		setWidth("90%");
		addEventListener("onChange", eventListener);
	}

	public MyTextboxAngka(String value) throws WrongValueException {
		super(value);
		setWidth("90%");
		addEventListener("onChange", eventListener);
	}

	public String getValue() {
		return super.getValue().replaceAll("\\D+", "");
	}

}
