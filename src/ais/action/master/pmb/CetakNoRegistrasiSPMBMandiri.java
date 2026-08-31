package ais.action.master.pmb;

import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk cetak no registrasi spmb mandiri. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code noRegistrasi}; operasi lokal: {@code
 * display}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
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
