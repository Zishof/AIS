package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.HeadlessActionContext;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

public class MyGrid extends Grid {

	/**
	 *
	 */
	private static final long serialVersionUID = -8952577624352155452L;
	private boolean hasInit = false;

	/** Cache hasil query background agar tidak DB-call setiap konstruksi grid. TTL 5 menit. */
	private static volatile String cachedBgStyle = null;
	private static volatile long bgCacheTime = 0L;
	private static final long BG_CACHE_TTL_MS = 5L * 60 * 1000;

	public MyGrid() {
		super();
		initBg();
		setSclass("dgrid");
		init();
	}

	/**
	 * Normalisasi seluruh renderer grid lama maupun baru. ZK versi lama memanggil
	 * {@code RowRenderer.render(Row,Object)}, jadi mengandalkan overload tiga argumen
	 * pada {@link MyRowRenderer} saja tidak cukup. Wrapper ini memastikan sel tombol
	 * terakhir selalu diserap ke menu kebab dan kolomnya diperkecil.
	 */
	@Override
	public void setRowRenderer(final RowRenderer renderer) {
		if (renderer == null) {
			super.setRowRenderer((RowRenderer) null);
			return;
		}
		super.setRowRenderer(new RowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				renderer.render(row, data);
				UIHelper.absorptionKebab(row);
			}
		});
	}

	public void initBg() {
		if (HeadlessActionContext.isActive()) return;
		long now = System.currentTimeMillis();
		if (cachedBgStyle != null && (now - bgCacheTime) < BG_CACHE_TTL_MS) {
			setStyle(cachedBgStyle);
			return;
		}

		String defaultStyle = "background:url('" + Common.ROOT
				+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;"
				+ "-moz-background-size: cover;background-size: cover;-o-background-size: cover;";
		String resolvedStyle = defaultStyle;
		LampiranLain kop = null;
		try {
			Sekolah sekolah = SekolahUtil.getSekolah();

			if ((sekolah != null && sekolah.getId() != null)) {
				kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.BG_SEKOLAH);
				if (kop == null || kop.getId() == null) {
					kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.BG_YAYASAN);
				}
			}

			if (kop == null) {
				Yayasan yayasan = SekolahUtil.getYayasan();
				if ((yayasan != null && yayasan.getId() != null)) {
					kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.BG_YAYASAN);
				}
			}

			if (kop == null) {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if ((perguruanTinggi != null && perguruanTinggi.getId() != null)) {
					kop = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.BG_PT);
				}
			}

			if (kop != null) {
				resolvedStyle = "background:url('" + kop.createLinkUri(true, true)
						+ "') no-repeat center center fixed;-webkit-background-size: cover;"
						+ "-moz-background-size: cover;background-size: cover;-o-background-size: cover;";
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyGrid.java:86");
			// biarkan resolvedStyle = defaultStyle
		}

		cachedBgStyle = resolvedStyle;
		bgCacheTime = now;
		setStyle(resolvedStyle);
	}

	private void init() {
		hasInit = true;
		try {
			setVisible(false);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// FIX Error E (NPE getAttachedUiEngine via setVisible): callback Timer (lewat
					// CommonTimerHelper) bisa akhirnya fire SETELAH grid ini sudah lepas dari
					// halaman (halaman/tab ditutup pengguna sebelum Timer sempat jalan). getParent()
					// tetap bisa non-null walau sudah terlepas dari Page -- cek getPage() secara
					// eksplisit, skip diam-diam kalau sudah detached (bukan kondisi error).
					if (MyGrid.this.getPage() == null) {
						/*
						 * Grid yang memang belum pernah ditempel ke halaman (misalnya isi dialog
						 * yang dibangun secara lazy) masih memiliki parent == null. Timer boleh
						 * mengembalikan state visible-nya tanpa menyentuh UI engine. Tanpa ini,
						 * grid tetap hidden selamanya dan dialog hanya menampilkan area putih.
						 *
						 * Jika parent masih ada tetapi page sudah null, komponen berarti baru saja
						 * terlepas dari desktop; kondisi tersebut tetap dilewati untuk mencegah NPE
						 * getAttachedUiEngine yang menjadi alasan guard ini ditambahkan.
						 */
						if (MyGrid.this.getParent() == null) {
							MyGrid.this.setVisible(true);
						}
						return;
					}
					if (MyGrid.this.getParent() != null && MyGrid.this.getParent() instanceof North) {

						List<Component> componentsTool = UIUtil.checkGrigMobile(MyGrid.this);
						final North north = (North) MyGrid.this.getParent();
						Common.clear(north);
						north.setTitle("Menu");
						north.setBorder("none");
						/* PANEL FILTER: dahulu dipaksa "min-height: 250px" sehingga layar yang
						 * filternya hanya SATU baris (mis. Presensi) menyisakan pita kosong ~200px
						 * antara baris filter dan tabel data. Sekarang tinggi mengikuti isi.
						 *
						 * Perubahan ini TIDAK menyusutkan panel yang isinya memang tinggi: bila
						 * konten > nilai minimal, konten yang menentukan tinggi. Jadi efeknya hanya
						 * menghapus ruang yang memang tak terpakai.
						 *
						 * Rollback tanpa deploy ulang: set konfigurasi "tinggi_minimal_panel_filter"
						 * ke 250 untuk mengembalikan perilaku lama. */
						String tinggiMinimalPanel = "0";
						try {
							tinggiMinimalPanel = Common.getKonfigurasi("tinggi_minimal_panel_filter", "0").getNilai()
									.trim();
							if (tinggiMinimalPanel.length() == 0) {
								tinggiMinimalPanel = "0";
							}
						} catch (Throwable abaikan) {
							tinggiMinimalPanel = "0";
						}
						north.setStyle("min-height: " + tinggiMinimalPanel + "px;");

						setWidth("100%");

						Groupbox toolbarData1 = new Groupbox();
						toolbarData1.setParent(north);
						toolbarData1.setStyle("min-height: " + tinggiMinimalPanel + "px;");

						Row rowLagi = Common.tampilanScroll1(toolbarData1);
						rowLagi.appendChild(MyGrid.this);

						Groupbox toolbarData = new Groupbox();

						boolean ada = false;

						Button btnTambah = null;
						Button btnCari = null;
						if (componentsTool != null) {

							for (Component component : componentsTool) {

								if (component instanceof Button
										&& ((Button) component).getLabel().toLowerCase().contains("cari")) {
									btnCari = (Button) component;
									btnCari.setParent(toolbarData);
									ada = true;
									break;
								}
							}

							for (Component component : componentsTool) {
								if (component instanceof Button
										&& ((Button) component).getLabel().toLowerCase().contains("tambah")) {
									btnTambah = (Button) component;
									btnTambah.setParent(toolbarData);
									ada = true;
									break;
								}

							}
						}

						if (componentsTool != null) {

							List<Component> copy = new ArrayList<Component>();
							for (Component component : componentsTool) {
								if ((btnTambah == null || component != btnTambah)
										&& (btnCari == null || component != btnCari)) {
									copy.add(component);
								}
							}
							for (Component component : copy) {
								component.setParent(toolbarData);
								ada = true;
							}

						}

						if (ada) {
							Row rowUtama = new Row();
							toolbarData.setParent(rowUtama);
							rowUtama.setParent(rowLagi.getParent());
						}

					}

					// Kumpulkan semua descendant yg sudah di-setVisible(false) selama
					// grid tersembunyi.  ZK mengoptimalkan: jika parent hidden, Au command
					// setVisible(false) pada child tidak dikirim ke browser.  Akibatnya saat
					// grid ditampilkan kembali, child-child itu muncul padahal seharusnya
					// tersembunyi.
					// Solusi: setelah grid ditampilkan, paksa invalidate() pada tiap komponen
					// yang tersembunyi.  invalidate() memaksa ZK mengirim HTML penuh komponen
					// ke browser (bukan sekadar delta Au), sehingga browser menerima
					// display:none sesuai state isVisible()=false — terlepas dari apakah
					// state sebelumnya sudah false (ZK dedup biasa tidak berlaku untuk invalidate).
					List<Component> tersembunyi = new ArrayList<Component>();
					ambilComponentTersembunyi(MyGrid.this, tersembunyi);

					setVisible(true);

					for (Component c : tersembunyi) {
						/* Jaring pengaman: sebagian komponen ZK MELARANG setVisible (mis. Popup
						 * -> UnsupportedOperationException "Use open/close instead"). Satu
						 * komponen bermasalah TIDAK boleh menggagalkan penampilan seluruh grid,
						 * jadi tiap komponen diproses terpisah. */
						try {
							c.setVisible(false);
							c.invalidate();
						} catch (Throwable abaikan) {
							String pesan = abaikan.getMessage() == null ? "" : abaikan.getMessage();
							if (pesan.indexOf("Use open/close instead") < 0) {
								ais.common.ErrorAuditUtil.record(abaikan,
										"MyGrid.pulihkanVisibilitas:" + c.getClass().getName());
							}
						}
					}

				}
			}, "Harap tunggu, Sedang menyiapkan data..", false, 300);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyGrid.java:201"); 
		}
	}

	/** Rekursi ke seluruh pohon komponen; kumpulkan yg visible=false (tidak masuk ke dalam yg hidden). */
	private static void ambilComponentTersembunyi(Component parent, List<Component> hasil) {
		for (Object o : parent.getChildren()) {
			if (!(o instanceof Component)) {
				continue;
			}
			Component child = (Component) o;
			/* JANGAN kumpulkan Popup: ZK melarang setVisible pada Popup
			 * (UnsupportedOperationException "Use open/close instead") karena visibilitasnya
			 * dikelola sendiri lewat open()/close(). Popup memang selalu tampak "hidden" saat
			 * belum dibuka, sehingga tanpa filter ini SETIAP menu aksi kebab (⋯) pada baris
			 * grid ikut terjaring dan menggagalkan pemulihan visibilitas seluruh grid.
			 * Anak-anak di dalam Popup tetap ditelusuri agar perilaku lama tidak berubah. */
			if (child instanceof org.zkoss.zul.Popup) {
				ambilComponentTersembunyi(child, hasil);
				continue;
			}
			if (!child.isVisible()) {
				hasil.add(child);
				// tidak perlu rekursi ke dalam child yg hidden — parent hidden sudah cukup
			} else {
				ambilComponentTersembunyi(child, hasil);
			}
		}
	}

	public void setModelCheckMobile(ListModel strset) {
		setModelCheckMobile(strset, false);
	}

	public void setModelCheckMobile(ListModel strset, final Boolean ubahLangsung) {

		super.setModel(strset);

		if (ubahLangsung && !hasInit) {
			init();
		}

		if (ubahLangsung || Common.isMobile()) {

			hasInit = true;
			setVisible(false);
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						UIUtil.checkGrigMobile(MyGrid.this, ubahLangsung);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyGrid.java:245");
					}

					setVisible(true);
				}
			}, "Sedang menyiapkan data..", false, 500);
		}
	}

}
