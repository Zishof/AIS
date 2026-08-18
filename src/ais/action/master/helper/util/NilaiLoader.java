package ais.action.master.helper.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiTambahan;

public class NilaiLoader {

	public static void startLoad(final Detailperkuliahan detailperkuliahan, final FormatNilai formatNilai,
			final Component component) {
		startLoad(detailperkuliahan, formatNilai, null, component);
	}

	public static void startLoad(final Detailperkuliahan detailperkuliahan,
			final FormatNilaiTambahan formatNilaiTambahan, final Component component) {
		startLoad(detailperkuliahan, null, formatNilaiTambahan, component);
	}

	public static void startLoad(final Detailperkuliahan detailperkuliahan, final FormatNilai formatNilai,
			final FormatNilaiTambahan formatNilaiTambahan, final Component component) {
		if (component instanceof Doublebox) {
			((Doublebox) component).setReadonly(true);
			((Doublebox) component).setVisible(false);
		}

		Double nilai = 0.0;

		try {
			if (formatNilai != null) {
				nilai = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
			} else if (formatNilaiTambahan != null) {
				nilai = detailperkuliahan.retreiveDetailNilaiTambahan(formatNilaiTambahan);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		Double n = nilai == null ? 0.0 : nilai.doubleValue();

		if (component instanceof Doublebox) {
			((Doublebox) component).setVisible(true);
			((Doublebox) component).setReadonly(false);
			((Doublebox) component).setValue(n);
		} else if (component instanceof Label) {
			((Label) component).setValue(Common.numberFormat.get().format(n));
		}

	}

}
