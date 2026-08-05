package ais.action.master.pmb;

import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.ui.util.MyWindow;

public class CetakNoRegistrasiSPMBMandiri extends GenericAutowireComposer{

	/**
	 * 
	 */
	private static final long serialVersionUID = -635226110737859882L;
	
	private Textbox noRegistrasi;
	public void display(MyWindow window) throws InterruptedException{
		window.setHeight("30%");
		window.setWidth("60%");
		
		Borderlayout borderlayout =new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		
		Center center = new Center();
		center.setWidth("100%");
		center.setParent(borderlayout);
		
		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(center);
		
		Rows rows =  new Rows();
		rows.setParent(grid);
		
		
		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Registrasi"));
		row.appendChild(noRegistrasi = new Textbox());
		noRegistrasi.setWidth("90%");
		
		
		window.onModal();
		
	}

}
