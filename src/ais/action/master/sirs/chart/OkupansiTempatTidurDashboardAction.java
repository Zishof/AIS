package ais.action.master.sirs.chart;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sirs.chart.helper.OkupansiTempatTidurDashboardBuilder;
import ais.ui.util.MyChart;

/**
 * Layar <b>Dasbor Okupansi Tempat Tidur (Rawat Inap)</b> — kondisi terkini tempat tidur.
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan berapa tempat tidur yang sedang dipakai dan yang masih kosong saat ini, beserta
 * sebarannya per kelas perawatan dan perkiraan tingkat keterisian. Berguna bagi petugas pendaftaran
 * &amp; manajemen untuk cepat tahu apakah masih ada tempat untuk pasien baru dan di kelas mana.
 *
 * <h3>Peran class ini</h3>
 * <i>Controller tipis</i>: menggambar dasbor sekali saat halaman dibuka dan menyediakan tombol
 * <b>Segarkan</b> (opsional, bila disediakan di ZUL) untuk memuat ulang kondisi terkini. Seluruh
 * logika data &amp; grafik didelegasikan ke {@link OkupansiTempatTidurDashboardBuilder#render(MyChart)}
 * (HTML/CSS via HtmlChartHelper, tanpa JFreeChart). Karena dasbor bersifat SNAPSHOT (kondisi saat ini),
 * tidak ada pilihan tahun/bulan.
 *
 * <h3>Manajemen session &amp; kompatibilitas</h3>
 * Tidak membuka session sendiri; pembacaan di builder memakai {@code currentSession()} (ditutup
 * otomatis, tidak ditutup manual). Java 1.7 / ZK 5.5, {@code try/catch} gaya Java 1.6. Autowire:
 * {@link MyChart} id {@code chartOkupansi}, dan (opsional) {@link Toolbarbutton} id {@code refresh}.
 *
 * @author AIS
 */
public class OkupansiTempatTidurDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 3344556677889900112L;

	private MyChart chartOkupansi;
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

	/** Menggambar/memperbarui dasbor sesuai kondisi tempat tidur terkini. */
	public void render() {
		OkupansiTempatTidurDashboardBuilder.render(chartOkupansi);
	}
}
