package ais.action.master.inventory;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.GrupProduk;
import ais.database.model.inventory.Produk;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Grup Produk -- kendali HPP/harga jual terpusat lintas toko (lihat javadoc {@link GrupProduk}).
 * Menyimpan grup MENYALIN harganya ke seluruh {@link Produk} anggota di semua toko/outlet.
 *
 * <p>Penyalinan dilakukan per-baris lewat session Hibernate (bukan bulk HQL) SENGAJA: {@code Produk}
 * ber-{@code @Audited}, dan bulk update HQL melewati Envers sehingga perubahan harga massal tidak
 * meninggalkan jejak audit sama sekali -- untuk perubahan finansial lintas 90 outlet, jejak audit
 * per-baris justru bagian terpenting fiturnya. Volume per grup ~1 produk x jumlah outlet (puluhan
 * s.d. ratusan baris), flush periodik tiap 250 baris (pola sama commit periodik provisioning demo).</p>
 */
public class GrupProdukAction extends GenericCrudAction<GrupProduk> {

	private static final long serialVersionUID = -5779730267402400331L;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private MyDoublebox hargaBeli;
	private MyDoublebox hargaJual;

	@Override
	protected Class<GrupProduk> getEntityClass() {
		return GrupProduk.class;
	}

	@Override
	protected GrupProduk createNewEntity() {
		return new GrupProduk();
	}

	@Override
	protected String getWindowTitle() {
		return "Grup Produk (Harga Terpusat)";
	}

	@Override
	protected String[] getDownloadUploadContents() {
		return new String[] { "id", "kode", "nama", "keterangan", "hargaBeli", "hargaJual", "aktif" };
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GrupProduk.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}
		criteria.add(searchnama.getValue().trim().isEmpty()
				? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@Override
	protected MyRowRenderer createRenderer() {
		return new GrupProdukRenderer();
	}

	@Override
	protected void buildFormContent(MyWindow window, final GrupProduk grup) throws Exception {

		org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

		org.zkoss.zul.Center center = new org.zkoss.zul.Center();
		center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
		ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Div cardWrap = new Div();
		cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
		cardWrap.setParent(center);

		Grid formGrid = new Grid();
		formGrid.setStyle("border:none;width:100%;");
		formGrid.setParent(cardWrap);

		Rows rows = new Rows();
		rows.setParent(formGrid);

		FormBuilder fb = new FormBuilder(rows);

		kode = new Textbox(grup.getKode());
		kode.setWidth("100%");
		fb.addRow("Kode Grup", kode, "Singkatan unik, mis. AYAM-MRN  KULIT-MRN");

		nama = new Textbox(grup.getNama());
		nama.setWidth("100%");
		fb.addRow("Nama Grup *", nama, "Mis. Ayam Marinasi -- produk dgn bahan sama di seluruh outlet");

		keterangan = new Textbox(grup.getKeterangan());
		keterangan.setWidth("100%");
		keterangan.setRows(3);
		fb.addRow("Keterangan", keterangan, "Catatan tambahan bila diperlukan");

		fb.addSectionHeader("Harga Terpusat -- diterapkan ke SEMUA produk anggota di seluruh toko");

		hargaBeli = new MyDoublebox(grup.getHargaBeli());
		hargaBeli.setWidth("100%");
		fb.addRow("HPP / Harga Beli", hargaBeli,
				"Kosongkan bila HPP tidak dikendalikan grup (harga beli anggota tidak disentuh)");

		hargaJual = new MyDoublebox(grup.getHargaJual());
		hargaJual.setWidth("100%");
		fb.addRow("Harga Jual", hargaJual,
				"Kosongkan bila harga jual tidak dikendalikan grup (harga jual anggota tidak disentuh)");

		org.zkoss.zul.South south = new org.zkoss.zul.South();
		ZkCompat.setFlex(south, true);
		south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("padding:6px 12px;");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup tanpa menyimpan");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan & Terapkan ke Semua Outlet", "/img/save.gif");
		save.setTooltiptext("Simpan grup dan salin harga ke seluruh produk anggota");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(window);
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().isEmpty()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan Grup Produk",
					"Kolom Nama Grup belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi terlebih dahulu Nama Grup.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (checkNamaDuplikat()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan Grup Produk",
					"Nama Grup sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama grup yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar grup yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}
		Session session = HibernateUtil.currentSession();
		GrupProduk grup = currentEntity;
		if (grup.getId() != null) {
			grup = (GrupProduk) session.load(GrupProduk.class, grup.getId());
			currentEntity = grup;
		}
		grup.setKode(kode.getValue());
		grup.setNama(nama.getValue());
		grup.setKeterangan(keterangan.getValue());
		grup.setHargaBeli(hargaBeli.getValue() == null ? null : hargaBeli.getValue().doubleValue());
		grup.setHargaJual(hargaJual.getValue() == null ? null : hargaJual.getValue().doubleValue());
		Common.refreshSaveOrUpdate(session, grup);

		int diterapkan = terapkanHargaKeAnggota(session, grup);
		if (diterapkan > 0) {
			ais.ui.util.MyMessageboxConfig.show(
					"Harga grup diterapkan ke " + diterapkan + " produk anggota di seluruh toko/outlet.",
					"Informasi", ais.ui.util.MyMessageboxConfig.OK,
					ais.ui.util.MyMessageboxConfig.INFORMATION);
		}
		return true;
	}

	/** Delegasi ke logika bersama lintas platform (ZK/JSP/Desktop/Android). */
	private int terapkanHargaKeAnggota(Session session, GrupProduk grup) {
		return GrupProdukUtil.terapkanHargaKeAnggota(session, grup);
	}

	private boolean checkNamaDuplikat() {
		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(GrupProduk.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(currentEntity.getId() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", currentEntity.getId()))
				.uniqueResult()).intValue();
		return count != 0;
	}

	class GrupProdukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final GrupProduk grup = (GrupProduk) arg1;
			new Label(grup.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(GrupProduk.class, grup, grup.getNama()).setParent(arg0);
			new Label(grup.getHargaBeli() == null ? "-" : Common.formatNumber(grup.getHargaBeli(), 0))
					.setParent(arg0);
			new Label(grup.getHargaJual() == null ? "-" : Common.formatNumber(grup.getHargaJual(), 0))
					.setParent(arg0);

			new Label(String.valueOf(
					GrupProdukUtil.jumlahAnggota(HibernateUtil.currentSession(), grup)))
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grup.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					grup.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grup);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, grup, GrupProdukAction.this).setParent(arg0);
		}
	}

	@Override
	protected String getHelpContent() {
		return "<div style='font-family:sans-serif;font-size:13px;line-height:1.8;color:#1e293b;'>"
				+ "<h3 style='margin-top:0;color:#1d4ed8;border-bottom:2px solid #dbeafe;padding-bottom:6px;'>"
				+ "&#128218; Panduan Penggunaan &mdash; Grup Produk (Harga Terpusat)</h3>"

				+ "<p>Modul ini mengendalikan <b>HPP dan harga jual secara terpusat</b> untuk produk yang sama "
				+ "di banyak toko/outlet. Ubah harga sekali di grup &mdash; sistem menyalinnya ke seluruh "
				+ "produk anggota di semua outlet, tanpa perlu membuka outlet satu per satu.</p>"

				+ "<h4 style='color:#0f172a;'>&#128221; Cara Pakai</h4>"
				+ "<ol style='margin-top:0;padding-left:18px;'>"
				+ "<li>Buat grup di sini (mis. \"Ayam Marinasi\").</li>"
				+ "<li>Buka layar <b>Produk</b>, sunting produk terkait di tiap toko, pilih <b>Grup Produk</b> ini.</li>"
				+ "<li>Kembali ke sini, isi HPP/Harga Jual, klik <b>Simpan &amp; Terapkan ke Semua Outlet</b>.</li>"
				+ "</ol>"

				+ "<div style='background:#fff7ed;border:1px solid #fed7aa;border-radius:6px;"
				+ "padding:10px 14px;margin-top:10px;'>"
				+ "&#9888;&#65039; <b>Perhatian:</b> Suntingan harga lokal per-outlet akan <b>tertimpa</b> "
				+ "setiap kali grup disimpan &mdash; grup adalah sumber kebenaran harga anggotanya. "
				+ "Kolom harga yang dikosongkan tidak disalin (harga anggota dibiarkan apa adanya). "
				+ "Setiap perubahan tercatat pada jejak audit per produk.</div>"
				+ "</div>";
	}
}
