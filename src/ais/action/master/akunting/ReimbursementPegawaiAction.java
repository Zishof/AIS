package ais.action.master.akunting;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.JenisPengeluaran;
import ais.database.model.akunting.JenisReimbursement;
import ais.database.model.akunting.ReimbursementPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.FormSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * <h3>Reimbursement Pegawai — pola UangMuka (SOP-driven, persetujuan di AKHIR).</h3>
 *
 * <p>Rework total dari versi multi-tab (Pengajuan Baru/Pengajuan Saya/Persetujuan
 * Atasan/Pembayaran Finance) menjadi klon pola {@link UangMukaAction}:</p>
 * <ul>
 *   <li>{@code implements FormSop}: form diinstansiasi mesin SOP
 *       (TampilanAlurSopAction) via {@code AlurSop.formInputan}; persetujuan
 *       terjadi di langkah AKHIR disposisi (bukan tombol approve di layar ini).
 *       Status/penyetuju diturunkan dari {@code DisposisiSop} di entity.</li>
 *   <li>Pemilihan Anggaran ({@link AmbilDataWorkspaceBanbox}) + Satuan Kerja,
 *       persis uang muka. TANPA sumber Permintaan Pembelian (PR).</li>
 *   <li>Rincian barang/biaya meniru {@link KasKecilAction}: baris JSON
 *       {@code formula} (key/akun/nama/qty/harga/jumlah/tanggal) dengan picker
 *       {@link AmbilDataAkunBanbox} per baris; total baris = nominal dokumen.</li>
 *   <li>Integrasi DPC: saat status DISETUJUI, dokumen otomatis masuk daftar DPC
 *       lewat {@link DaftarPengajuanTransfer#simpanReimbursement} (post-save
 *       timer + safety-net renderer) sehingga dapat diproses
 *       {@link ProsesTransferAction}.</li>
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class ReimbursementPegawaiAction extends GenericAutowireComposer
		implements FormSop, ais.ui.util.DataInitDefault {

	// ===== komponen halaman (autowire reimbursement_pegawai.zul) =====
	private MyWindow window;
	private MyWindow addWindow;
	private MyGrid grid;
	private Textbox serachnama;
	private Textbox serachkode;
	private Combobox searchstatus;
	private AmbilDataWorkspaceBanbox searchAnggaran;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;
	private Checkbox searchtelahDpc;
	private MyDatebox start;
	private MyDatebox end;
	private Tabpanel statistik;
	private Tabpanel monitor;

	// ===== state form (FormSop) =====
	private ReimbursementPegawai reimbursement;
	private DisposisiSop disposisiSop;
	private boolean persetujuan;
	private boolean viewOnly;
	private Tbmuser tbmuser;
	private JSONArray array = new JSONArray();

	// kontrol form
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataWorkspaceBanbox workspace;
	private Combobox jenisReimbursementCombo;
	private MyFormRow rowAnggaran;
	private MyFormRow rowAkunInfo;
	private Label akunInfo;
	private Tabpanel jenisPanel;
	private Tabpanel jenisPengeluaranPanel;
	private List jenisPengeluaranList;
	private Label kode;
	private Textbox nama;
	private AmbilDataPegawaiBanbox pegawaiPenerima;
	private MyDatebox tanggalKegiatan;
	private Textbox keterangan;
	private Label unit;
	private Label saldoAnggaran;
	private Vbox itemBox;
	private Label footerTotal;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		tbmuser = Common.getCurrentUser();
		if (addWindow != null) {
			addWindow.setVisible(false);
		}
		if (searchstatus != null && searchstatus.getItemCount() == 0) {
			searchstatus.appendItem("");
			searchstatus.appendItem(ReimbursementPegawai.DIAJUKAN);
			searchstatus.appendItem(ReimbursementPegawai.DISETUJUI);
			searchstatus.appendItem(ReimbursementPegawai.DITOLAK);
		}
		onSearchDefault(null);
	}

	// =====================================================================
	// Halaman daftar (klon layout uang_muka.zul)
	// =====================================================================

	/** Kontrak DataInitDefault: tombol Ubah baris (Common.copyEditDeleteButtons) membuka form ini. */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		persetujuan = false;
		ReimbursementPegawai d = (ReimbursementPegawai) obj;
		bukaForm(d, d.getDisposisiSop(), false);
	}

	@Override
	public void onSearchDefault(Event event) {
		try {
			cariData();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit ReimbursementPegawaiAction.onSearchDefault");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void cariData() throws Exception {
		if (grid == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		Criteria c = session.createCriteria(ReimbursementPegawai.class);

		if (serachnama != null && !serachnama.getValue().trim().isEmpty()) {
			String cari = "%" + serachnama.getValue().trim() + "%";
			c.add(Restrictions.or(Restrictions.ilike("nama", cari), Restrictions.ilike("deskripsi", cari)));
		}
		if (serachkode != null && !serachkode.getValue().trim().isEmpty()) {
			c.add(Restrictions.ilike("kode", "%" + serachkode.getValue().trim() + "%"));
		}
		if (searchAnggaran != null && searchAnggaran.getAttribute("workspace") instanceof Workspace) {
			c.add(Restrictions.eq("workspace", (Workspace) searchAnggaran.getAttribute("workspace")));
		}
		if (searchparent != null && searchparent.getAttribute("satuanKerja") instanceof SatuanKerja) {
			c.add(Restrictions.eq("satuanKerja", (SatuanKerja) searchparent.getAttribute("satuanKerja")));
		}
		if (searchtelahDpc != null && searchtelahDpc.isChecked()) {
			c.add(Restrictions.isNotNull("daftarPengajuanTransfer"));
		}
		if (start != null && start.getValue() != null) {
			c.add(Restrictions.ge("tanggalPengajuan", start.getValue()));
		}
		if (end != null && end.getValue() != null) {
			java.util.Calendar cal = WaktuUtil.getCalendar();
			cal.setTime(end.getValue());
			cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
			cal.set(java.util.Calendar.MINUTE, 59);
			cal.set(java.util.Calendar.SECOND, 59);
			c.add(Restrictions.le("tanggalPengajuan", cal.getTime()));
		}
		c.addOrder(Order.desc("id")).setMaxResults(300);
		List list = c.list();

		// filter turunan (status & aktif dihitung dari DisposisiSop, bukan kolom murni)
		String pilihStatus = searchstatus == null || searchstatus.getSelectedItem() == null ? ""
				: searchstatus.getSelectedItem().getLabel().trim();
		java.util.List hasil = new java.util.ArrayList();
		for (int i = 0; i < list.size(); i++) {
			ReimbursementPegawai d = (ReimbursementPegawai) list.get(i);
			try {
				if (!pilihStatus.isEmpty() && !pilihStatus.equalsIgnoreCase(d.getStatus())) {
					continue;
				}
				if (searchaktif != null && searchaktif.isChecked() && !Boolean.TRUE.equals(d.getAktif())) {
					continue;
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.onSearchDefault-filter");
			}
			hasil.add(d);
		}

		grid.setRowRenderer(new ReimbursementRenderer());
		grid.setModelCheckMobile(new SimpleListModel(hasil));
	}

	public void onAdd(Event event) throws Exception {
		persetujuan = false;
		viewOnly = false;
		bukaForm(new ReimbursementPegawai(), null, false);
	}

	/** Buka addWindow berisi form() — dipakai onAdd, tombol Ubah, dan tombol Lihat. */
	private void bukaForm(ReimbursementPegawai data, DisposisiSop dispo, boolean readOnly) throws Exception {
		if (addWindow == null) {
			return;
		}
		viewOnly = readOnly;
		addWindow.getChildren().clear();

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setParent(addWindow);

		MyToolbarbuttonConfig save = null;
		if (!readOnly) {
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("32px");
			toolbar.setParent(isi);
			save = new MyToolbarbuttonConfig("Simpan / Ajukan", "/img/save.gif");
			save.setParent(toolbar);
		}

		MyGrid f = form(data, dispo, save, null);
		f.setParent(isi);

		if (save != null) {
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (onSave(arg0)) {
						addWindow.setVisible(false);
						onSearchDefault(null);
					}
				}
			});
		}

		addWindow.setTitle(readOnly ? "Rincian Reimbursement"
				: (data.getId() == null ? "Tambah Reimbursement" : "Ubah Reimbursement"));
		addWindow.setVisible(true);
		addWindow.doHighlighted();
	}

	public void onStatistik(Event event) throws Exception {
		if (statistik == null) {
			return;
		}
		statistik.getChildren().clear();
		List list = HibernateUtil.currentSession().createCriteria(ReimbursementPegawai.class)
				.addOrder(Order.desc("id")).setMaxResults(1000).list();
		int aju = 0, setuju = 0, tolak = 0;
		double total = 0, totalSetuju = 0;
		for (int i = 0; i < list.size(); i++) {
			ReimbursementPegawai d = (ReimbursementPegawai) list.get(i);
			try {
				String st = d.getStatus();
				total += d.getNominal() == null ? 0 : d.getNominal().doubleValue();
				if (ReimbursementPegawai.DISETUJUI.equals(st)) {
					setuju++;
					totalSetuju += d.getNominal() == null ? 0 : d.getNominal().doubleValue();
				} else if (ReimbursementPegawai.DITOLAK.equals(st)) {
					tolak++;
				} else {
					aju++;
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.onStatistik");
			}
		}
		Vbox v = new Vbox();
		v.setParent(statistik);
		tulis(v, "Total pengajuan: " + list.size() + " (Rp " + Common.numberFormat.get().format(total) + ")");
		tulis(v, "Dalam proses: " + aju);
		tulis(v, "Disetujui: " + setuju + " (Rp " + Common.numberFormat.get().format(totalSetuju) + ")");
		tulis(v, "Ditolak: " + tolak);
	}

	public void onMonitor(Event event) throws Exception {
		if (monitor == null) {
			return;
		}
		monitor.getChildren().clear();
		Vbox v = new Vbox();
		v.setParent(monitor);
		tulis(v, "Reimbursement DISETUJUI dan status DPC-nya:");
		List list = HibernateUtil.currentSession().createCriteria(ReimbursementPegawai.class)
				.addOrder(Order.desc("id")).setMaxResults(300).list();
		int n = 0;
		for (int i = 0; i < list.size(); i++) {
			ReimbursementPegawai d = (ReimbursementPegawai) list.get(i);
			try {
				if (!ReimbursementPegawai.DISETUJUI.equals(d.getStatus())) {
					continue;
				}
				n++;
				Vbox baris = new Vbox();
				baris.setParent(v);
				tulis(baris, d.getKode() + " — " + (d.getNama() == null ? d.getDeskripsi() : d.getNama()) + " (Rp "
						+ Common.numberFormat.get().format(d.getNominal()) + ")");
				if (d.getDaftarPengajuanTransfer() != null) {
					DaftarPengajuanTransfer.tampilStatus(d.getDaftarPengajuanTransfer(), baris);
				} else {
					tulis(baris, "  Status DPC : menunggu dimasukkan ke daftar transfer");
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.onMonitor");
			}
		}
		if (n == 0) {
			tulis(v, "(belum ada)");
		}
	}

	private void tulis(Component parent, String teks) {
		Label l = new Label(teks);
		l.setParent(parent);
	}

	// =====================================================================
	// Tab CRUD Jenis Reimbursement (setelah tab Monitor)
	// =====================================================================

	private boolean bolehKelolaJenis() {
		Tbmuser u = tbmuser != null ? tbmuser : Common.getCurrentUser();
		try {
			return u != null && u.ambilRolesId().contains(ais.database.model.Tbmrole.ADMINISTRATOR);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.bolehKelolaJenis");
			return false;
		}
	}

	public void onJenisReimbursement(Event event) throws Exception {
		if (jenisPanel == null) {
			return;
		}
		jenisPanel.getChildren().clear();

		final boolean bolehKelola = bolehKelolaJenis();
		Vbox v = new Vbox();
		v.setWidth("100%");
		v.setParent(jenisPanel);

		if (bolehKelola) {
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("32px");
			toolbar.setParent(v);
			MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Jenis", "/img/new.gif");
			tambah.setParent(toolbar);
			tambah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					bukaFormJenis(new JenisReimbursement());
				}
			});
		} else {
			tulis(v, "Hanya administrator yang dapat menambah/mengubah Jenis Reimbursement.");
		}

		Grid g = new Grid();
		g.setWidth("100%");
		g.setParent(v);
		Columns columns = new Columns();
		columns.setParent(g);
		String[] judul = new String[] { "Nama", "Menggunakan Anggaran", "Akun", "Satuan Kerja", "Keterangan", "Aktif", "" };
		String[] lebar = new String[] { "18%", "14%", "20%", "16%", "18%", "6%", "8%" };
		for (int i = 0; i < judul.length; i++) {
			MyColumnConfig col = new MyColumnConfig();
			col.setLabel(judul[i]);
			col.setWidth(lebar[i]);
			col.setParent(columns);
		}
		Rows rowsJenis = new Rows();
		rowsJenis.setParent(g);

		List list = HibernateUtil.currentSession().createCriteria(JenisReimbursement.class)
				.addOrder(Order.asc("id")).list();
		for (int i = 0; i < list.size(); i++) {
			final JenisReimbursement j = (JenisReimbursement) list.get(i);
			Row r = new Row();
			r.setValign("top");
			r.setParent(rowsJenis);
			Label nm = new Label(j.getNama());
			nm.setStyle("font-weight:bold;");
			nm.setParent(r);
			new Label(Boolean.TRUE.equals(j.getMenggunakanAnggaran())
					? "Ya — wajib pilih Anggaran" : "Tidak — akun tetap").setParent(r);
			new Label(j.getAkun() == null ? "-" : j.getAkun().toString()).setParent(r);
			new Label(j.getSatuanKerja() == null ? "(semua)" : j.getSatuanKerja().getNama()).setParent(r);
			new Label(j.getKeterangan() == null ? "" : j.getKeterangan()).setParent(r);
			new Label(Boolean.TRUE.equals(j.getAktif()) ? "Ya" : "Tidak").setParent(r);
			Hbox aksi = new Hbox();
			aksi.setParent(r);
			if (bolehKelola) {
				Button ubah = new Button("Ubah");
				ubah.setParent(aksi);
				ubah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						bukaFormJenis(j);
					}
				});
			}
		}
		if (list.isEmpty()) {
			tulis(v, "(belum ada Jenis Reimbursement — akan dibuat otomatis saat restart, atau tambah manual)");
		}
	}

	/** Form tambah/ubah Jenis Reimbursement — memakai addWindow yang sama. */
	private void bukaFormJenis(final JenisReimbursement j) throws Exception {
		if (addWindow == null || !bolehKelolaJenis()) {
			return;
		}
		addWindow.getChildren().clear();

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setParent(addWindow);

		MyGrid f = new MyGrid();
		f.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(f);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("32%");
		c1.setParent(columns);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(columns);
		Rows rowsF = new Rows();
		rowsF.setParent(f);

		MyFormRow row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Nama *"));
		final Textbox namaJenis = new Textbox(j.getNama() == null ? "" : j.getNama());
		namaJenis.setWidth("90%");
		row.appendChild(namaJenis);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Menggunakan Anggaran"));
		final Checkbox pakaiAnggaran = new Checkbox("Pengaju wajib memilih Anggaran (Workspace)");
		pakaiAnggaran.setChecked(Boolean.TRUE.equals(j.getMenggunakanAnggaran()));
		row.appendChild(pakaiAnggaran);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Akun (wajib bila TANPA anggaran)"));
		final AmbilDataAkunBanbox akunJenis = new AmbilDataAkunBanbox(false);
		akunJenis.setWidth("90%");
		if (j.getAkun() != null) {
			akunJenis.setAttribute("akun", j.getAkun());
			akunJenis.setValue(j.getAkun().toString());
		}
		row.appendChild(akunJenis);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Satuan Kerja (opsional)"));
		final AmbilDataSatuanKerjaBanbox skJenis = new AmbilDataSatuanKerjaBanbox(true);
		skJenis.setWidth("90%");
		if (j.getSatuanKerja() != null) {
			skJenis.setAttribute("satuanKerja", j.getSatuanKerja());
			skJenis.setValue(j.getSatuanKerja().getNama());
		}
		skJenis.setDisabled(false);
		row.appendChild(skJenis);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Keterangan"));
		final Textbox ketJenis = new Textbox(j.getKeterangan() == null ? "" : j.getKeterangan());
		ketJenis.setRows(2);
		ketJenis.setWidth("90%");
		row.appendChild(ketJenis);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Aktif"));
		final Checkbox aktifJenis = new Checkbox("");
		aktifJenis.setChecked(Boolean.TRUE.equals(j.getAktif()));
		row.appendChild(aktifJenis);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("32px");
		toolbar.setParent(isi);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan Jenis", "/img/save.gif");
		simpan.setParent(toolbar);
		f.setParent(isi);

		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (namaJenis.getValue() == null || namaJenis.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama jenis wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Akun akunPilihan = akunJenis.getAttribute("akun") instanceof Akun
						? (Akun) akunJenis.getAttribute("akun") : null;
				if (!pakaiAnggaran.isChecked() && (akunPilihan == null || akunPilihan.getId() == null)) {
					MyMessageboxConfig.show(
							"Akun wajib dipilih untuk Jenis Reimbursement TANPA anggaran — akun inilah yang dipakai semua pengajuan jenis ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();
				JenisReimbursement data = j.getId() == null ? j
						: (JenisReimbursement) session.load(JenisReimbursement.class, j.getId());
				data.setNama(namaJenis.getValue().trim());
				data.setMenggunakanAnggaran(Boolean.valueOf(pakaiAnggaran.isChecked()));
				data.setAkun(akunPilihan);
				data.setSatuanKerja(skJenis.getAttribute("satuanKerja") instanceof SatuanKerja
						? (SatuanKerja) skJenis.getAttribute("satuanKerja") : null);
				data.setKeterangan(ketJenis.getValue());
				data.setAktif(Boolean.valueOf(aktifJenis.isChecked()));
				if (data.getId() == null) {
					session.save(data);
				} else {
					session.update(data);
				}
				session.flush();
				addWindow.setVisible(false);
				onJenisReimbursement(null);
			}
		});

		addWindow.setTitle(j.getId() == null ? "Tambah Jenis Reimbursement" : "Ubah Jenis Reimbursement");
		addWindow.setVisible(true);
		addWindow.doHighlighted();
	}

	// =====================================================================
	// Tab CRUD Jenis Pengeluaran (akun dipetakan admin; pegawai tinggal pilih)
	// =====================================================================

	public void onJenisPengeluaran(Event event) throws Exception {
		if (jenisPengeluaranPanel == null) {
			return;
		}
		jenisPengeluaranPanel.getChildren().clear();

		final boolean bolehKelola = bolehKelolaJenis();
		Vbox v = new Vbox();
		v.setWidth("100%");
		v.setParent(jenisPengeluaranPanel);

		if (bolehKelola) {
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("32px");
			toolbar.setParent(v);
			MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Jenis Pengeluaran", "/img/new.gif");
			tambah.setParent(toolbar);
			tambah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					bukaFormJenisPengeluaran(new JenisPengeluaran());
				}
			});
		} else {
			tulis(v, "Hanya administrator yang dapat menambah/mengubah Jenis Pengeluaran.");
		}

		Grid g = new Grid();
		g.setWidth("100%");
		g.setParent(v);
		Columns columns = new Columns();
		columns.setParent(g);
		String[] judul = new String[] { "Nama", "Akun", "Jenis Asset", "Keterangan", "Aktif", "" };
		String[] lebar = new String[] { "22%", "26%", "16%", "22%", "6%", "8%" };
		for (int i = 0; i < judul.length; i++) {
			MyColumnConfig col = new MyColumnConfig();
			col.setLabel(judul[i]);
			col.setWidth(lebar[i]);
			col.setParent(columns);
		}
		Rows rowsJp = new Rows();
		rowsJp.setParent(g);

		List list = HibernateUtil.currentSession().createCriteria(JenisPengeluaran.class)
				.addOrder(Order.asc("nama")).list();
		for (int i = 0; i < list.size(); i++) {
			final JenisPengeluaran jp = (JenisPengeluaran) list.get(i);
			Row r = new Row();
			r.setValign("top");
			r.setParent(rowsJp);
			Label nm = new Label(jp.getNama());
			nm.setStyle("font-weight:bold;");
			nm.setParent(r);
			Label ak = new Label(jp.getAkun() == null ? "(belum dipetakan)" : jp.getAkun().toString());
			if (jp.getAkun() == null) {
				ak.setStyle("color:#dc2626;");
			}
			ak.setParent(r);
			new Label(jp.getJenisAsset() == null ? "-" : jp.getJenisAsset().getNama()).setParent(r);
			new Label(jp.getKeterangan() == null ? "" : jp.getKeterangan()).setParent(r);
			new Label(Boolean.TRUE.equals(jp.getAktif()) ? "Ya" : "Tidak").setParent(r);
			Hbox aksi = new Hbox();
			aksi.setParent(r);
			if (bolehKelola) {
				Button ubah = new Button("Ubah");
				ubah.setParent(aksi);
				ubah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						bukaFormJenisPengeluaran(jp);
					}
				});
			}
		}
		if (list.isEmpty()) {
			tulis(v, "(belum ada Jenis Pengeluaran — dibuat otomatis saat restart, atau tambah manual)");
		}
	}

	/** Form tambah/ubah Jenis Pengeluaran — memakai addWindow yang sama. */
	private void bukaFormJenisPengeluaran(final JenisPengeluaran jp) throws Exception {
		if (addWindow == null || !bolehKelolaJenis()) {
			return;
		}
		addWindow.getChildren().clear();

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setParent(addWindow);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("32px");
		toolbar.setParent(isi);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan Jenis Pengeluaran", "/img/save.gif");
		simpan.setParent(toolbar);

		MyGrid f = new MyGrid();
		f.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(f);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("32%");
		c1.setParent(columns);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(columns);
		Rows rowsF = new Rows();
		rowsF.setParent(f);
		f.setParent(isi);

		MyFormRow row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Nama Jenis Pengeluaran *"));
		final Textbox namaJp = new Textbox(jp.getNama() == null ? "" : jp.getNama());
		namaJp.setWidth("90%");
		row.appendChild(namaJp);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Akun Biaya *"));
		final AmbilDataAkunBanbox akunJp = new AmbilDataAkunBanbox(false);
		akunJp.setWidth("90%");
		if (jp.getAkun() != null) {
			akunJp.setAttribute("akun", jp.getAkun());
			akunJp.setValue(jp.getAkun().toString());
		}
		row.appendChild(akunJp);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Jenis Asset (opsional)"));
		final Combobox jaCombo = new Combobox();
		jaCombo.setReadonly(true);
		jaCombo.setWidth("90%");
		org.zkoss.zul.Comboitem tanpaAsset = jaCombo.appendItem("(tanpa mapping asset)");
		tanpaAsset.setValue(null);
		jaCombo.setSelectedItem(tanpaAsset);
		try {
			List assets = HibernateUtil.currentSession()
					.createCriteria(ais.database.model.asset.JenisAsset.class).addOrder(Order.asc("nama")).list();
			for (int i = 0; i < assets.size(); i++) {
				ais.database.model.asset.JenisAsset ja = (ais.database.model.asset.JenisAsset) assets.get(i);
				org.zkoss.zul.Comboitem item = jaCombo.appendItem(ja.getNama());
				item.setValue(ja);
				if (jp.getJenisAsset() != null && jp.getJenisAsset().getId() != null
						&& jp.getJenisAsset().getId().equals(ja.getId())) {
					jaCombo.setSelectedItem(item);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.bukaFormJenisPengeluaran-asset");
		}
		row.appendChild(jaCombo);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Keterangan"));
		final Textbox ketJp = new Textbox(jp.getKeterangan() == null ? "" : jp.getKeterangan());
		ketJp.setRows(2);
		ketJp.setWidth("90%");
		row.appendChild(ketJp);

		row = new MyFormRow();
		row.setParent(rowsF);
		row.appendChild(new MyLabelConfig("Aktif"));
		final Checkbox aktifJp = new Checkbox("");
		aktifJp.setChecked(Boolean.TRUE.equals(jp.getAktif()));
		row.appendChild(aktifJp);

		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (namaJp.getValue() == null || namaJp.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama jenis pengeluaran wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();
				JenisPengeluaran data = jp.getId() == null ? jp
						: (JenisPengeluaran) session.load(JenisPengeluaran.class, jp.getId());
				data.setNama(namaJp.getValue().trim());
				data.setAkun(akunJp.getAttribute("akun") instanceof Akun ? (Akun) akunJp.getAttribute("akun") : null);
				data.setJenisAsset(jaCombo.getSelectedItem() == null ? null
						: (ais.database.model.asset.JenisAsset) jaCombo.getSelectedItem().getValue());
				data.setKeterangan(ketJp.getValue());
				data.setAktif(Boolean.valueOf(aktifJp.isChecked()));
				if (data.getId() == null) {
					session.save(data);
				} else {
					session.update(data);
				}
				session.flush();
				jenisPengeluaranList = null; // segarkan cache combo di form pengajuan
				addWindow.setVisible(false);
				onJenisPengeluaran(null);
			}
		});

		addWindow.setTitle(jp.getId() == null ? "Tambah Jenis Pengeluaran" : "Ubah Jenis Pengeluaran");
		addWindow.setVisible(true);
		addWindow.doHighlighted();
	}

	// =====================================================================
	// FormSop — form() dipanggil mesin SOP DAN halaman daftar (addWindow)
	// =====================================================================

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop dispo, MyToolbarbuttonConfig save,
			EventListener setujuiData) throws Exception {
		reimbursement = (ReimbursementPegawai) generalValueObject;
		disposisiSop = dispo;
		if (tbmuser == null) {
			tbmuser = Common.getCurrentUser();
		}
		jenisPengeluaranList = null; // muat ulang master jenis pengeluaran tiap buka form

		array = new JSONArray();
		try {
			if (reimbursement.getFormula() != null && !reimbursement.getFormula().trim().isEmpty()) {
				array = new JSONArray(reimbursement.getFormula());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.form-parseFormula");
		}

		final boolean editable = !persetujuan && !viewOnly;

		MyGrid f = new MyGrid();
		f.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(f);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("30%");
		c1.setParent(columns);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(f);

		// ---- Satuan Kerja ----
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setWidth("90%");
		if (reimbursement.getSatuanKerja() != null) {
			satuanKerja.setAttribute("satuanKerja", reimbursement.getSatuanKerja());
			satuanKerja.setValue(reimbursement.getSatuanKerja().getNama());
		}
		satuanKerja.setDisabled(!editable);
		row.appendChild(satuanKerja);

		// ---- Jenis Reimbursement (menentukan wajib-anggaran vs akun tetap) ----
		MyFormRow rowJenis = new MyFormRow();
		rowJenis.setParent(rows);
		rowJenis.appendChild(new MyLabelConfig("Jenis Reimbursement *"));
		jenisReimbursementCombo = new Combobox();
		jenisReimbursementCombo.setReadonly(true);
		jenisReimbursementCombo.setWidth("90%");
		rowJenis.appendChild(jenisReimbursementCombo);
		try {
			List jenises = HibernateUtil.currentSession().createCriteria(JenisReimbursement.class)
					.addOrder(Order.asc("id")).list();
			JenisReimbursement terpilih = reimbursement.getJenisReimbursement();
			for (int i = 0; i < jenises.size(); i++) {
				JenisReimbursement j = (JenisReimbursement) jenises.get(i);
				if (!Boolean.TRUE.equals(j.getAktif())) {
					continue;
				}
				org.zkoss.zul.Comboitem item = jenisReimbursementCombo.appendItem(j.toString());
				item.setValue(j);
				if (terpilih != null && terpilih.getId() != null && terpilih.getId().equals(j.getId())) {
					jenisReimbursementCombo.setSelectedItem(item);
				}
			}
			if (jenisReimbursementCombo.getSelectedItem() == null && jenisReimbursementCombo.getItemCount() > 0) {
				// default ikut dokumen: tanpaAnggaran lama -> jenis tanpa-anggaran; selain itu jenis ber-anggaran
				int pilih = 0;
				for (int i = 0; i < jenisReimbursementCombo.getItemCount(); i++) {
					JenisReimbursement j = (JenisReimbursement) jenisReimbursementCombo.getItemAtIndex(i).getValue();
					boolean pakaiAnggaran = Boolean.TRUE.equals(j.getMenggunakanAnggaran());
					if (Boolean.TRUE.equals(reimbursement.getTanpaAnggaran()) ? !pakaiAnggaran : pakaiAnggaran) {
						pilih = i;
						break;
					}
				}
				jenisReimbursementCombo.setSelectedIndex(pilih);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.form-jenis");
		}
		jenisReimbursementCombo.setDisabled(!editable);

		// ---- Anggaran (Pemilihan Anggaran — pola UangMuka) ----
		rowAnggaran = new MyFormRow();
		rowAnggaran.setParent(rows);
		rowAnggaran.appendChild(new MyLabelConfig("Anggaran *"));
		workspace = new AmbilDataWorkspaceBanbox(false);
		workspace.setWidth("90%");
		if (reimbursement.getWorkspace() != null) {
			workspace.setAttribute("workspace", reimbursement.getWorkspace());
			workspace.setValue(reimbursement.getWorkspace().toString());
		}
		workspace.setDisabled(!editable);
		rowAnggaran.appendChild(workspace);

		// ---- Akun tetap (dari Jenis Reimbursement tanpa-anggaran) — hanya info ----
		rowAkunInfo = new MyFormRow();
		rowAkunInfo.setParent(rows);
		rowAkunInfo.appendChild(new MyLabelConfig("Akun (dari Jenis)"));
		akunInfo = new Label("-");
		rowAkunInfo.appendChild(akunInfo);
		aturBarisAnggaran();
		jenisReimbursementCombo.addEventListener("onSelect", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				aturBarisAnggaran();
			}
		});

		// ---- Info anggaran terpilih ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Unit/Satuan Kerja"));
		unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Total Anggaran"));
		saldoAnggaran = new Label();
		row.appendChild(saldoAnggaran);
		isiInfoAnggaran();
		workspace.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Workspace w = (Workspace) workspace.getAttribute("workspace");
				if (w != null && w.getSatuanKerja() != null) {
					satuanKerja.setAttribute("satuanKerja", w.getSatuanKerja());
					satuanKerja.setValue(w.getSatuanKerja().getNama());
				}
				isiInfoAnggaran();
			}
		});

		// ---- Kode ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Kode"));
		kode = new Label(reimbursement.getKode() == null ? generateCode(false) : reimbursement.getKode());
		row.appendChild(kode);

		// ---- Judul ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Judul Pengajuan *"));
		nama = new Textbox(reimbursement.getNama() == null ? "" : reimbursement.getNama());
		nama.setWidth("90%");
		nama.setReadonly(!editable);
		row.appendChild(nama);

		// ---- Pegawai penerima ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Pegawai Penerima *"));
		pegawaiPenerima = new AmbilDataPegawaiBanbox(true);
		pegawaiPenerima.setWidth("90%");
		Pegawai p = reimbursement.getPegawai() != null ? reimbursement.getPegawai()
				: (tbmuser == null ? null : tbmuser.getPegawai());
		if (p != null) {
			pegawaiPenerima.setAttribute("pegawai", p);
			pegawaiPenerima.setValue(p.getNama());
		}
		pegawaiPenerima.setDisabled(!editable);
		row.appendChild(pegawaiPenerima);

		// ---- Tanggal pengeluaran ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tanggal Pengeluaran *"));
		tanggalKegiatan = new MyDatebox();
		tanggalKegiatan.setFormat("dd/MM/yyyy");
		tanggalKegiatan.setValue(reimbursement.getTanggalPengeluaran() == null ? WaktuUtil.getDate()
				: reimbursement.getTanggalPengeluaran());
		tanggalKegiatan.setDisabled(!editable);
		row.appendChild(tanggalKegiatan);

		// ---- Rincian barang/biaya (pola KasKecil) ----
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Rincian Barang/Biaya *"));
		Vbox wadah = new Vbox();
		wadah.setWidth("100%");
		if (editable) {
			MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
			tambah.setParent(wadah);
			tambah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					JSONObject o = new JSONObject();
					o.put("key", Math.abs(Common.randLong()));
					o.put("nama", "");
					o.put("qty", 1.0);
					o.put("harga", 0.0);
					o.put("jumlah", 0.0);
					array.put(o);
					reloadItems(editable);
				}
			});
		}
		itemBox = new Vbox();
		itemBox.setWidth("100%");
		itemBox.setParent(wadah);
		row.appendChild(wadah);

		// ---- Total ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jumlah Pengajuan"));
		footerTotal = new Label("0");
		footerTotal.setStyle("font-weight:bold;");
		row.appendChild(footerTotal);
		reloadItems(editable);

		// ---- Keterangan ----
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Keterangan"));
		keterangan = new Textbox(reimbursement.getKeterangan() == null ? "" : reimbursement.getKeterangan());
		keterangan.setRows(3);
		keterangan.setWidth("90%");
		keterangan.setReadonly(!editable);
		row.appendChild(keterangan);

		// ---- Info pengajuan/persetujuan ----
		if (reimbursement.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new MyLabelConfig("Diajukan Oleh"));
			row.appendChild(new Label((reimbursement.getDibuatOleh() == null ? "-"
					: reimbursement.getDibuatOleh().getUserNama())
					+ (reimbursement.getTanggalPengajuan() == null ? ""
							: " — " + Common.dateFormat4.get().format(reimbursement.getTanggalPengajuan()))));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new MyLabelConfig("Status"));
			Label st = new Label(reimbursement.getStatus());
			st.setStyle("font-weight:bold;");
			row.appendChild(st);

			if (reimbursement.getDisetujuiOleh() != null) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new MyLabelConfig("Disetujui Oleh"));
				row.appendChild(new Label(reimbursement.getDisetujuiOleh().getUserNama()
						+ (reimbursement.getTanggalPersetujuan() == null ? ""
								: " — " + Common.dateFormat4.get().format(reimbursement.getTanggalPersetujuan()))));
			}
		}

		if (save != null) {
			save.setLabel(persetujuan ? "Setujui dan Simpan" : "Simpan / Ajukan");
		}

		return f;
	}

	/** Daftar Jenis Pengeluaran aktif (dimuat sekali per siklus form/render). */
	private List ambilJenisPengeluaran() {
		if (jenisPengeluaranList == null) {
			try {
				jenisPengeluaranList = HibernateUtil.currentSession().createCriteria(JenisPengeluaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.addOrder(Order.asc("nama")).list();
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.ambilJenisPengeluaran");
				jenisPengeluaranList = new java.util.ArrayList();
			}
		}
		return jenisPengeluaranList;
	}

	private JenisReimbursement jenisTerpilih() {
		if (jenisReimbursementCombo == null || jenisReimbursementCombo.getSelectedItem() == null) {
			return null;
		}
		return (JenisReimbursement) jenisReimbursementCombo.getSelectedItem().getValue();
	}

	private void aturBarisAnggaran() {
		JenisReimbursement j = jenisTerpilih();
		boolean pakaiAnggaran = j == null || Boolean.TRUE.equals(j.getMenggunakanAnggaran());
		if (rowAnggaran != null) {
			rowAnggaran.setVisible(pakaiAnggaran);
		}
		if (rowAkunInfo != null) {
			rowAkunInfo.setVisible(!pakaiAnggaran);
			if (akunInfo != null) {
				akunInfo.setValue(j == null || j.getAkun() == null
						? "(admin belum menentukan akun pada Jenis Reimbursement ini)"
						: j.getAkun().toString());
			}
		}
		// prefill Satuan Kerja dari jenis (bila jenis menetapkan dan belum terisi)
		try {
			if (j != null && j.getSatuanKerja() != null && satuanKerja != null
					&& satuanKerja.getAttribute("satuanKerja") == null) {
				satuanKerja.setAttribute("satuanKerja", j.getSatuanKerja());
				satuanKerja.setValue(j.getSatuanKerja().getNama());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.aturBarisAnggaran");
		}
	}

	private void isiInfoAnggaran() {
		try {
			Workspace w = workspace == null ? null : (Workspace) workspace.getAttribute("workspace");
			if (w == null) {
				unit.setValue("-");
				saldoAnggaran.setValue("-");
				return;
			}
			unit.setValue(w.getSatuanKerja() == null ? "-" : w.getSatuanKerja().getNama());
			saldoAnggaran.setValue(Common.numberFormat.get().format(w.getHargaTotal() == null ? 0 : w.getHargaTotal()));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.isiInfoAnggaran");
		}
	}

	/** Render ulang grid rincian item dari {@link #array} (pola KasKecil, JSON formula). */
	private void reloadItems(final boolean editable) {
		if (itemBox == null) {
			return;
		}
		itemBox.getChildren().clear();

		// grid polos (BUKAN MyGrid: MyGrid nested punya bug setVisible+Timer re-hide)
		Grid g = new Grid();
		g.setWidth("100%");
		g.setParent(itemBox);
		Columns columns = new Columns();
		columns.setParent(g);
		String[] judul = new String[] { "Uraian Biaya/Barang", "Jenis Pengeluaran", "Barang/Asset (opsional)",
				"Tanggal", "Qty", "Harga", "Jumlah", "" };
		String[] lebar = new String[] { "19%", "17%", "15%", "10%", "6%", "13%", "14%", "6%" };
		for (int i = 0; i < judul.length; i++) {
			MyColumnConfig col = new MyColumnConfig();
			col.setLabel(judul[i]);
			col.setWidth(lebar[i]);
			col.setParent(columns);
		}
		Rows rows = new Rows();
		rows.setParent(g);

		for (int i = 0; i < array.length(); i++) {
			final JSONObject o = array.optJSONObject(i);
			if (o == null || o.length() == 0) {
				continue; // baris terhapus (pola KasKecil: diganti JSONObject kosong)
			}
			final int index = i;
			Row r = new Row();
			r.setParent(rows);

			final MyTextbox uraian = new MyTextbox(o.optString("nama", ""));
			uraian.setWidth("95%");
			uraian.setReadonly(!editable);
			uraian.setParent(r);

			// Pegawai cukup memilih JENIS PENGELUARAN — akun biaya sudah dipetakan admin
			// pada master Jenis Pengeluaran (tab "Jenis Pengeluaran"), tidak perlu
			// memilih kode akun yang rumit per baris.
			final Combobox jpCombo = new Combobox();
			jpCombo.setReadonly(true);
			jpCombo.setWidth("95%");
			long jpTerpilih = o.optLong("jenisPengeluaran", 0);
			try {
				for (int k = 0; k < ambilJenisPengeluaran().size(); k++) {
					JenisPengeluaran jp = (JenisPengeluaran) ambilJenisPengeluaran().get(k);
					org.zkoss.zul.Comboitem item = jpCombo.appendItem(jp.getNama()
							+ (jp.getAkun() == null ? " (akun belum dipetakan)" : ""));
					item.setValue(jp);
					if (jpTerpilih > 0 && jp.getId() != null && jp.getId().longValue() == jpTerpilih) {
						jpCombo.setSelectedItem(item);
					}
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.reloadItems-jp");
			}
			// baris lama (tersimpan langsung dengan akun, sebelum ada master jenis):
			// tampilkan info akun agar tetap terbaca meski combo tidak terpilih
			long akunId = o.optLong("akun", 0);
			if (jpTerpilih <= 0 && akunId > 0) {
				try {
					Akun a = (Akun) ConstantValues.ambil(Akun.class.getName(), Long.valueOf(akunId));
					if (a != null) {
						jpCombo.setValue(a.toString());
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.reloadItems-akunLama");
				}
			}
			jpCombo.setDisabled(!editable);
			jpCombo.setParent(r);

			// Barang/Asset OPSIONAL: isi hanya bila baris ini berupa BARANG yang akan
			// diterima lewat BAST (PenerimaanPengadaanMasterAsset) setelah disetujui —
			// baris tanpa mapping tetap sah sebagai biaya murni.
			final ais.action.master.asset.helper.AmbilDataMasterAssetBanbox barangB =
					new ais.action.master.asset.helper.AmbilDataMasterAssetBanbox(null);
			barangB.setWidth("95%");
			long barangId = o.optLong("masterAsset", 0);
			if (barangId > 0) {
				try {
					ais.database.model.asset.MasterAsset ma = (ais.database.model.asset.MasterAsset) HibernateUtil
							.currentSession().get(ais.database.model.asset.MasterAsset.class, Long.valueOf(barangId));
					if (ma != null) {
						barangB.setAttribute("masterAsset", ma);
						barangB.setValue(ma.toString());
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.reloadItems-barang");
				}
			}
			barangB.setDisabled(!editable);
			barangB.setParent(r);
			barangB.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object ma = barangB.getAttribute("masterAsset");
					if (ma instanceof ais.database.model.asset.MasterAsset) {
						o.put("masterAsset", ((ais.database.model.asset.MasterAsset) ma).getId().longValue());
					}
				}
			});

			final MyDatebox tgl = new MyDatebox();
			tgl.setFormat("dd/MM/yyyy");
			tgl.setWidth("95%");
			long t = o.optLong("tanggal", 0);
			tgl.setValue(t > 0 ? new Date(t) : WaktuUtil.getDate());
			tgl.setDisabled(!editable);
			tgl.setParent(r);

			final MyDoublebox qty = new MyDoublebox(Double.valueOf(o.optDouble("qty", 1.0)));
			qty.setWidth("95%");
			qty.setDisabled(!editable);
			qty.setParent(r);

			final MyDoublebox harga = new MyDoublebox(Double.valueOf(o.optDouble("harga", 0.0)));
			harga.setWidth("95%");
			harga.setDisabled(!editable);
			harga.setParent(r);

			final Label jumlah = new Label(Common.numberFormat.get().format(o.optDouble("jumlah", 0.0)));
			jumlah.setParent(r);

			EventListener tulisBalik = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					// jenis pengeluaran terpilih -> simpan id jenis + akun turunannya
					if (jpCombo.getSelectedItem() != null
							&& jpCombo.getSelectedItem().getValue() instanceof JenisPengeluaran) {
						JenisPengeluaran jp = (JenisPengeluaran) jpCombo.getSelectedItem().getValue();
						o.put("jenisPengeluaran", jp.getId().longValue());
						if (jp.getAkun() != null && jp.getAkun().getId() != null) {
							o.put("akun", jp.getAkun().getId().longValue());
						} else {
							o.put("akun", 0L);
						}
					}
					o.put("nama", uraian.getValue() == null ? "" : uraian.getValue().trim());
					double q = qty.getValue() == null ? 0.0 : qty.getValue().doubleValue();
					double h = harga.getValue() == null ? 0.0 : harga.getValue().doubleValue();
					o.put("qty", q);
					o.put("harga", h);
					o.put("jumlah", q * h);
					if (tgl.getValue() != null) {
						o.put("tanggal", tgl.getValue().getTime());
					}
					if (!o.has("key")) {
						o.put("key", Math.abs(Common.randLong()));
					}
					jumlah.setValue(Common.numberFormat.get().format(q * h));
					hitungTotal();
				}
			};
			uraian.addEventListener("onChange", tulisBalik);
			qty.addEventListener("onChange", tulisBalik);
			harga.addEventListener("onChange", tulisBalik);
			tgl.addEventListener("onChange", tulisBalik);
			jpCombo.addEventListener("onSelect", tulisBalik);

			if (editable) {
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/delete.gif");
				hapus.setParent(r);
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						array.put(index, new JSONObject());
						reloadItems(editable);
					}
				});
			} else {
				new Label("").setParent(r);
			}
		}
		hitungTotal();
	}

	private double hitungTotal() {
		double total = 0;
		for (int i = 0; i < array.length(); i++) {
			JSONObject o = array.optJSONObject(i);
			if (o == null || o.length() == 0) {
				continue;
			}
			total += o.optDouble("jumlah", 0.0);
		}
		if (footerTotal != null) {
			footerTotal.setValue(Common.numberFormat.get().format(total));
		}
		return total;
	}

	// =====================================================================
	// FormSop — onSave (pola persist UangMukaAction.onSave)
	// =====================================================================

	@Override
	public boolean onSave(Event event) throws Exception {
		if (reimbursement == null) {
			return false;
		}

		JenisReimbursement jenis = jenisTerpilih();
		boolean tanpa = jenis != null && !Boolean.TRUE.equals(jenis.getMenggunakanAnggaran());
		Workspace w = workspace == null ? null : (Workspace) workspace.getAttribute("workspace");
		SatuanKerja sk = satuanKerja == null ? null : (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

		if (jenis == null) {
			MyMessageboxConfig.show("Jenis Reimbursement wajib dipilih.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Judul pengajuan wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (!tanpa && (w == null || w.getId() == null)) {
			MyMessageboxConfig.show("Anggaran wajib dipilih dari daftar untuk Jenis Reimbursement \""
					+ jenis.getNama() + "\".", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanpa && (jenis.getAkun() == null || jenis.getAkun().getId() == null)) {
			MyMessageboxConfig.show("Akun pada Jenis Reimbursement \"" + jenis.getNama()
					+ "\" belum ditentukan. Mohon administrator melengkapi akun pada tab Jenis Reimbursement terlebih dahulu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		Pegawai p = pegawaiPenerima == null ? null : (Pegawai) pegawaiPenerima.getAttribute("pegawai");
		if (p == null || p.getId() == null) {
			p = tbmuser == null ? null : tbmuser.getPegawai();
		}
		if (p == null || p.getId() == null) {
			MyMessageboxConfig.show("Pegawai penerima wajib dipilih dari daftar.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalKegiatan.getValue() == null) {
			MyMessageboxConfig.show("Tanggal pengeluaran wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		// validasi rincian item (pola KasKecil): tiap baris wajib akun + jumlah
		double total = 0;
		int barisValid = 0;
		for (int i = 0; i < array.length(); i++) {
			JSONObject o = array.optJSONObject(i);
			if (o == null || o.length() == 0) {
				continue;
			}
			if (o.optLong("akun", 0) <= 0) {
				if (o.optLong("jenisPengeluaran", 0) > 0) {
					MyMessageboxConfig.show(
							"Akun untuk Jenis Pengeluaran pada rincian item belum dipetakan oleh administrator. "
									+ "Mohon admin melengkapi akun pada tab \"Jenis Pengeluaran\" terlebih dahulu.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} else {
					MyMessageboxConfig.show("Setiap baris rincian wajib memilih Jenis Pengeluaran.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
				return false;
			}
			double jml = o.optDouble("jumlah", 0.0);
			if (jml <= 0) {
				MyMessageboxConfig.show("Jumlah pada rincian item harus lebih dari 0 (isi qty dan harga).",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			total += jml;
			barisValid++;
		}
		if (barisValid == 0) {
			MyMessageboxConfig.show("Rincian barang/biaya minimal satu baris.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (reimbursement.getId() != null) {
			reimbursement = (ReimbursementPegawai) session.load(ReimbursementPegawai.class, reimbursement.getId());
		}

		if (reimbursement.getDibuatOleh() == null) {
			reimbursement.setDibuatOleh(tbmuser);
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			reimbursement.setDisposisiSop(disposisiSop);
		}
		reimbursement.setJenisReimbursement(jenis);
		reimbursement.setTanpaAnggaran(Boolean.valueOf(tanpa));
		reimbursement.setWorkspace(tanpa ? null : w);
		reimbursement.setSatuanKerja(sk != null ? sk
				: (w != null ? w.getSatuanKerja() : jenis.getSatuanKerja()));
		if (tanpa) {
			// akun tetap dari Jenis Reimbursement — pengaju tidak memilih akun per pengajuan
			reimbursement.setAkun(jenis.getAkun());
		}
		reimbursement.setNama(nama.getValue().trim());
		reimbursement.setDeskripsi(nama.getValue().trim());
		if (reimbursement.getKategori() == null || reimbursement.getKategori().trim().isEmpty()) {
			reimbursement.setKategori("Reimbursement");
		}
		reimbursement.setKeterangan(keterangan.getValue());
		reimbursement.setFormula(array.toString());
		reimbursement.setNominal(Double.valueOf(total));
		reimbursement.setTanggalPengeluaran(tanggalKegiatan.getValue());
		if (reimbursement.getTanggalPengajuan() == null) {
			reimbursement.setTanggalPengajuan(WaktuUtil.getDate());
		}
		reimbursement.setPegawai(p);
		if (reimbursement.getAtasan() == null) {
			reimbursement.setAtasan(p.getAtasanlangsung() == null ? p : p.getAtasanlangsung());
		}
		if (reimbursement.getStatus() == null || reimbursement.getStatus().trim().isEmpty()) {
			reimbursement.setStatus(ReimbursementPegawai.DIAJUKAN);
		}

		if (reimbursement.getId() != null) {
			session.update(reimbursement);
		} else {
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			reimbursement.setKode(noAgenda);
			session.save(reimbursement);
		}
		session.flush();

		// DPC: bila (sudah) DISETUJUI oleh alur SOP, masukkan ke daftar transfer.
		// DITUNDA 3,5 detik: timer mesin SOP (KunciEntityHelper FOR UPDATE NOWAIT pada
		// disposisi_sop + update dokumen ini) berjalan tepat setelah onSave — bila
		// simpanReimbursement jalan BERSAMAAN, kedua transaksi saling menunggu baris
		// yang sama -> popup "data disposisi sedang diproses pengguna lain". Jeda ini
		// membiarkan update mesin SOP selesai dulu (sekaligus mencegah FK
		// daftar_pengajuan_transfer tertimpa update detached engine). Bila tetap
		// terlewat, safety-net di renderer akan membuatkan baris DPC saat daftar dirender.
		final Long idFin = reimbursement.getId();
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (idFin == null) {
					return;
				}
				// Baca SEGAR dari DB (bukan instance in-memory): pada langkah persetujuan
				// akhir, disposisiSetuju baru ditulis mesin SOP SETELAH onSave ini — status
				// instance lama masih "Diajukan". Setelah jeda 3,5 dtk nilai di DB sudah
				// final, sehingga DPC terbentuk LANGSUNG saat persetujuan (tidak menunggu
				// daftar dirender / tombol Singkronkan).
				ReimbursementPegawai segar = (ReimbursementPegawai) HibernateUtil.currentSession()
						.get(ReimbursementPegawai.class, idFin);
				if (segar != null && ReimbursementPegawai.DISETUJUI.equals(segar.getStatus())) {
					DaftarPengajuanTransfer.simpanReimbursement(segar);
				}
			}
		}, "", false, 3500);

		return true;
	}

	private String generateCode(boolean tambah) {
		long count = 0;
		try {
			Number n = (Number) HibernateUtil.currentSession().createCriteria(ReimbursementPegawai.class)
					.setProjection(Projections.rowCount()).uniqueResult();
			count = n == null ? 0 : n.longValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.generateCode");
		}

		// PENGKODEAN KUSTOM (pola UangMuka/SeleksiVendor): admin mengatur format nomor
		// lewat layar "Nomor Surat Proses Pengadaan Barang/Jasa" (menu Aset & Pengadaan
		// - Setup) entri "018 - Reimbursement Pegawai" kolom "Ganti Format". Kompat:
		// bila belum, dicoba entri lama di Nomor Surat Alur Keuangan; terakhir fallback
		// format bawaan RMB-yyyyMM-urut.
		try {
			ais.database.model.surat.NomorSurat ns = null;
			if (ais.database.model.asset.NomorSuratAlurPengadaan.REIMBURSEMENT_PEGAWAI_DATA != null) {
				ns = ais.database.model.asset.NomorSuratAlurPengadaan.REIMBURSEMENT_PEGAWAI_DATA.getNomorSurat();
			}
			if (ns == null && ais.database.model.akunting.NomorSuratAlurKeuangan.REIMBURSEMENT_DATA != null) {
				ns = ais.database.model.akunting.NomorSuratAlurKeuangan.REIMBURSEMENT_DATA.getNomorSurat();
			}
			if (ns != null) {
				Long index = ns.getGunakanIndexUrut() ? ns.getNomorIndex() : Long.valueOf(count + 1);
				if (tambah) {
					ais.database.model.surat.NomorSurat.tambahIndexNomorSurat(ns);
				}
				String noAgenda = ns.format(index, WaktuUtil.getDate());
				return ais.action.master.KodeUnikUtil.pastikanUnik(ReimbursementPegawai.class, noAgenda);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.generateCode-kustom");
		}

		String prefix = "RMB-" + new SimpleDateFormat("yyyyMM").format(WaktuUtil.getDate()) + "-";
		return ais.action.master.KodeUnikUtil.pastikanUnik(ReimbursementPegawai.class, prefix + (count + 1));
	}

	// =====================================================================
	// FormSop — kontrak lain
	// =====================================================================

	@Override
	public String istilah() throws Exception {
		return "Pengajuan Reimbursement Pegawai";
	}

	@Override
	public DataSop ambil() throws Exception {
		return reimbursement;
	}

	@Override
	public Class ambilClass() throws Exception {
		return ReimbursementPegawai.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		ReimbursementPegawai d = (ReimbursementPegawai) generalValueObject;
		File file = File.createTempFile("reimbursement_", ".pdf");
		FileOutputStream fout = new FileOutputStream(file);
		Document doc = new Document(PageSize.A4, 28, 28, 30, 30);
		PdfWriter.getInstance(doc, fout);
		doc.open();

		Font fJudul = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
		Font fN = FontFactory.getFont(FontFactory.HELVETICA, 9);
		Font fNb = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

		Paragraph judul = new Paragraph("BUKTI PENGAJUAN REIMBURSEMENT PEGAWAI", fJudul);
		judul.setAlignment(Element.ALIGN_CENTER);
		judul.setSpacingAfter(8);
		doc.add(judul);

		PdfPTable head = new PdfPTable(new float[] { 25, 75 });
		head.setWidthPercentage(100);
		tulisHead(head, "Kode", d.getKode(), fN, fNb);
		tulisHead(head, "Judul", d.getNama() == null ? d.getDeskripsi() : d.getNama(), fN, fNb);
		tulisHead(head, "Pegawai", d.getPegawai() == null ? "-" : d.getPegawai().getNama(), fN, fNb);
		tulisHead(head, "Satuan Kerja", d.getSatuanKerja() == null ? "-" : d.getSatuanKerja().getNama(), fN, fNb);
		tulisHead(head, "Anggaran", d.getWorkspace() == null ? "-" : d.getWorkspace().getNama(), fN, fNb);
		tulisHead(head, "Tanggal Pengeluaran", d.getTanggalPengeluaran() == null ? "-"
				: Common.dateFormat4.get().format(d.getTanggalPengeluaran()), fN, fNb);
		tulisHead(head, "Status", d.getStatus(), fN, fNb);
		tulisHead(head, "Diajukan Oleh", d.getDibuatOleh() == null ? "-" : d.getDibuatOleh().getUserNama(), fN, fNb);
		if (d.getDisetujuiOleh() != null) {
			tulisHead(head, "Disetujui Oleh", d.getDisetujuiOleh().getUserNama()
					+ (d.getTanggalPersetujuan() == null ? ""
							: " (" + Common.dateFormat4.get().format(d.getTanggalPersetujuan()) + ")"), fN, fNb);
		}
		head.setSpacingAfter(10);
		doc.add(head);

		PdfPTable t = new PdfPTable(new float[] { 6, 32, 22, 8, 16, 16 });
		t.setWidthPercentage(100);
		for (int i = 0; i < 6; i++) {
			String h = new String[] { "No", "Uraian", "Jenis Pengeluaran", "Qty", "Harga", "Jumlah" }[i];
			PdfPCell c = new PdfPCell(new Paragraph(h, fNb));
			c.setBackgroundColor(new BaseColor(224, 231, 255));
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			c.setPadding(3);
			t.addCell(c);
		}

		JSONArray items = new JSONArray();
		try {
			if (d.getFormula() != null && !d.getFormula().trim().isEmpty()) {
				items = new JSONArray(d.getFormula());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.cetakData-parse");
		}
		int no = 1;
		double total = 0;
		for (int i = 0; i < items.length(); i++) {
			JSONObject o = items.optJSONObject(i);
			if (o == null || o.length() == 0) {
				continue;
			}
			String namaAkun = "-";
			try {
				long akunId = o.optLong("akun", 0);
				if (akunId > 0) {
					Akun a = (Akun) ConstantValues.ambil(Akun.class.getName(), Long.valueOf(akunId));
					if (a != null) {
						namaAkun = a.toString();
					}
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.cetakData-akun");
			}
			double jml = o.optDouble("jumlah", 0.0);
			total += jml;
			t.addCell(sel("" + (no++), fN, Element.ALIGN_CENTER));
			t.addCell(sel(o.optString("nama", "-"), fN, Element.ALIGN_LEFT));
			t.addCell(sel(namaAkun, fN, Element.ALIGN_LEFT));
			t.addCell(sel("" + o.optDouble("qty", 0.0), fN, Element.ALIGN_CENTER));
			t.addCell(sel(Common.numberFormat.get().format(o.optDouble("harga", 0.0)), fN, Element.ALIGN_RIGHT));
			t.addCell(sel(Common.numberFormat.get().format(jml), fN, Element.ALIGN_RIGHT));
		}
		PdfPCell cTot = new PdfPCell(new Paragraph("TOTAL", fNb));
		cTot.setColspan(5);
		cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cTot.setPadding(3);
		t.addCell(cTot);
		t.addCell(sel(Common.numberFormat.get().format(total), fNb, Element.ALIGN_RIGHT));
		doc.add(t);

		doc.close();
		fout.close();
		return file;
	}

	private void tulisHead(PdfPTable t, String label, String nilai, Font fN, Font fNb) {
		PdfPCell c = new PdfPCell(new Paragraph(label, fNb));
		c.setPadding(3);
		t.addCell(c);
		PdfPCell v = new PdfPCell(new Paragraph(nilai == null ? "-" : nilai, fN));
		v.setPadding(3);
		t.addCell(v);
	}

	private PdfPCell sel(String s, Font f, int align) {
		PdfPCell c = new PdfPCell(new Paragraph(s == null ? "" : s, f));
		c.setHorizontalAlignment(align);
		c.setPadding(3);
		return c;
	}

	// =====================================================================
	// Renderer daftar (klon kolom uang_muka + status DPC + safety-net)
	// =====================================================================

	/**
	 * Renderer lokal untuk layar/komponen {@link ReimbursementPegawaiAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ReimbursementPegawaiAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ReimbursementPegawaiAction
	 */
	private class ReimbursementRenderer implements RowRenderer {
		@Override
		public void render(Row row, Object value) throws Exception {
			final ReimbursementPegawai d = (ReimbursementPegawai) value;
			row.setValign("top");

			Vbox v = new Vbox();
			v.setParent(row);
			Label k = new Label(d.getKode());
			k.setStyle("font-weight:bold;");
			k.setParent(v);
			new Label(d.getNama() == null ? d.getDeskripsi() : d.getNama()).setParent(v);

			new Label("Rp " + Common.numberFormat.get().format(d.getNominal())).setParent(row);

			new Label(d.getTanggalPengeluaran() == null ? "-"
					: Common.dateFormat4.get().format(d.getTanggalPengeluaran())).setParent(row);

			Vbox aju = new Vbox();
			aju.setParent(row);
			new Label(d.getDibuatOleh() == null ? "-" : d.getDibuatOleh().getUserNama()).setParent(aju);
			if (d.getTanggalPengajuan() != null) {
				new Label(Common.dateFormat4.get().format(d.getTanggalPengajuan())).setParent(aju);
			}

			Vbox setuju = new Vbox();
			setuju.setParent(row);
			String st = d.getStatus();
			Label lst = new Label(st);
			if (ReimbursementPegawai.DISETUJUI.equals(st)) {
				lst.setStyle("color:#059669; font-weight:bold;");
			} else if (ReimbursementPegawai.DITOLAK.equals(st)) {
				lst.setStyle("color:#dc2626; font-weight:bold;");
			} else {
				lst.setStyle("color:#b45309; font-weight:bold;");
			}
			lst.setParent(setuju);
			if (d.getDisetujuiOleh() != null) {
				new Label(d.getDisetujuiOleh().getUserNama()).setParent(setuju);
				if (d.getTanggalPersetujuan() != null) {
					new Label(Common.dateFormat4.get().format(d.getTanggalPersetujuan())).setParent(setuju);
				}
			}

			new Label(d.getKeterangan() == null ? "" : d.getKeterangan()).setParent(row);

			// status DPC + safety-net (pola renderer UangMukaAction): bila sudah
			// disetujui tetapi belum punya baris DPC, buat lewat timer agar
			// persetujuan yang terjadi di mesin SOP tetap masuk daftar transfer.
			Vbox dpc = new Vbox();
			dpc.setParent(row);

			// Link WORKFLOW/PENGAJUAN SOP (pola UangMuka): buka riwayat disposisi
			// pengajuan ini langsung dari daftar.
			if (d.getDisposisiSop() != null && d.getDisposisiSop().getId() != null) {
				org.zkoss.zul.A linkSop = new org.zkoss.zul.A("Workflow/Pengajuan SOP");
				linkSop.setStyle("font-size:9px;");
				try {
					if (d.getDisposisiSop().getSop() != null) {
						linkSop.setTooltiptext("SOP " + d.getDisposisiSop().getSop().getNama());
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementRenderer-linkSop");
				}
				linkSop.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						ais.action.master.sop.TampilanAlurSopAction.prosess(d.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
				linkSop.setParent(dpc);
			}

			if (d.getDaftarPengajuanTransfer() != null) {
				DaftarPengajuanTransfer.tampilStatus(d.getDaftarPengajuanTransfer(), dpc);
			} else if (d.getDisetujuiOleh() != null) {
				new Label("Menunggu masuk daftar DPC...").setParent(dpc);
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						DaftarPengajuanTransfer.simpanReimbursement(d);
					}
				});
			} else {
				new Label("-").setParent(dpc);
			}

			new Label(Boolean.TRUE.equals(d.getAktif()) ? "Ya" : "Tidak").setParent(row);

			// Tombol baris SERAGAM dengan UangMuka: ikon standar Ubah/Hapus dari
			// Common.copyEditDeleteButtons (Hapus kini MUNCUL untuk pengajuan yang
			// masih bisa dihapus) + ikon Cetak print.png + ikon Lihat.
			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			boolean bolehUbahHapus = !ReimbursementPegawai.DISETUJUI.equals(st)
					&& !ReimbursementPegawai.LUNAS.equals(st);
			Hbox aksi = Common.copyEditDeleteButtons(bolehUbahHapus, false, bolehUbahHapus, d,
					ReimbursementPegawaiAction.this);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(aksi));

			MyToolbarbuttonConfig lihat = new MyToolbarbuttonConfig("", "/img/search.gif");
			lihat.setTooltiptext("Lihat Rincian");
			lihat.setOrient("vertical");
			lihat.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					persetujuan = false;
					bukaForm(d, d.getDisposisiSop(), true);
				}
			});
			aksiButtons.add(lihat);

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("", "/img/print.png");
			cetak.setTooltiptext("Cetak");
			cetak.setOrient("vertical");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					File f = cetakData(d);
					if (f != null) {
						Filedownload.save(f, "application/pdf");
					}
				}
			});
			aksiButtons.add(cetak);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
		}
	}
}
