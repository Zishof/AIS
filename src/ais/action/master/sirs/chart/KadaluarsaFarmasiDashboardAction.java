package ais.action.master.sirs.chart;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sirs.chart.helper.KadaluarsaFarmasiDashboardBuilder;
import ais.ui.util.MyChart;

/**
 * Layar <b>Dasbor Kewaspadaan Kadaluarsa Obat/Barang Medis (Farmasi)</b> — kondisi terkini.
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan barang yang sudah kadaluarsa dan yang akan segera kadaluarsa, dikelompokkan menurut
 * sisa waktu, ditambah daftar barang paling mendesak. Membantu petugas farmasi/gudang menarik,
 * menukar, atau memakai lebih dulu barang yang mendekati batas waktu sebelum menjadi kerugian.
 *
 * <h3>Peran class ini</h3>
 * <i>Controller tipis</i>: menggambar dasbor sekali saat halaman dibuka dan menyediakan tombol
 * <b>Segarkan</b> (opsional, bila disediakan di ZUL) untuk memuat ulang kondisi terkini. Seluruh
 * logika data &amp; grafik didelegasikan ke {@link KadaluarsaFarmasiDashboardBuilder#render(MyChart)}
 * (HTML/CSS via HtmlChartHelper, tanpa JFreeChart). Karena dasbor bersifat SNAPSHOT (relatif terhadap
 * tanggal hari ini), tidak ada pilihan tahun/bulan.
 *
 * <h3>Manajemen session &amp; kompatibilitas</h3>
 * Tidak membuka session sendiri; pembacaan di builder memakai {@code currentSession()} (ditutup
 * otomatis, tidak ditutup manual). Java 1.7 / ZK 5.5, {@code try/catch} gaya Java 1.6. Autowire:
 * {@link MyChart} id {@code chartKadaluarsa}, dan (opsional) {@link Toolbarbutton} id {@code refresh}.
 *
 * @author AIS
 */
public class KadaluarsaFarmasiDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 5566778899001122334L;

	private MyChart chartKadaluarsa;
	private Toolbarbutton refresh;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (refresh != null) {
			refresh.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					render();
				}
			});
		}
		render();
	}

	/** Menggambar/memperbarui dasbor sesuai kondisi kadaluarsa terkini. */
	public void render() {
		KadaluarsaFarmasiDashboardBuilder.render(chartKadaluarsa);
	}
}
