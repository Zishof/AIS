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
public class ReimbursementPegawaiAction extends GenericAutowireComposer implements FormSop {

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
	private Checkbox tanpaAnggaran;
	private AmbilDataAkunBanbox akunTanpa;
	private MyFormRow rowAnggaran;
	private MyFormRow rowAkunPilih;
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

	public void onSearchDefault(Event event) throws Exception {
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

		// ---- Tanpa anggaran (opsional, ikut konfigurasi seperti uang muka) ----
		tanpaAnggaran = new Checkbox("Merupakan tanpa anggaran");
		tanpaAnggaran.setChecked(Boolean.TRUE.equals(reimbursement.getTanpaAnggaran()));
		MyFormRow rowTanpa = new MyFormRow();
		rowTanpa.setParent(rows);
		rowTanpa.appendChild(new MyLabelConfig(""));
		rowTanpa.appendChild(tanpaAnggaran);
		rowTanpa.setVisible(Common.bolehKonfigurasi("tampilkan_tanpa_anggaran"));
		tanpaAnggaran.setDisabled(!editable);

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

		// ---- Akun manual saat tanpa anggaran ----
		rowAkunPilih = new MyFormRow();
		rowAkunPilih.setParent(rows);
		rowAkunPilih.appendChild(new MyLabelConfig("Akun *"));
		akunTanpa = new AmbilDataAkunBanbox(false);
		akunTanpa.setWidth("90%");
		if (reimbursement.getAkun() != null) {
			akunTanpa.setAttribute("akun", reimbursement.getAkun());
			akunTanpa.setValue(reimbursement.getAkun().toString());
		}
		akunTanpa.setDisabled(!editable);
		rowAkunPilih.appendChild(akunTanpa);
		aturBarisAnggaran();
		tanpaAnggaran.addEventListener("onCheck", new EventListener() {
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

	private void aturBarisAnggaran() {
		boolean tanpa = tanpaAnggaran != null && tanpaAnggaran.isChecked();
		if (rowAnggaran != null) {
			rowAnggaran.setVisible(!tanpa);
		}
		if (rowAkunPilih != null) {
			rowAkunPilih.setVisible(tanpa);
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
		String[] judul = new String[] { "Uraian Biaya/Barang", "Akun", "Tanggal", "Qty", "Harga", "Jumlah", "" };
		String[] lebar = new String[] { "24%", "22%", "13%", "8%", "14%", "14%", "5%" };
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

			final AmbilDataAkunBanbox akunB = new AmbilDataAkunBanbox(false);
			akunB.setWidth("95%");
			long akunId = o.optLong("akun", 0);
			if (akunId > 0) {
				try {
					Akun a = (Akun) ConstantValues.ambil(Akun.class.getName(), Long.valueOf(akunId));
					if (a != null) {
						akunB.setAttribute("akun", a);
						akunB.setValue(a.toString());
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.reloadItems-akun");
				}
			}
			akunB.setDisabled(!editable);
			akunB.setParent(r);

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
					Object data = arg0 == null ? null : arg0.getData();
					if (data instanceof Akun) {
						o.put("akun", ((Akun) data).getId().longValue());
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
			akunB.setEventListener(tulisBalik);

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

		boolean tanpa = tanpaAnggaran != null && tanpaAnggaran.isChecked();
		Workspace w = workspace == null ? null : (Workspace) workspace.getAttribute("workspace");
		SatuanKerja sk = satuanKerja == null ? null : (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

		if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Judul pengajuan wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (!tanpa && (w == null || w.getId() == null)) {
			MyMessageboxConfig.show("Anggaran wajib dipilih dari daftar.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanpa && !(akunTanpa.getAttribute("akun") instanceof Akun)) {
			MyMessageboxConfig.show("Akun wajib dipilih ketika tanpa anggaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
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
				MyMessageboxConfig.show("Setiap baris rincian wajib memilih Akun biaya/barang.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
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
		reimbursement.setTanpaAnggaran(Boolean.valueOf(tanpa));
		reimbursement.setWorkspace(tanpa ? null : w);
		reimbursement.setSatuanKerja(sk != null ? sk : (w == null ? null : w.getSatuanKerja()));
		if (tanpa) {
			reimbursement.setAkun((Akun) akunTanpa.getAttribute("akun"));
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

		// DPC: bila (sudah) DISETUJUI oleh alur SOP, masukkan ke daftar transfer
		final ReimbursementPegawai fin = reimbursement;
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (ReimbursementPegawai.DISETUJUI.equals(fin.getStatus())) {
					DaftarPengajuanTransfer.simpanReimbursement(fin);
				}
			}
		});

		return true;
	}

	private String generateCode(boolean tambah) {
		String prefix = "RMB-" + new SimpleDateFormat("yyyyMM").format(WaktuUtil.getDate()) + "-";
		long count = 0;
		try {
			Number n = (Number) HibernateUtil.currentSession().createCriteria(ReimbursementPegawai.class)
					.setProjection(Projections.rowCount()).uniqueResult();
			count = n == null ? 0 : n.longValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawaiAction.generateCode");
		}
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
			String h = new String[] { "No", "Uraian", "Akun", "Qty", "Harga", "Jumlah" }[i];
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

			Hbox aksi = new Hbox();
			aksi.setParent(row);
			Button lihat = new Button("Lihat");
			lihat.setParent(aksi);
			lihat.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					persetujuan = false;
					bukaForm(d, d.getDisposisiSop(), true);
				}
			});
			if (ReimbursementPegawai.DIAJUKAN.equals(st) && d.getDisposisiSop() == null) {
				Button ubah = new Button("Ubah");
				ubah.setParent(aksi);
				ubah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						persetujuan = false;
						bukaForm(d, null, false);
					}
				});
			}
			Button cetak = new Button("Cetak");
			cetak.setParent(aksi);
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					File f = cetakData(d);
					if (f != null) {
						Filedownload.save(f, "application/pdf");
					}
				}
			});
		}
	}
}
