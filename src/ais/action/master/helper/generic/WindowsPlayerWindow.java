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

/**
 * Tipe khusus untuk windows player window. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String url}; operasi domain lain
 * ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class WindowsPlayerWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7041626862427552460L;

	private String url;

	public WindowsPlayerWindow(String url) {
		super();
		this.url = url;
		display();
	}

	public WindowsPlayerWindow(String url, String title, String border,
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
		setTitle("Windows Media Player");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);

		Html html = new ais.ui.util.MyHtml();
		center.appendChild(html);
		html.setHeight("100%");
		html.setWidth("100%");
		String str = "<object id=\"MediaPlayer\" classid=\"CLSID:22D6F312-B0F6-11D0-94AB-0080C74C7E95\" standby=\"Loading Windows Media Player components...\" type=\"application/x-oleobject\" height=\"45\" width=\"320\"> "
				+ "<param name=\"AutoStart\" value=\"true\"> "
				+ "<param name=\"AutoSize\" value=\"false\"> "
				+ "<param name=\"DisplaySize\" value=\"0\"> "
				+ "<param name=\"Enabled\" value=\"true\"> "
				+ "<param name=\"enableContextMenu\" value=\"false\"> "
				+ "<param name=\"ShowControls\" value=\"true\"> "
				+ "<param name=\"ShowAudioControls\" value=\"true\"> "
				+ "<param name=\"ShowPositionControls\" value=\"false\"> "
				+

				"<param name=\"ShowStatusBar\" value=\"false\"> "
				+ "<param name=\"Volume\" value=\"50\"> "
				+ "<param name=\"ShowCaptioning\" value=\"false\"> "
				+ "<param name=\"FileName\" value=\""
				+ url
				+ "\"> "
				+ "<embed type=\"application/x-mplayer2\" src=\""
				+ url
				+ "\" name=\"MediaPlayer\" showcontrols=\"1\" showpositioncontrols=\"0\" enablecontextmenu=\"0\" showstatusbar=\"0\" showdisplay=\"0\" autostart=\"1\" pluginspage=\"http://www.microsoft.com/Windows/Downloads/Contents/Products/MediaPlayer/\" height=\"100%\" width=\"100%\"> "
				+ "</object>";

		// System.out.println("str = " + str);

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
				WindowsPlayerWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

	}

}
