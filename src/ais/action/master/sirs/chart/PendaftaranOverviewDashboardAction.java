package ais.action.master.sirs.chart;

import java.util.Calendar;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;

import ais.action.master.sirs.chart.helper.PendaftaranOverviewDashboardBuilder;
import ais.common.Common;
import ais.ui.util.MyChart;

/**
 * Layar <b>Ringkasan Pendaftaran Pasien</b> — memberi pandangan menyeluruh atas seluruh jenis
 * kunjungan (Rawat Jalan, Rawat Inap, UGD, dsb.) sepanjang satu tahun terpilih.
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan berapa banyak pasien yang mendaftar dalam setahun, lewat jalur layanan mana saja, dan
 * bulan mana yang paling ramai. Pengguna cukup memilih tahun; seluruh grafik (kartu angka, garis tren
 * per jalur, lingkaran porsi, dan batang total per bulan) langsung diperbarui. Cocok untuk manajemen
 * memantau beban tiap jalur layanan dan pola musiman kunjungan.
 *
 * <h3>Peran class ini</h3>
 * <i>Controller tipis</i>: menyiapkan pilihan tahun, memasang pendengar {@code onChange}, dan
 * memerintahkan penggambaran. Seluruh logika data &amp; grafik didelegasikan ke
 * {@link PendaftaranOverviewDashboardBuilder#render(MyChart, Integer)} (HTML/CSS via HtmlChartHelper,
 * tanpa JFreeChart) demi <i>reuse</i> dan kemudahan pemeliharaan.
 *
 * <h3>Manajemen session &amp; ketahanan</h3>
 * Tidak membuka session sendiri; pembacaan terjadi di builder memakai {@code currentSession()}
 * (ditutup otomatis, tidak ditutup manual). Tahun kosong digantikan tahun berjalan; kegagalan
 * ditangani ramah oleh builder sehingga tak menjatuhkan halaman.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6. Komponen UI
 * di-<i>autowire</i>: satu {@link Combobox} (id {@code tahun}) dan satu {@link MyChart} (id
 * {@code chartPendaftaran}) sebagai wadah grafik.
 *
 * @author AIS
 */
public class PendaftaranOverviewDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 5566778899001122334L;

	/** Wadah grafik (HTML/CSS) — diisi oleh {@link PendaftaranOverviewDashboardBuilder}. */
	private MyChart chartPendaftaran;

	/** Pilihan tahun yang ditinjau. */
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

	/**
	 * Menggambar (atau memperbarui) seluruh grafik untuk tahun yang sedang dipilih. Tahun kosong
	 * digantikan tahun berjalan agar dasbor selalu punya konteks valid.
	 */
	public void render() {
		Integer th = (tahun == null || tahun.getSelectedItem() == null)
				? Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR))
				: (Integer) tahun.getSelectedItem().getValue();

		PendaftaranOverviewDashboardBuilder.render(chartPendaftaran, th);
	}
}
