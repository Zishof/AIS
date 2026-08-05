package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Paging;
import org.zkoss.zul.event.PagingEvent;

/**
 * Paging kembar untuk daftar standar: komponen ber-id "paging" (atas) dan
 * "paging2" (bawah) saling menyinkronkan halaman aktif, ukuran halaman,
 * total data, dan visibilitas. Action cukup mengenal "paging" seperti
 * biasa; klik pada "paging2" diteruskan sebagai onPaging ke "paging"
 * sehingga listener action tetap berjalan tanpa perubahan apa pun.
 */
public class MyPagingKembar extends Paging {

	private static final long serialVersionUID = 1L;

	/** penjaga agar sinkronisasi dua arah tidak saling memantul */
	private boolean sedangSinkron = false;

	public MyPagingKembar() {
		addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyPagingKembar pasangan = cariPasangan();
				if (pasangan == null) {
					return;
				}
				pasangan.salinDari(MyPagingKembar.this);
				if ("paging2".equals(getId())) {
					/* klik di paging bawah: teruskan ke paging utama agar
					 * listener yang dipasang action (Common.initPaging)
					 * memuat ulang data halaman tersebut */
					Events.postEvent(new PagingEvent("onPaging", pasangan, pasangan.getActivePage()));
				}
			}
		});
	}

	private MyPagingKembar cariPasangan() {
		String id = getId();
		String idPasangan = "paging2".equals(id) ? "paging" : ("paging".equals(id) ? "paging2" : null);
		if (idPasangan == null) {
			return null;
		}
		Component c = getFellowIfAny(idPasangan);
		return c instanceof MyPagingKembar ? (MyPagingKembar) c : null;
	}

	private void salinDari(MyPagingKembar sumber) {
		if (sedangSinkron) {
			return;
		}
		sedangSinkron = true;
		try {
			if (getPageSize() != sumber.getPageSize()) {
				super.setPageSize(sumber.getPageSize());
			}
			if (getTotalSize() != sumber.getTotalSize()) {
				super.setTotalSize(sumber.getTotalSize());
			}
			if (getActivePage() != sumber.getActivePage()) {
				super.setActivePage(sumber.getActivePage());
			}
			if (isVisible() != sumber.isVisible()) {
				super.setVisible(sumber.isVisible());
			}
			if (isDetailed() != sumber.isDetailed()) {
				super.setDetailed(sumber.isDetailed());
			}
			/* initPaging hanya menata paging utama (setMold("os"),
			 * pageIncrement); tanpa ini paging bawah tampil dengan
			 * mold default yang gaya & kontrasnya berbeda */
			if (getMold() == null ? sumber.getMold() != null : !getMold().equals(sumber.getMold())) {
				super.setMold(sumber.getMold());
			}
			if (getPageIncrement() != sumber.getPageIncrement()) {
				super.setPageIncrement(sumber.getPageIncrement());
			}
		} finally {
			sedangSinkron = false;
		}
	}

	private void sinkronKePasangan() {
		if (sedangSinkron) {
			return;
		}
		MyPagingKembar pasangan = cariPasangan();
		if (pasangan != null) {
			pasangan.salinDari(this);
		}
	}

	/* Setter yang dipanggil kode action pada paging utama ikut menyeret
	 * pasangannya. salinDari memakai super.setX sehingga tidak memantul. */

	@Override
	public void setActivePage(int pg) throws org.zkoss.zk.ui.WrongValueException {
		super.setActivePage(pg);
		sinkronKePasangan();
	}

	@Override
	public void setTotalSize(int size) throws org.zkoss.zk.ui.WrongValueException {
		super.setTotalSize(size);
		sinkronKePasangan();
	}

	@Override
	public void setPageSize(int size) throws org.zkoss.zk.ui.WrongValueException {
		super.setPageSize(size);
		sinkronKePasangan();
	}

	@Override
	public boolean setVisible(boolean visible) {
		boolean lama = super.setVisible(visible);
		sinkronKePasangan();
		return lama;
	}

	@Override
	public void setDetailed(boolean detailed) {
		super.setDetailed(detailed);
		sinkronKePasangan();
	}

	@Override
	public void setMold(String mold) {
		super.setMold(mold);
		sinkronKePasangan();
	}

	@Override
	public void setPageIncrement(int pginc) throws org.zkoss.zk.ui.WrongValueException {
		super.setPageIncrement(pginc);
		sinkronKePasangan();
	}
}
