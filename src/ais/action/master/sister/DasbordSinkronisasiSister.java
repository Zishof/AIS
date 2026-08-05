package ais.action.master.sister;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.DataSisterApi;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h2>Dasbor "Sinkronisasi Data SISTER" (mandiri, mengikuti pola dasbor Neo Feeder)</h2>
 *
 * <p>
 * Komponen dasbor yang dibuka dari tombol <b>"Sister"</b> pada bilah atas aplikasi — sejajar dengan tombol
 * "Neo Feeder". Menyediakan, dalam satu halaman: (a) bilah aksi untuk menyinkronkan data referensi &amp; data
 * dosen/Tridharma dari SISTER, menguji akun, serta menyegarkan tampilan; dan (b) <b>ringkasan visual</b>
 * (kartu angka + grafik batang, donat, radar/spider, dan tren) yang digambar dengan HTML/CSS modern (bukan
 * JFreeChart) sehingga rapi &amp; responsif di ponsel maupun komputer.
 * </p>
 *
 * <h3>Pemakaian ulang &amp; pemeliharaan</h3>
 * <p>
 * Komponen ini sengaja "tipis": seluruh logika penggambaran ringkasan didelegasikan ke
 * {@link DasborSisterUiHelper}, sedangkan dialog login &amp; pemilihan dosen (sinkron bertahap) didelegasikan
 * ke {@link SisterAksiHelper} — keduanya juga dipakai halaman kelola Data SISTER. Dengan begitu perbaikan di
 * kemudian hari cukup dilakukan di satu tempat. Proses sinkronisasi berat berjalan di thread latar aman pada
 * {@link DataSisterApi} (satu {@code openSession()} yang ditutup di {@code finally}).
 * </p>
 *
 * <h3>Hak akses</h3>
 * <p>
 * Kemunculan tombol "Sister" dikendalikan per-ROLE lewat {@code Tbmrole.getBolehAksesSister()} (default aktif
 * untuk ADMINISTRATOR, AKADEMIK, &amp; role bernama mengandung "akademik"/"admin"), menggantikan gerbang lama
 * berbasis konfigurasi. Lihat {@code Common.getApakahAdminBolehAksesSister()}.
 * </p>
 *
 * @author e-Campus
 */
public class DasbordSinkronisasiSister extends Div {

	private static final long serialVersionUID = 1L;

	/** Wadah bagian ringkasan (dipisah agar mudah disegarkan tanpa membangun ulang bilah aksi). */
	private Div isi;

	public DasbordSinkronisasiSister() {
		super();
		setWidth("100%");
		bangun();
	}

	/** Membangun bilah aksi + ringkasan. */
	private void bangun() {
		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("margin-bottom:8px;");
		toolbar.setParent(this);

		MyToolbarbuttonConfig btnReferensi = new MyToolbarbuttonConfig("Sinkronkan Data Referensi",
				"/img/Actions-view-media-equalizer-icon.png");
		btnReferensi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				MyMessageboxConfig.show(
						"Tarik seluruh data referensi dari SISTER sekarang? Proses berjalan di latar belakang.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) == MyMessageboxConfig.OK) {
									DataSisterApi.synDataSister();
								}
							}
						});
			}
		});
		btnReferensi.setParent(toolbar);

		MyToolbarbuttonConfig btnDosen = new MyToolbarbuttonConfig("Sinkronkan Data Dosen (SDM & Tridharma)",
				"/img/Actions-view-media-equalizer-icon.png");
		btnDosen.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				SisterAksiHelper.bukaDialogPilihDosen(getPage().getFirstRoot());
			}
		});
		btnDosen.setParent(toolbar);

		MyToolbarbuttonConfig btnLogin = new MyToolbarbuttonConfig("Login ke SISTER", "/img/svg/key.svg");
		btnLogin.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				SisterAksiHelper.bukaDialogLogin(getPage().getFirstRoot());
			}
		});
		btnLogin.setParent(toolbar);

		MyToolbarbuttonConfig btnSegar = new MyToolbarbuttonConfig("Segarkan Ringkasan", "/img/svg/refresh.svg");
		btnSegar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				segarkan();
			}
		});
		btnSegar.setParent(toolbar);

		isi = new Div();
		isi.setWidth("100%");
		isi.setParent(this);
		DasborSisterUiHelper.bangunRingkasan(isi);
	}

	/** Menghitung ulang &amp; menggambar ulang ringkasan (mis. setelah sinkronisasi selesai). */
	private void segarkan() {
		if (isi == null) {
			return;
		}
		Common.clear(isi);
		DasborSisterUiHelper.bangunRingkasan(isi);
	}
}
