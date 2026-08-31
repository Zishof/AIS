package ais.action.master.obe;

import java.util.List;

import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk capaian lulusan vs profil lulusan. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Div}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox fakultas}, {@code Combobox
 * jurusan}, {@code Center center}, {@code java.util.List loadedCpl}, {@code java.util.List loadedPl}, {@code
 * PerguruanTinggi perguruanTinggi}, {@code Textbox nama}, {@code Textbox nama1}; inisialisasi/lifecycle ({@code
 * initKHS()}, {@code init()}); operasi domain lain ({@code onKHS()}, {@code showMatriks()}, {@code esc()},
 * {@code createTransferAdapter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Div
 */
public class CapaianLulusanVsProfilLulusanAction extends Div {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;

	private Center center;
	private java.util.List<CapaianLulusan> loadedCpl = null;
	private java.util.List<ProfilLulusan> loadedPl = null;

	public CapaianLulusanVsProfilLulusanAction() {
		setWidth("100%");
		setHeight("100%");
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public CapaianLulusanVsProfilLulusanAction(String title, String border, boolean closable) throws Exception {
		setWidth("100%");
		setHeight("100%");
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

	}

	private PerguruanTinggi perguruanTinggi;

	private Textbox nama;

	private Textbox nama1;

	private void init() throws Exception {
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		// Setelah panel native dipindahkan ke MyButtonTabbox, Borderlayout tidak
		// lagi mendapat ukuran implisit dari Tabpanel. Samakan dengan panel ZUL
		// Kategori CPL yang selalu memiliki lebar dan tinggi eksplisit.
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setParent(this);

		North west = new North();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		Div obeNorth = ObePageHelpHelper.pasangPadaNorth(west, "CPL vs Profil Lulusan",
				"Hubungkan CPL dengan Profil Lulusan. Centang profil yang didukung oleh CPL pada setiap baris; satu CPL boleh mendukung lebih dari satu profil.");
		west.setHeight("142px");

		/* Hindari transformasi otomatis MyGrid -> North berjudul "Menu". */
		Div filterContainer = new Div();
		filterContainer.setWidth("100%");
		filterContainer.setHeight("72px");
		filterContainer.setParent(obeNorth);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(filterContainer);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas" + " *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " *"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Profil"));
		row.appendChild(nama1 = new Textbox());
		nama1.setWidth("90%");

		nama1.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyButtonConfig button = new MyButtonConfig("Refresh");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		MyButtonConfig matriksBtn = new MyButtonConfig("Lihat Matriks");
		matriksBtn.setParent(row);
		matriksBtn.setStyle("margin-left:6px;background:#1d4ed8;color:#fff;border-radius:4px;padding:2px 10px;border:0");
		matriksBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				showMatriks();
			}
		});
		ObeRelationMatrixTransferHelper.pasang(row, createTransferAdapter());

	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({ "deprecation", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Common.clear(center);
					if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + "Fakultas", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					Jurusan jurusan = (Jurusan) CapaianLulusanVsProfilLulusanAction.this.jurusan.getSelectedItem()
							.getValue();
					List<ProfilLulusan> profilLulusans = ConstantValues.simpleList(HibernateUtil.currentSession()
							.createCriteria(ProfilLulusan.class)

							.add(nama1.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama1.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama1.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							ProfilLulusan.class);

					List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(HibernateUtil.currentSession()
							.createCriteria(CapaianLulusan.class)

							.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianLulusan.class);

					MyGrid grid = new MyGrid();
					ais.ui.util.ZkCompat.setFixedLayout(grid, false);
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Kode");
					column.setParent(columns);
					column.setWidth("80px");

					column = new MyColumnConfig("Capaian");
					column.setParent(columns);
					column.setWidth("300px");

					for (ProfilLulusan profilLulusan : profilLulusans) {
						column = new MyColumnConfig(profilLulusan.getKode());
						column.setParent(columns);
						column.setWidth("40px");
						column.setTooltiptext(profilLulusan.getKode() + " " + profilLulusan.getNama());
					}

					Rows rows = new Rows();
					rows.setParent(grid);

					loadedCpl = capaianLulusans;
					loadedPl = profilLulusans;

					for (final CapaianLulusan capaianLulusan : capaianLulusans) {
						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(capaianLulusan.getKode()));
						row.appendChild(ObeBaseAction.ringkasanKeterangan(capaianLulusan.getNama()));
						for (final ProfilLulusan profilLulusan : profilLulusans) {
							final Checkbox checkbox = new Checkbox();
							checkbox.setTooltiptext(profilLulusan.getKode() + " " + profilLulusan.getNama());
							checkbox.setChecked(ObeRelationMatrixTransferHelper.containsId(capaianLulusan.getProfil(), profilLulusan.getId()));
							row.appendChild(checkbox);
							checkbox.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									String p = ObeRelationMatrixTransferHelper.setId(capaianLulusan.getProfil(),
											profilLulusan.getId(), checkbox.isChecked());
									capaianLulusan.setProfil(p);
									Common.refreshUpdate(capaianLulusan);
								}
							});
						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

	private void showMatriks() {
		if (loadedCpl == null || loadedPl == null || loadedCpl.isEmpty()) {
			try {
				MyMessageboxConfig.show("Klik Refresh dahulu untuk memuat data.", "Info",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianLulusanVsProfilLulusanAction.java:314"); /* ignore */ }
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<style>");
		sb.append(".mx{font-family:Arial,sans-serif;padding:14px;color:#1e293b;overflow-x:auto}");
		sb.append(".mx h3{margin:0 0 10px;color:#1e40af;font-size:13pt}");
		sb.append(".mxt{border-collapse:collapse;font-size:9pt}");
		sb.append(".mxt th,.mxt td{border:1px solid #cbd5e1;padding:4px 6px;text-align:center}");
		sb.append(".lkode{background:#dbeafe;text-align:left;white-space:nowrap;font-weight:bold;position:sticky;left:0;z-index:1}");
		sb.append(".lnama{background:#f0f7ff;text-align:left;font-size:8pt;max-width:240px}");
		sb.append(".hcol{background:#dbeafe;font-weight:bold;writing-mode:vertical-rl;transform:rotate(180deg);height:80px;min-width:30px;cursor:default}");
		sb.append(".yes{background:#16a34a;color:#fff;font-weight:bold}");
		sb.append(".no{background:#f8fafc}");
		sb.append(".tot{background:#fef9c3;font-weight:bold}");
		sb.append(".corner{background:#e2e8f0;font-size:8pt;font-weight:bold}");
		sb.append(".summ{font-size:9pt;color:#64748b;margin-top:8px;padding:6px 0}");
		sb.append("</style>");
		sb.append("<div class='mx'>");
		sb.append("<h3>Matriks CPL &times; Profil Lulusan (PL)</h3>");
		sb.append("<table class='mxt'><thead><tr>");
		sb.append("<th class='corner lkode'>CPL</th><th class='corner lnama'>Deskripsi CPL</th>");
		for (ProfilLulusan pl : loadedPl) {
			sb.append("<th class='hcol' title='").append(esc(pl.getNama())).append("'>")
			  .append(esc(pl.getKode())).append("</th>");
		}
		sb.append("<th class='corner tot'>#</th></tr></thead><tbody>");
		int[] colTotals = new int[loadedPl.size()];
		int grand = 0;
		for (CapaianLulusan cpl : loadedCpl) {
			int rowTotal = 0;
			sb.append("<tr><td class='lkode'>").append(esc(cpl.getKode())).append("</td>");
			sb.append("<td class='lnama'>").append(esc(cpl.getNama())).append("</td>");
			for (int i = 0; i < loadedPl.size(); i++) {
				boolean mapped = ObeRelationMatrixTransferHelper.containsId(cpl.getProfil(), loadedPl.get(i).getId());
				if (mapped) { rowTotal++; colTotals[i]++; grand++; }
				sb.append("<td class='").append(mapped ? "yes" : "no").append("'>")
				  .append(mapped ? "&#10003;" : "&nbsp;").append("</td>");
			}
			sb.append("<td class='tot'>").append(rowTotal).append("</td></tr>");
		}
		sb.append("<tr><td class='tot' colspan='2'>Total</td>");
		for (int t : colTotals) sb.append("<td class='tot'>").append(t).append("</td>");
		sb.append("<td class='tot'>").append(grand).append("</td></tr>");
		sb.append("</tbody></table>");
		int max = loadedCpl.size() * loadedPl.size();
		int pct = max == 0 ? 0 : grand * 100 / max;
		sb.append("<div class='summ'>Cakupan: <b>").append(grand).append("/").append(max)
		  .append("</b> (").append(pct).append("%) &mdash; ")
		  .append(loadedCpl.size()).append(" CPL &times; ").append(loadedPl.size()).append(" PL</div>");
		sb.append("</div>");
		MyWindow win = new MyWindow("Matriks CPL × Profil Lulusan", "normal", true);
		win.setWidth("90%");
		win.setHeight("88%");
		org.zkoss.zul.Div d = new org.zkoss.zul.Div();
		d.setStyle("overflow:auto;height:100%;padding:2px");
		d.setParent(win);
		new org.zkoss.zul.Html(sb.toString()).setParent(d);
		try { win.onModal(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
	}

	private static String esc(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private ObeRelationMatrixTransferHelper.MatrixAdapter createTransferAdapter() {
		return new ObeRelationMatrixTransferHelper.MatrixAdapter() {
			public String getTitle() { return "CPL vs Profil Lulusan"; }
			public String getRowLabel() { return "CPL"; }
			public String getColumnLabel() { return "Profil Lulusan"; }
			public int getRowCount() { return loadedCpl == null ? 0 : loadedCpl.size(); }
			public int getColumnCount() { return loadedPl == null ? 0 : loadedPl.size(); }
			public String getRowCode(int row) { return loadedCpl.get(row).getKode(); }
			public String getRowName(int row) { return loadedCpl.get(row).getNama(); }
			public String getColumnCode(int column) { return loadedPl.get(column).getKode(); }
			public String getColumnName(int column) { return loadedPl.get(column).getNama(); }
			public boolean isSelected(int row, int column) {
				return ObeRelationMatrixTransferHelper.containsId(loadedCpl.get(row).getProfil(), loadedPl.get(column).getId());
			}
			public void applyRow(int row, boolean[] selected) throws Exception {
				CapaianLulusan cpl = loadedCpl.get(row);
				String ids = cpl.getProfil();
				for (int i = 0; i < loadedPl.size(); i++)
					ids = ObeRelationMatrixTransferHelper.setId(ids, loadedPl.get(i).getId(), selected[i]);
				cpl.setProfil(ids);
				Common.refreshUpdate(cpl);
			}
			public void refresh() throws Exception { onKHS(null); }
		};
	}

}
