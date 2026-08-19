package ais.action.master.koperasi.gudang;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import ais.action.master.koperasi.helper.LokasiKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisLokasi;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>JenisLokasiZkAction — pengelola master "Jenis Lokasi" versi <b>ZKoss</b> (native ZK 5.5).</h2>
 *
 * <p>
 * Layar ini adalah padanan ZKoss dari halaman JSP "Jenis Lokasi": menampilkan daftar kategori tempat
 * (Gudang, Outlet, Kasir, Entry Data, dsb) dalam sebuah {@link MyGrid} dan menyediakan tombol
 * tambah/ubah/hapus. Seluruh logika data <b>tidak diduplikasi</b> — layar ini memanggil util pusat
 * {@link LokasiKantinUtil} (query, simpan, hapus, serta gerbang hak akses), persis seperti sisi JSP.
 * Dengan begitu perilaku kedua versi dijamin konsisten dan pemeliharaan cukup di satu tempat.
 * </p>
 *
 * <h3>Hak akses</h3>
 * <p>
 * Tombol tambah/ubah/hapus hanya tampil bila {@link LokasiKantinUtil#bolehKelola(HttpServletRequest)}
 * bernilai benar (admin dan bukan pedagang/pemilik toko). Pengguna lain tetap dapat melihat daftar.
 * Karena gerbang yang sama dipakai sisi JSP dan ZK, tidak ada celah kebijakan yang berbeda.
 * </p>
 *
 * <h3>Sesi</h3>
 * <p>
 * Memakai {@link HibernateUtil#currentSession()} (dikelola kerangka kerja ZK) untuk baca/tulis melalui
 * util; sesi ini <b>tidak ditutup manual</b> sesuai aturan. Kelas dapat diluncurkan langsung (mis. dari
 * {@code display.zul}/peluncur kantin) karena punya konstruktor tanpa argumen yang membangun UI-nya
 * sendiri. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see LokasiKantinUtil
 * @see LokasiZkAction
 */
public class JenisLokasiZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123456001L;

	private MyGrid grid;
	private boolean boleh;

	public JenisLokasiZkAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Membangun tata letak: kepala (judul + tombol Tambah) lalu grid daftar. */
	private void init() {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		boleh = LokasiKantinUtil.bolehKelola(req);

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);

		Hlayout head = new Hlayout();
		head.setStyle("gap:10px;margin-bottom:8px;align-items:center");
		head.setParent(root);
		Div t = new Div();
		t.setParent(head);
		Label ttl = new Label(ais.common.Common.getBahasaConfig("Jenis Lokasi"));
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(t);
		Label desc = new Label(ais.common.Common.getBahasaConfig("Kategori tempat: Gudang, Outlet, Kasir, Entry Data, dan lainnya."));
		desc.setStyle("display:block;color:#64748b;font-size:12px");
		desc.setParent(t);
		if (boleh) {
			Button tambah = new Button("Tambah");
			tambah.setStyle("margin-left:auto");
			tambah.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					openForm(null);
				}
			});
			tambah.setParent(head);
		}

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(root);
		Columns cols = new Columns();
		cols.setParent(grid);
		tambahKolom(cols, "Nama", null);
		tambahKolom(cols, "Keterangan", null);
		tambahKolom(cols, "Aktif", "90px");
		if (boleh) {
			tambahKolom(cols, "", "120px");
		}
		new Rows().setParent(grid);

		refresh();
	}

	private void tambahKolom(Columns cols, String label, String width) {
		Column c = new Column(label);
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(cols);
	}

	/** Memuat ulang baris grid dari basis data (via util pusat). */
	private void refresh() {
		Rows rows = grid.getRows();
		rows.getChildren().clear();
		Session session = HibernateUtil.currentSession();
		List<JenisLokasi> daftar = LokasiKantinUtil.daftarJenisLokasi(session, false);
		for (int i = 0; i < daftar.size(); i++) {
			final JenisLokasi j = daftar.get(i);
			Row r = new Row();
			r.setParent(rows);

			String warna = j.getWarna() == null ? "#0d6efd" : j.getWarna();
			Label nm = new Label(j.getNama() == null ? "" : j.getNama());
			nm.setStyle("display:inline-block;padding:2px 10px;border-radius:999px;color:#fff;font-weight:600;background:"
					+ warna);
			nm.setParent(r);

			new Label(j.getKeterangan() == null ? "" : j.getKeterangan()).setParent(r);
			new Label(j.getAktif() != null && j.getAktif().booleanValue() ? "Aktif" : "Nonaktif").setParent(r);

			if (boleh) {
				Hlayout aksi = new Hlayout();
				aksi.setParent(r);
				Button ubah = new Button("Ubah");
				ubah.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						openForm(j);
					}
				});
				ubah.setParent(aksi);
				Button hapus = new Button("Hapus");
				hapus.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						onDelete(j);
					}
				});
				hapus.setParent(aksi);
			}
		}
	}

	/** Menampilkan dialog modal tambah/ubah. {@code j} null berarti tambah. */
	private void openForm(final JenisLokasi j) throws Exception {
		if (!boleh) {
			return;
		}
		final Window win = new Window(j == null ? "Tambah Jenis Lokasi" : "Ubah Jenis Lokasi", "normal", true);
		win.setWidth("420px");
		win.setParent(this);
		Vlayout box = new Vlayout();
		box.setStyle("padding:14px");
		box.setParent(win);

		final Textbox nama = tb(box, "Nama *", j == null ? "" : j.getNama());
		final Textbox ket = tb(box, "Keterangan", j == null ? "" : j.getKeterangan());
		final Textbox warna = tb(box, "Warna (hex, mis. #0d6efd)", j == null || j.getWarna() == null ? "#0d6efd" : j.getWarna());
		final Textbox ikon = tb(box, "Ikon (mis. fas fa-warehouse)", j == null || j.getIkon() == null ? "" : j.getIkon());
		new Label(ais.common.Common.getBahasaConfig("Urutan")).setParent(box);
		final Intbox urutan = new Intbox(j == null || j.getUrutan() == null ? Integer.valueOf(0) : j.getUrutan());
		urutan.setParent(box);
		final Checkbox aktif = new Checkbox("Aktif");
		aktif.setChecked(j == null || (j.getAktif() != null && j.getAktif().booleanValue()));
		aktif.setParent(box);

		Hlayout foot = new Hlayout();
		foot.setStyle("margin-top:12px;gap:8px");
		foot.setParent(box);
		Button batal = new Button("Batal");
		batal.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});
		batal.setParent(foot);
		Button simpan = new Button("Simpan");
		simpan.setStyle("background:#0d6efd;color:#fff");
		simpan.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().length() == 0) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi nama terlebih dahulu. Kolom Nama wajib diisi.");
					return;
				}
				try {
					HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
					String oleh = Common.getCurrentUser(req) != null ? Common.getCurrentUser(req).getUserNama() : null;
					String olehId = Common.getCurrentUser(req) != null ? String.valueOf(Common.getCurrentUser(req).getUserId()) : null;
					LokasiKantinUtil.simpanJenisLokasi(HibernateUtil.currentSession(), j == null ? null : j.getId(),
							nama.getValue(), ket.getValue(), aktif.isChecked(), warna.getValue(), ikon.getValue(),
							urutan.getValue(), oleh, olehId);
					win.detach();
					refresh();
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
					MyMessageboxConfig.showFormat("Mohon maaf, Bapak/Ibu. Penyimpanan data jenis lokasi tidak dapat diselesaikan. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan dan kebenaran data yang diisi; (2) coba simpan ulang beberapa saat lagi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, ex.getMessage());
				}
			}
		});
		simpan.setParent(foot);

		win.doModal();
	}

	/** Konfirmasi lalu hapus satu jenis lokasi. */
	private void onDelete(final JenisLokasi j) throws Exception {
		MyMessageboxConfig.showFormatCb("Apakah Bapak/Ibu yakin ingin menghapus jenis lokasi \"{V1}\"? Data yang telah dihapus tidak dapat dikembalikan.", "Konfirmasi",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					public void onEvent(Event e) throws Exception {
						if (new Integer(e.getData().toString()).intValue() == MyMessageboxConfig.OK) {
							try {
								LokasiKantinUtil.hapusJenisLokasi(HibernateUtil.currentSession(), j.getId());
								refresh();
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
								MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu. Jenis lokasi ini tidak dapat dihapus karena kemungkinan masih digunakan oleh lokasi lain. Langkah yang dapat dilakukan: (1) pastikan tidak ada lokasi yang masih menggunakan jenis lokasi ini; (2) pindahkan atau hapus terlebih dahulu lokasi yang terkait; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				}, (j.getNama() == null ? "" : j.getNama()));
	}

	/** Membuat pasangan Label + Textbox pada kotak, mengembalikan Textbox-nya. */
	private Textbox tb(Vlayout box, String label, String value) {
		new Label(label).setParent(box);
		Textbox t = new Textbox(value == null ? "" : value);
		t.setWidth("100%");
		t.setParent(box);
		return t;
	}
}
