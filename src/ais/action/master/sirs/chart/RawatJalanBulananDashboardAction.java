package ais.action.master.sirs.chart;

import java.util.Calendar;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;

import ais.action.master.sirs.chart.helper.RawatJalanDashboardBuilder;
import ais.common.Common;
import ais.ui.util.MyChart;

/**
 * Layar ringkasan <b>Kunjungan Pasien Rawat Jalan per Bulan</b> (sepanjang satu tahun terpilih).
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan berapa banyak pasien rawat jalan yang datang tiap bulan dalam setahun, sekaligus poli
 * mana yang paling banyak melayani. Pengguna cukup memilih tahun, lalu seluruh grafik (kartu angka,
 * garis tren bulanan, batang perbandingan poli, lingkaran porsi, dan jaring laba-laba) langsung
 * diperbarui. Berguna untuk melihat pola musiman kunjungan (bulan ramai vs sepi) dan merencanakan
 * kebutuhan tenaga serta stok sepanjang tahun.
 *
 * <h3>Peran class ini</h3>
 * <i>Controller tipis</i>: hanya menyiapkan pilihan tahun, memasang pendengar {@code onChange}, dan
 * memerintahkan penggambaran. SELURUH logika data &amp; grafik didelegasikan ke
 * {@link RawatJalanDashboardBuilder#renderBulanan(MyChart, Integer)} — berbagi kode yang sama persis
 * dengan layar mingguan agar tidak ada duplikasi dan pemeliharaan cukup di satu tempat (<i>reuse</i>).
 *
 * <h3>Cara kerja</h3>
 * Pada {@link #doAfterCompose(Component)}, combobox tahun diisi ({@link Common#generateTahun}) dan
 * diberi pendengar {@code onChange} yang memanggil {@link #render()}; lalu {@link #render()} dipanggil
 * sekali agar grafik langsung tampil. Grafik digambar sepenuhnya dengan HTML + CSS modern melalui
 * builder (tanpa JFreeChart), ringan dan responsif di ponsel maupun desktop.
 *
 * <h3>Manajemen session &amp; ketahanan</h3>
 * Tidak membuka session sendiri; pembacaan data terjadi di builder memakai {@code currentSession()}
 * (ditutup otomatis — tidak ditutup manual). Nilai tahun yang kosong digantikan tahun berjalan, dan
 * builder menangani kegagalan secara ramah sehingga error data tidak menjatuhkan halaman.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6. Komponen UI
 * di-<i>autowire</i> dari ZUL: satu {@link Combobox} (id {@code tahunPasienRawatJalanBulanan}) dan
 * satu {@link MyChart} (id {@code pasienRawatJalan}) sebagai wadah grafik.
 *
 * @author AIS
 */
public class RawatJalanBulananDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 8806806334907195811L;

	/** Wadah grafik (HTML/CSS) — diisi oleh {@link RawatJalanDashboardBuilder}. */
	private MyChart pasienRawatJalan;

	/** Pilihan tahun yang ditinjau. */
	private Combobox tahunPasienRawatJalanBulanan;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Common.generateTahun(tahunPasienRawatJalanBulanan);
		if (tahunPasienRawatJalanBulanan != null) {
			tahunPasienRawatJalanBulanan.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					render();
				}
			});
		}

		render();
	}

	/**
	 * Menggambar (atau memperbarui) seluruh grafik untuk tahun yang sedang dipilih. Tahun yang kosong
	 * digantikan tahun berjalan agar dasbor selalu punya konteks valid.
	 */
	public void render() {
		Integer th = (tahunPasienRawatJalanBulanan == null
				|| tahunPasienRawatJalanBulanan.getSelectedItem() == null)
						? Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR))
						: (Integer) tahunPasienRawatJalanBulanan.getSelectedItem().getValue();

		RawatJalanDashboardBuilder.renderBulanan(pasienRawatJalan, th);
	}
}
