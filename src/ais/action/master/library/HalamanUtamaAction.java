package ais.action.master.library;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.utama.DashboardPustaka;
import ais.common.Common;

/**
 * Controller/action ZK untuk halaman utama. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class HalamanUtamaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.setUserAccess((HttpServletRequest) execution.getNativeRequest());
		Common.initLaguage();
		if (session != null) { session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID")); }
		session.removeAttribute("usersTemp");

		String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
		String image = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia("logo_perguruanTinggi_");

		Borderlayout borderlayout = new Borderlayout();
		if (borderlayout != null) { borderlayout.setParent(page.getFirstRoot()); }

		

		if (Common.isMobile()) {
			North north = new North();
			north.setBorder("none");
			borderlayout.appendChild(north);
			north.setHeight("150px");
			north.setSclass("headerHbox");

			Grid grid = new Grid();grid.setSclass("dgrid");
			grid.setHeight("100%");
			grid.setSclass("fgrid");
			grid.setStyle("border:0px;background: transparent;");
			grid.setParent(north);

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setWidth("100%");
			column.setAlign("center");
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			Row row = new Row();row.setValign("top");
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Image imgLogo;
			row.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			imgLogo.setHeight("58px");

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			row.appendChild(namaSeleksi);

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_pustaka_kampus", "Sistem Informasi Perpustakaan").getNilai());
			row.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1pmb");
			namaSekolah.setSclass("mottopmb");

		} else {

			borderlayout.setStyle("border-radius:20px;");

			North north = new North();
			north.setHeight("60px");
			north.setBorder("none");
			north.setSclass("headerHbox");
			borderlayout.appendChild(north);

			Hbox hbox = new Hbox();
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			Image imgLogo;
			hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			hbox.appendChild(new Space());
			hbox.setWidth("100%");
			hbox.setHeight("90px");
			north.appendChild(hbox);

			Vbox vbox = new Vbox();

			vbox.setWidth("100%");
			vbox.setPack("center");
			hbox.appendChild(vbox);

			imgLogo.setHeight("50px");

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_pustaka_kampus", "Sistem Informasi Perpustakaan").getNilai());
			vbox.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1");
			namaSekolah.setSclass("motto");

		}

		Center center = new Center();
		if (center != null) { center.setBorder("none"); }
		if (center != null) { center.setParent(borderlayout); }

		DashboardPustaka a = new DashboardPustaka();
		ais.ui.util.BaseDasbordPortal.mountWrapped(a, center,
			"Dasbor Perpustakaan", "Gambaran koleksi, peminjaman aktif, dan aktivitas perpustakaan hari ini.");
	}

}
