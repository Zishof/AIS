package ais.action.master.ticket;

import java.io.File;
import java.util.Date;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.ticket.Ticket;
import ais.database.model.ticket.TicketKategori;
import ais.ui.util.FormSop;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>Jembatan SOP untuk {@link Ticket} — implementasi {@link FormSop}</h3>
 *
 * <p>Membuat modul Ticketing dapat DIAJUKAN lewat engine SOP/disposisi generik
 * ({@code DisposisiSopAction}, menu "Pengajuan SOP (Workflow)"), sama seperti pengajuan pengadaan/
 * surat. Engine yang membuat {@code DisposisiSop} &amp; langkah alur; kelas ini pasif — hanya
 * menyediakan form data tiket dan menyimpannya. Setelah tersimpan, engine memanggil
 * {@code ticket.setDisposisiSop(...)} (lihat {@code DisposisiSopAction} baris ~1808).</p>
 *
 * <p><b>Aktivasi (konfigurasi data, bukan kode):</b> admin membuat satu {@code Sop} "Pengajuan
 * Ticket", dengan langkah {@code AlurSop start=true} yang {@code formInputan}-nya diisi nama FQ
 * kelas ini ({@code ais.action.master.ticket.TicketFormSop}). Kelas otomatis muncul di daftar
 * pilihan form ({@code InitDataHelper} reflection {@code getSubTypesOf(FormSop.class)}) sesudah
 * restart.</p>
 *
 * <p><b>PENTING:</b> konstruktor no-arg WAJIB ringan &amp; tak melempar — dipanggil massal saat
 * startup ({@code c.newInstance()}) untuk membangun daftar {@code istilah()}. Java 1.7.</p>
 */
public class TicketFormSop implements FormSop {

	private Ticket ticket;
	private DisposisiSop disposisiSop;
	private boolean persetujuan;

	// kontrol input (dibaca ulang di onSave)
	private Textbox judul;
	private Textbox deskripsi;
	private Textbox modul;
	private Combobox tipe;
	private Combobox prioritas;
	private Combobox kategori;
	private Combobox satuanKerja;

	public TicketFormSop() {
	}

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujui) throws Exception {
		this.ticket = (generalValueObject instanceof Ticket) ? (Ticket) generalValueObject : new Ticket();
		// pertahankan disposisiSop yang sudah ada bila engine mengirim null/kosong
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		if (this.ticket.getDisposisiSop() != null) {
			this.persetujuan = true;
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		new Column("", null, "180px").setParent(columns);
		new Column().setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		judul = new Textbox(ticket.getJudul());
		judul.setWidth("95%");
		baris(rows, "Judul *", judul);

		deskripsi = new Textbox(ticket.getDeskripsi());
		deskripsi.setMultiline(true);
		deskripsi.setRows(4);
		deskripsi.setWidth("95%");
		baris(rows, "Deskripsi", deskripsi);

		tipe = new Combobox();
		tipe.setReadonly(true);
		isiCombo(tipe, new String[] { Ticket.TIPE_KENDALA, Ticket.TIPE_PERMINTAAN, Ticket.TIPE_PROGRESS,
				Ticket.TIPE_INTERAKSI, Ticket.TIPE_LAINNYA }, ticket.getTipe(), Ticket.TIPE_KENDALA);
		baris(rows, "Tipe", tipe);

		prioritas = new Combobox();
		prioritas.setReadonly(true);
		isiCombo(prioritas, new String[] { Ticket.PRIORITAS_RENDAH, Ticket.PRIORITAS_SEDANG, Ticket.PRIORITAS_TINGGI,
				Ticket.PRIORITAS_KRITIS }, ticket.getPrioritas(), Ticket.PRIORITAS_SEDANG);
		baris(rows, "Prioritas", prioritas);

		kategori = new Combobox();
		kategori.setReadonly(true);
		try {
			Common.insertComboDanSemua(kategori, "nama", TicketKategori.class);
			if (ticket.getTicketKategori() != null) {
				pilihByValue(kategori, ticket.getTicketKategori());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketFormSop.form-kategori");
		}
		baris(rows, "Kategori", kategori);

		modul = new Textbox(ticket.getModul());
		modul.setWidth("95%");
		baris(rows, "Modul terkait", modul);

		satuanKerja = new Combobox();
		satuanKerja.setReadonly(true);
		try {
			Common.insertComboDanSemua(satuanKerja, "nama", SatuanKerja.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketFormSop.form-sk");
		}
		baris(rows, "Satuan Kerja tujuan", satuanKerja);

		return grid;
	}

	@Override
	public boolean onSave(Event event) throws Exception {
		if (judul == null || judul.getValue() == null || judul.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Judul tiket wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (ticket == null) {
			ticket = new Ticket();
		}
		ticket.setJudul(judul.getValue().trim());
		ticket.setDeskripsi(deskripsi == null ? null : deskripsi.getValue());
		ticket.setTipe(nilai(tipe));
		ticket.setPrioritas(nilai(prioritas));
		ticket.setModul(modul == null ? null : modul.getValue());
		if (kategori != null && kategori.getSelectedItem() != null && kategori.getSelectedItem().getValue() != null) {
			ticket.setTicketKategori((TicketKategori) kategori.getSelectedItem().getValue());
		}
		if (satuanKerja != null && satuanKerja.getSelectedItem() != null
				&& satuanKerja.getSelectedItem().getValue() != null) {
			ticket.setSatuanKerja((SatuanKerja) satuanKerja.getSelectedItem().getValue());
		}
		if (ticket.getStatus() == null) {
			ticket.setStatus(Ticket.STATUS_BARU);
		}
		if (ticket.getProgress() == null) {
			ticket.setProgress(0);
		}
		if (ticket.getAktif() == null) {
			ticket.setAktif(true);
		}
		if (ticket.getTanggalDibuat() == null) {
			ticket.setTanggalDibuat(new Date());
		}

		Tbmuser pengaju = Common.getCurrentUser();
		if (pengaju != null && ticket.getPengajuUserId() == null) {
			ticket.setPengajuUserId(pengaju.getUserId());
			ticket.setPengajuNama(pengaju.getUserNama());
			ticket.setPengajuTipe(TicketingAction.tipePengguna(pengaju));
			try {
				if (pengaju.hakAkses() != null && pengaju.hakAkses().getRoleId() != null) {
					ticket.setHakAksesTarget("," + pengaju.hakAkses().getRoleId() + ",");
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketFormSop.onSave-role");
			}
		}

		// Tautkan disposisi bila engine sudah menyediakannya (engine juga menautkan ulang setelahnya).
		if (disposisiSop != null && disposisiSop.getId() != null) {
			ticket.setDisposisiSop(disposisiSop);
		}

		boolean baru = ticket.getId() == null;
		Common.refreshSaveOrUpdate(session, ticket);
		if (ticket.getId() != null && (ticket.getNomorTiket() == null || ticket.getNomorTiket().trim().isEmpty())) {
			ticket.setNomorTiket("TKT-" + ticket.getId());
			Common.refreshUpdate(session, ticket);
		}
		if (baru) {
			TicketNotifikasi.tiketBaru(ticket);
		}
		return true;
	}

	@Override
	public String istilah() throws Exception {
		return "Pengajuan Ticket / Kendala Sistem";
	}

	@Override
	public DataSop ambil() throws Exception {
		return ticket;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return Ticket.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		Ticket t = (generalValueObject instanceof Ticket) ? (Ticket) generalValueObject : this.ticket;
		if (t == null) {
			return null;
		}
		return TicketPdf.cetak(t);
	}

	// ---- util kecil ----

	private static void baris(Rows rows, String label, org.zkoss.zk.ui.Component field) {
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(label));
		row.appendChild(field);
	}

	private static void isiCombo(Combobox combo, String[] nilai, String terpilih, String defaultNilai) {
		combo.getChildren().clear();
		int idxDefault = 0;
		for (int i = 0; i < nilai.length; i++) {
			Comboitem ci = new Comboitem(nilai[i]);
			ci.setValue(nilai[i]);
			ci.setParent(combo);
			if (nilai[i].equals(defaultNilai)) {
				idxDefault = i;
			}
			if (terpilih != null && terpilih.equals(nilai[i])) {
				combo.setSelectedItem(ci);
			}
		}
		if (combo.getSelectedItem() == null && combo.getItemCount() > 0) {
			combo.setSelectedIndex(idxDefault);
		}
	}

	private static void pilihByValue(Combobox combo, Object value) {
		if (value == null) {
			return;
		}
		for (Object o : combo.getItems()) {
			Comboitem ci = (Comboitem) o;
			if (value.equals(ci.getValue())) {
				combo.setSelectedItem(ci);
				return;
			}
		}
	}

	private static String nilai(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null) {
			return null;
		}
		Object v = combo.getSelectedItem().getValue();
		return v == null ? combo.getSelectedItem().getLabel() : v.toString();
	}
}
