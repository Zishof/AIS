package ais.action.master.sirs.chart;

import java.util.Calendar;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;

import ais.action.master.sirs.chart.helper.PendapatanDashboardBuilder;
import ais.common.Common;
import ais.ui.util.MyChart;

/**
 * Layar <b>Dasbor Pendapatan (Kas Masuk) Pasien</b> untuk satu tahun terpilih.
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan berapa total uang yang diterima dari pasien tiap bulan, bulan mana yang paling besar,
 * serta perbandingan pembayaran tunai vs non-tunai. Pengguna cukup memilih tahun; grafik langsung
 * diperbarui. Cocok bagi manajemen/keuangan memantau arus kas dari pelayanan pasien.
 *
 * <h3>Peran class ini</h3>
 * <i>Controller tipis</i> yang mendelegasikan seluruh logika data &amp; grafik ke
 * {@link PendapatanDashboardBuilder#render(MyChart, Integer)} (HTML/CSS via HtmlChartHelper, tanpa
 * JFreeChart) demi <i>reuse</i>. Tidak membuka session sendiri; pembacaan di builder memakai
 * {@code currentSession()} (ditutup otomatis, tidak ditutup manual). Java 1.7 / ZK 5.5, {@code try/catch}
 * gaya Java 1.6. Autowire: {@link Combobox} id {@code tahun} + {@link MyChart} id {@code chartPendapatan}.
 *
 * @author AIS
 */
public class PendapatanDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 7788990011223344556L;

	private MyChart chartPendapatan;
	private Combobox tahun;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.generateTahun(tahun);
		if (tahun != null) {
			tahun.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					render();
				}
			});
		}
		render();
	}

	/** Menggambar/memperbarui grafik untuk tahun terpilih (kosong → tahun berjalan). */
	public void render() {
		Integer th = (tahun == null || tahun.getSelectedItem() == null)
				? Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR))
				: (Integer) tahun.getSelectedItem().getValue();
		PendapatanDashboardBuilder.render(chartPendapatan, th);
	}
}
