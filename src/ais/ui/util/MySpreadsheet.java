package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zss.ui.Spreadsheet;

public class MySpreadsheet extends Spreadsheet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3723253326511941267L;

	public MySpreadsheet() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void setParent(Component component) {
		super.setParent(component);
//		try {
//			((HtmlBasedComponent) component.getParent()).setStyle("max-height: 700px;min-height: 50px;");
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:23");
//			// TODO: handle exception
//		}
	}

	public void setMaxrows(int rows) {
		try {
			// FIX defensif: org.zkoss.poi.xssf.usermodel.helpers.ColumnHelper /
			// CTWorksheetImpl.getColsArray() (pustaka pihak ketiga ZK Spreadsheet + POI) bisa
			// melempar IndexOutOfBoundsException ketika workbook template (mis. rowcolumn.xlsx)
			// belum punya definisi kolom/baris yang cukup untuk jumlah baris yang diminta.
			// Tidak ada hook resmi untuk override listener resize internal Spreadsheet
			// (Spreadsheet$InnerDataListener) dari kode aplikasi, jadi minimal cegah exception
			// dari panggilan setMaxrows sendiri agar tidak menjatuhkan seluruh halaman.
			super.setMaxrows(rows);
		} catch (IndexOutOfBoundsException e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(zss-poi-limitation) src/ais/ui/util/MySpreadsheet.java:setMaxrows");
			return;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:setMaxrows");
			return;
		}

		try {

			if (getParent() != null) {
				int tinggi = (rows * 17) + 2500;
				String h = tinggi > 2000 ? "2000px" : (tinggi + "px");
				((HtmlBasedComponent) getParent().getParent()).setStyle("max-height: " + h + ";min-height: 50px;");
				this.setStyle("max-height: " + h + ";min-height: 50px;");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MySpreadsheet.java:39");
			// TODO: handle exception
		}

	}
}
