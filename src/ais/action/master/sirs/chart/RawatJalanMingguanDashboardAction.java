package ais.action.master.sirs.chart;

import java.util.Calendar;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import ais.action.master.sirs.chart.helper.RawatJalanDashboardBuilder;
import ais.common.Common;
import ais.ui.util.MyChart;

/**
 * Layar ringkasan <b>Kunjungan Pasien Rawat Jalan per Minggu</b> (dalam satu bulan terpilih).
 *
 * <h3>Untuk apa layar ini (bahasa awam)</h3>
 * Menampilkan berapa banyak pasien rawat jalan yang datang tiap minggu pada bulan yang dipilih,
 * lengkap dengan poli mana yang paling sibuk. Pengguna cukup memilih tahun dan bulan, lalu seluruh
 * grafik (kartu angka, garis tren, batang perbandingan poli, lingkaran porsi, dan jaring laba-laba)
 * langsung diperbarui. Berguna bagi manajemen untuk memantau naik-turunnya kunjungan mingguan dan
 * mengatur jadwal dokter/petugas sesuai keramaian.
 *
 * <h3>Peran class ini</h3>
 * Class ini adalah <i>controller tipis</i>: tugasnya hanya (1) menyiapkan pilihan tahun &amp; bulan,
 * (2) memasang pendengar {@code onChange} agar grafik ikut berubah saat pilihan diganti, dan (3)
 * memerintahkan penggambaran. SELURUH logika pengambilan data serta penggambaran grafik didelegasikan
 * ke {@link RawatJalanDashboardBuilder#renderMingguan(MyChart, Integer, Integer)} agar tidak ada
 * duplikasi dengan layar bulanan dan agar pemeliharaan cukup di satu tempat (prinsip <i>reuse</i>).
 *
 * <h3>Cara kerja</h3>
 * Saat {@link #doAfterCompose(Component)} dipanggil, combobox tahun diisi ({@link Common#generateTahun})
 * dan combobox bulan diisi 1..12 dengan bulan berjalan terpilih secara default; keduanya diberi
 * pendengar {@code onChange} yang memanggil {@link #render()}. Pada akhir compose, {@link #render()}
 * dipanggil sekali agar grafik langsung tampil tanpa perlu interaksi. Grafik digambar sepenuhnya
 * dengan HTML + CSS modern melalui builder (tanpa JFreeChart), sehingga ringan dan responsif di
 * ponsel maupun desktop.
 *
 * <h3>Manajemen session &amp; ketahanan</h3>
 * Class ini tidak membuka session Hibernate sendiri; pembacaan data terjadi di dalam builder memakai
 * {@code currentSession()} (session request yang ditutup otomatis — tidak boleh ditutup manual).
 * Pemilihan nilai combobox dijaga dari {@code null} (memakai nilai berjalan sebagai cadangan), dan
 * builder menangani seluruh kegagalan secara ramah sehingga error data tidak menjatuhkan halaman.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 / ZKoss 5.5 (tanpa lambda/diamond/Stream), {@code try/catch} gaya Java 1.6. Komponen UI
 * di-<i>autowire</i> dari ZUL: dua {@link Combobox} (id {@code tahun}, {@code bulan}) dan satu
 * {@link MyChart} (id {@code pasienRawatJalan}) sebagai wadah grafik.
 *
 * @author AIS
 */
public class RawatJalanMingguanDashboardAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 8806806334907195811L;

	/** Wadah grafik (HTML/CSS) — diisi oleh {@link RawatJalanDashboardBuilder}. */
	private MyChart pasienRawatJalan;

	/** Pilihan tahun yang ditinjau. */
	private Combobox tahun;

	/** Pilihan bulan (1..12) yang ditinjau. */
	private Combobox bulan;

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

		if (bulan != null) {
			for (int i = 1; i <= 12; i++) {
				Comboitem comboitem = new Comboitem("" + i);
				comboitem.setValue(Integer.valueOf(i));
				bulan.appendChild(comboitem);
			}
			bulan.setSelectedIndex(Calendar.getInstance().get(Calendar.MONTH)); // 0-based → bulan berjalan.
			bulan.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					render();
				}
			});
		}

		render();
	}

	/**
	 * Menggambar (atau memperbarui) seluruh grafik sesuai tahun &amp; bulan yang sedang dipilih.
	 * Nilai combobox yang kosong digantikan nilai berjalan agar dasbor selalu punya konteks valid.
	 */
	public void render() {
		Integer th = (tahun == null || tahun.getSelectedItem() == null)
				? Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR))
				: (Integer) tahun.getSelectedItem().getValue();
		Integer bl = (bulan == null || bulan.getSelectedItem() == null)
				? Integer.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1)
				: (Integer) bulan.getSelectedItem().getValue();

		RawatJalanDashboardBuilder.renderMingguan(pasienRawatJalan, th, bl);
	}
}
