package ais.action.master.sirs.chart;

import java.util.Calendar;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;

import ais.action.master.sirs.chart.helper.DiagnosaTerbanyakDashboardBuilder;
import ais.common.Common;
import ais.ui.util.MyChart;

/**
 * Layar <b>Dasbor 10 Diagnosa Penyakit Terbanyak</b> untuk satu tahun terpilih.
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan penyakit apa saja yang paling sering ditangani rumah sakit/klinik dalam setahun,
 * diurut dari yang terbanyak. Pengguna cukup memilih tahun; grafik langsung diperbarui. Berguna bagi
 * manajemen untuk merencanakan stok obat, alat, dan tenaga sesuai pola penyakit yang benar-benar
 * banyak muncul.
 *
 * <h3>Peran class ini</h3>
 * <i>Controller tipis</i> yang mendelegasikan seluruh logika data &amp; grafik ke
 * {@link DiagnosaTerbanyakDashboardBuilder#render(MyChart, Integer)} (HTML/CSS via HtmlChartHelper,
 * tanpa JFreeChart) demi <i>reuse</i>. Tidak membuka session sendiri; pembacaan di builder memakai
 * {@code currentSession()} (ditutup otomatis, tidak ditutup manual). Java 1.7 / ZK 5.5, {@code try/catch}
 * gaya Java 1.6. Autowire: {@link Combobox} id {@code tahun} + {@link MyChart} id {@code chartDiagnosa}.
 *
 * @author AIS
 */
public class DiagnosaTerbanyakDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 2233445566778899001L;

	private MyChart chartDiagnosa;
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
		DiagnosaTerbanyakDashboardBuilder.render(chartDiagnosa, th);
	}
}
