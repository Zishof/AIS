package ais.action.master.library;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.library.helper.AmbilDataPenyediaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Penyedia;
import ais.database.model.library.SurveyVendor;
import ais.database.model.library.SurveyVendorKriteria;
import ais.database.model.library.SurveyVendorPengguna;
import ais.database.model.library.SurveyVendorPenilaian;
import ais.database.model.library.SurveyVendorPenilaianDetail;
import ais.database.model.library.SurveyVendorVendor;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>Survey Pemilihan Penilaian Vendor</h3> — angket penilaian vendor multi-pengguna (pra-pembelian).
 *
 * <p>Tahap 1-2 (Setup): staf pengadaan buat survey + vendor + kriteria(bobot configurable) + assign
 * penilai (per individu) + notifikasi. Tahap 3 (Penilaian Saya): tiap penilai mengisi skor 1..5 per
 * kriteria per vendor, dengan revisi. Tahap 4 (Dashboard): agregasi rata-rata lintas penilai, skor
 * tertimbang, ranking, pemenang otomatis vs vendor terpilih staf (audit trail) + penilaian akhir.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SurveyVendorAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 7720145511009000001L;

	private static final String[][] KRITERIA_DEFAULT = {
			{ "Kesesuaian Harga", "Apakah harga paling kompetitif dibanding vendor lain?" },
			{ "Spesifikasi & Kualitas Penawaran", "Apakah spesifikasi barang/jasa sesuai kebutuhan?" },
			{ "Ketersediaan Stok / Kapasitas", "Apakah vendor mampu menyediakan sesuai jumlah & waktu?" },
			{ "Kejelasan Penawaran", "Apakah penawaran tertulis jelas, lengkap, dan detail?" },
			{ "Legalitas Vendor", "Apakah vendor memiliki dokumen usaha lengkap?" },
			{ "Pengalaman Vendor", "Apakah vendor berpengalaman dalam bidang terkait?" },
			{ "Responsif & Komunikatif", "Seberapa cepat vendor merespons permintaan?" },
			{ "Metode Pembayaran", "Apakah syarat pembayaran fleksibel?" },
			{ "Reputasi", "Apakah vendor memiliki riwayat baik?" } };

	private MyWindow window;
	private Tabbox tabbox;
	private Tab tabPenilaian;
	private Div panelSetup;
	private Div panelPenilaian;
	private Div panelDashboard;

	private Tbmuser user;
	private boolean isStaf;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		user = Common.getCurrentUser();
		isStaf = isAdministrator() || CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		buildSetup();
		buildPenilaian();
		buildDashboard();
	}

	private boolean isAdministrator() {
		try {
			if (user == null) {
				return false;
			}
			java.util.Set roles = user.ambilRolesId();
			return roles != null && roles.contains(ais.database.model.Tbmrole.ADMINISTRATOR);
		} catch (Exception e) {
			return false;
		}
	}

	private Label lbl(Component parent, String text, String style) {
		Label l = new Label(text);
		if (style != null) {
			l.setStyle(style);
		}
		l.setParent(parent);
		return l;
	}

	// ==================================================================================
	// TAB 1 — DAFTAR SURVEY (Setup, Tahap 1-2)
	// ==================================================================================

	private void buildSetup() {
		Common.clear(panelSetup);
		if (isStaf) {
			Button tambah = new Button("+ Survey Baru");
			tambah.setStyle("background:#4f46e5;color:#fff;border:none;padding:6px 14px;border-radius:6px;cursor:pointer;font-weight:bold;");
			tambah.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception { formSurvey(null); }
			});
			tambah.setParent(panelSetup);
		}
		Grid g = grid(panelSetup);
		kolom(g, "Kode", "12%", "Judul", "24%", "Jenis", "14%", "Tanggal", "10%", "Status", "10%",
				"Vendor", "6%", "Penilai", "6%", "Aksi", "18%");
		Rows rs = new Rows();
		rs.setParent(g);
		for (SurveyVendor sv : daftarSurvey()) {
			final SurveyVendor fsv = sv;
			Row r = new Row();
			r.setParent(rs);
			new Label(nvl(sv.getKode())).setParent(r);
			new Label(nvl(sv.getJudul())).setParent(r);
			new Label(nvl(sv.getJenisBarangJasa())).setParent(r);
			new Label(sv.getTanggal() == null ? "-" : Common.dateFormat3.get().format(sv.getTanggal())).setParent(r);
			Label st = new Label(sv.getStatus());
			st.setStyle(SurveyVendor.AKTIF.equals(sv.getStatus()) ? "color:#059669;font-weight:bold;"
					: SurveyVendor.SELESAI.equals(sv.getStatus()) ? "color:#2563eb;font-weight:bold;" : "color:#6b7280;");
			st.setParent(r);
			new Label("" + hitung(SurveyVendorVendor.class, "surveyVendor", sv)).setParent(r);
			new Label("" + hitung(SurveyVendorPengguna.class, "surveyVendor", sv)).setParent(r);
			Hbox aksi = new Hbox();
			aksi.setParent(r);
			if (isStaf) {
				tombol(aksi, "Edit", "#2563eb", new EventListener() {
					public void onEvent(Event e) throws Exception { formSurvey(fsv); }
				});
				if (!SurveyVendor.AKTIF.equals(sv.getStatus())) {
					tombol(aksi, "Aktifkan+Notif", "#059669", new EventListener() {
						public void onEvent(Event e) throws Exception { aktifkan(fsv); }
					});
				}
				tombol(aksi, "Hapus", "#dc2626", new EventListener() {
					public void onEvent(Event e) throws Exception { hapusSurvey(fsv); }
				});
			} else {
				new Label("(lihat)").setParent(aksi);
			}
		}
	}

	private List<SurveyVendor> daftarSurvey() {
		Session s = HibernateUtil.currentSession();
		if (isStaf) {
			return s.createCriteria(SurveyVendor.class).addOrder(Order.desc("id")).list();
		}
		// non-staf: hanya survey yang dia jadi pengguna-nya
		List<SurveyVendorPengguna> ps = s.createCriteria(SurveyVendorPengguna.class)
				.add(Restrictions.eq("pengguna", user)).list();
		List<SurveyVendor> hasil = new ArrayList<SurveyVendor>();
		for (SurveyVendorPengguna p : ps) {
			if (p.getSurveyVendor() != null && !hasil.contains(p.getSurveyVendor())) {
				hasil.add(p.getSurveyVendor());
			}
		}
		return hasil;
	}

	// ---- Form Survey (create/edit) ----

	private void formSurvey(final SurveyVendor existing) throws Exception {
		final boolean baru = existing == null;
		final SurveyVendor sv = baru ? new SurveyVendor()
				: (SurveyVendor) HibernateUtil.currentSession().get(SurveyVendor.class, existing.getId());

		final MyWindow w = popup(baru ? "Survey Baru" : "Ubah Survey: " + nvl(sv.getKode()), "860px");
		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;max-height:74vh;overflow:auto;");
		isi.setParent(w);

		Grid head = grid(isi);
		kolom(head, "", "24%", "", "76%");
		Rows hr = new Rows();
		hr.setParent(head);
		final MyTextbox judul = tbRow(hr, "Judul Survey *", sv.getJudul(), false);
		final MyTextbox jenis = tbRow(hr, "Jenis Barang/Jasa", sv.getJenisBarangJasa(), false);
		final MyDatebox tanggal = new MyDatebox();
		tanggal.setValue(sv.getTanggal() == null ? WaktuUtil.getDate() : sv.getTanggal());
		labelRow(hr, "Tanggal", tanggal);
		final MyTextbox keterangan = tbRow(hr, "Keterangan", sv.getKeterangan(), true);
		final org.zkoss.zul.Checkbox qual = new org.zkoss.zul.Checkbox("Pakai gerbang Qualification (lulus/gagal) sebelum scoring");
		qual.setChecked(sv.getPakaiQualification());
		labelRow(hr, "Opsi", qual);

		// ---- Vendor (3 baris) ----
		lbl(isi, "A. Data Vendor", "font-weight:bold;color:#4f46e5;margin-top:8px;");
		final List<SurveyVendorVendor> vendorRef = muatVendor(sv);
		Grid gv = grid(isi);
		kolom(gv, "#", "5%", "Penyedia (master)", "27%", "Nama Vendor", "22%", "Alamat/Kontak", "22%", "Jenis B/J", "12%", "PIC", "12%");
		final Rows vr = new Rows();
		vr.setParent(gv);
		final AmbilDataPenyediaBanbox[] vPeny = new AmbilDataPenyediaBanbox[3];
		final MyTextbox[] vNama = new MyTextbox[3];
		final MyTextbox[] vAlamat = new MyTextbox[3];
		final MyTextbox[] vJenis = new MyTextbox[3];
		final MyTextbox[] vPic = new MyTextbox[3];
		for (int i = 0; i < 3; i++) {
			SurveyVendorVendor d = i < vendorRef.size() ? vendorRef.get(i) : null;
			Row r = new Row();
			r.setParent(vr);
			new Label("" + (i + 1)).setParent(r);
			vPeny[i] = new AmbilDataPenyediaBanbox();
			vPeny[i].setWidth("95%");
			if (d != null && d.getPenyedia() != null) {
				vPeny[i].setAttribute("penyedia", d.getPenyedia());
				vPeny[i].setValue(d.getPenyedia().getNama());
			}
			vPeny[i].setParent(r);
			vNama[i] = tb(d == null ? null : d.getNamaVendor());
			vNama[i].setParent(r);
			vAlamat[i] = tb(d == null ? null : d.getAlamatKontak());
			vAlamat[i].setParent(r);
			vJenis[i] = tb(d == null ? null : d.getJenisBarangJasa());
			vJenis[i].setParent(r);
			vPic[i] = tb(d == null ? null : d.getPicVendor());
			vPic[i].setParent(r);
		}

		// ---- Kriteria (configurable, seed 9 default) ----
		lbl(isi, "B. Kriteria & Bobot (bisa diubah/tambah)", "font-weight:bold;color:#4f46e5;margin-top:8px;");
		final Grid gk = grid(isi);
		kolom(gk, "Kriteria", "26%", "Pertanyaan Panduan", "54%", "Bobot %", "12%", "", "8%");
		final Rows kr = new Rows();
		kr.setParent(gk);
		List<SurveyVendorKriteria> kritRef = muatKriteria(sv);
		if (kritRef.isEmpty()) {
			for (String[] kd : KRITERIA_DEFAULT) {
				SurveyVendorKriteria k = new SurveyVendorKriteria();
				k.setNama(kd[0]);
				k.setPertanyaan(kd[1]);
				k.setBobot(Math.round((100.0 / KRITERIA_DEFAULT.length) * 100.0) / 100.0);
				kritRef.add(k);
			}
		}
		for (SurveyVendorKriteria k : kritRef) {
			barisKriteria(kr, k);
		}
		Button addK = new Button("+ Kriteria");
		addK.setStyle("margin-top:4px;");
		addK.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception { barisKriteria(kr, new SurveyVendorKriteria()); }
		});
		addK.setParent(isi);

		// ---- Penilai (assign per individu) ----
		lbl(isi, "C. Penilai (pengguna yang menilai)", "font-weight:bold;color:#4f46e5;margin-top:8px;");
		final Grid gp = grid(isi);
		kolom(gp, "Nama Pengguna", "50%", "Peran", "26%", "Lihat Semua", "12%", "", "12%");
		final Rows pr = new Rows();
		pr.setParent(gp);
		for (SurveyVendorPengguna p : muatPengguna(sv)) {
			barisPenilai(pr, p);
		}
		Button addP = new Button("+ Penilai");
		addP.setStyle("margin-top:4px;");
		addP.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception { barisPenilai(pr, new SurveyVendorPengguna()); }
		});
		addP.setParent(isi);

		// ---- Simpan ----
		Hbox foot = new Hbox();
		foot.setStyle("margin-top:12px;");
		foot.setParent(isi);
		tombol(foot, "Simpan", "#059669", new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (isBlank(judul.getValue())) {
					warn("Judul survey wajib diisi.");
					return;
				}
				Session s = HibernateUtil.currentSession();
				Transaction tx = s.beginTransaction();
				try {
					if (sv.getDibuatOleh() == null) {
						sv.setDibuatOleh(user);
						sv.setTanggalPembuatan(new Date());
						sv.setStatus(SurveyVendor.DRAFT);
					}
					sv.setJudul(judul.getValue());
					sv.setJenisBarangJasa(jenis.getValue());
					sv.setTanggal(tanggal.getValue());
					sv.setKeterangan(keterangan.getValue());
					sv.setPakaiQualification(qual.isChecked());
					if (sv.getId() == null) {
						sv.setKode("SVY-" + WaktuUtil.getDate().getTime());
						s.save(sv);
					} else {
						s.update(sv);
					}
					s.flush();
					// vendor
					for (int i = 0; i < 3; i++) {
						SurveyVendorVendor d = i < vendorRef.size() ? vendorRef.get(i) : new SurveyVendorVendor();
						Penyedia p = (Penyedia) vPeny[i].getAttribute("penyedia");
						boolean kosong = p == null && isBlank(vNama[i].getValue());
						if (kosong) {
							if (d.getId() != null) {
								s.delete(d);
							}
							continue;
						}
						d.setSurveyVendor(sv);
						d.setUrutan(i + 1);
						d.setPenyedia(p);
						d.setNamaVendor(vNama[i].getValue());
						d.setAlamatKontak(vAlamat[i].getValue());
						d.setJenisBarangJasa(vJenis[i].getValue());
						d.setPicVendor(vPic[i].getValue());
						s.saveOrUpdate(d);
					}
					// kriteria
					int urut = 1;
					for (Object o : kr.getChildren()) {
						Row r = (Row) o;
						SurveyVendorKriteria k = (SurveyVendorKriteria) r.getAttribute("k");
						MyTextbox nama = (MyTextbox) r.getAttribute("nama");
						MyTextbox pert = (MyTextbox) r.getAttribute("pert");
						MyDoublebox bobot = (MyDoublebox) r.getAttribute("bobot");
						if (isBlank(nama.getValue())) {
							if (k.getId() != null) {
								s.delete(k);
							}
							continue;
						}
						k.setSurveyVendor(sv);
						k.setUrutan(urut++);
						k.setNama(nama.getValue());
						k.setPertanyaan(pert.getValue());
						k.setBobot(bobot.getValue() == null ? 0.0 : bobot.getValue());
						k.setAktif(true);
						s.saveOrUpdate(k);
					}
					// penilai
					for (Object o : pr.getChildren()) {
						Row r = (Row) o;
						SurveyVendorPengguna p = (SurveyVendorPengguna) r.getAttribute("p");
						AmbilDataTbmuserBanbox box = (AmbilDataTbmuserBanbox) r.getAttribute("user");
						Combobox peran = (Combobox) r.getAttribute("peran");
						org.zkoss.zul.Checkbox ls = (org.zkoss.zul.Checkbox) r.getAttribute("ls");
						Tbmuser tu = (Tbmuser) box.getAttribute("tbmuser");
						if (tu == null) {
							if (p.getId() != null) {
								s.delete(p);
							}
							continue;
						}
						p.setSurveyVendor(sv);
						p.setPengguna(tu);
						p.setPeran(peran.getSelectedItem() == null ? SurveyVendorPengguna.PENILAI
								: peran.getSelectedItem().getLabel());
						p.setBolehLihatSemua(ls.isChecked());
						s.saveOrUpdate(p);
					}
					tx.commit();
					info("Survey tersimpan.");
					w.detach();
					buildSetup();
				} catch (Exception ex) {
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
					Common.tampilErrorJikaAdmin(ex);
					warn("Gagal menyimpan: " + ex.getMessage());
				}
			}
		});
		tombol(foot, "Batal", "#6b7280", new EventListener() {
			public void onEvent(Event e) throws Exception { w.detach(); }
		});
		w.doModal();
	}

	private void barisKriteria(Rows kr, SurveyVendorKriteria k) {
		final Row r = new Row();
		r.setParent(kr);
		r.setAttribute("k", k);
		MyTextbox nama = tb(k.getNama());
		r.setAttribute("nama", nama);
		nama.setParent(r);
		MyTextbox pert = tb(k.getPertanyaan());
		r.setAttribute("pert", pert);
		pert.setParent(r);
		MyDoublebox bobot = new MyDoublebox();
		bobot.setValue(k.getBobot());
		bobot.setWidth("70px");
		r.setAttribute("bobot", bobot);
		bobot.setParent(r);
		tombol(new Hbox() {{ setParent(r); }}, "x", "#dc2626", new EventListener() {
			public void onEvent(Event e) throws Exception { r.detach(); }
		});
	}

	private void barisPenilai(Rows pr, SurveyVendorPengguna p) {
		final Row r = new Row();
		r.setParent(pr);
		r.setAttribute("p", p);
		AmbilDataTbmuserBanbox box = new AmbilDataTbmuserBanbox();
		box.setWidth("95%");
		if (p.getPengguna() != null) {
			box.setAttribute("tbmuser", p.getPengguna());
			box.setValue(p.getPengguna().getUserNama());
		}
		r.setAttribute("user", box);
		box.setParent(r);
		Combobox peran = new Combobox();
		peran.setReadonly(true);
		peran.setWidth("90%");
		for (String x : new String[] { SurveyVendorPengguna.PENILAI, SurveyVendorPengguna.PENGAMAT }) {
			Comboitem it = peran.appendItem(x);
			it.setValue(x);
			if (x.equals(p.getPeran())) {
				peran.setSelectedItem(it);
			}
		}
		if (peran.getSelectedItem() == null) {
			peran.setSelectedIndex(0);
		}
		r.setAttribute("peran", peran);
		peran.setParent(r);
		org.zkoss.zul.Checkbox ls = new org.zkoss.zul.Checkbox();
		ls.setChecked(p.getBolehLihatSemua());
		r.setAttribute("ls", ls);
		ls.setParent(r);
		tombol(new Hbox() {{ setParent(r); }}, "x", "#dc2626", new EventListener() {
			public void onEvent(Event e) throws Exception { r.detach(); }
		});
	}

	private void aktifkan(SurveyVendor sv) throws Exception {
		Session s = HibernateUtil.currentSession();
		Transaction tx = s.beginTransaction();
		List<Tbmuser> penilai = new ArrayList<Tbmuser>();
		SurveyVendor db;
		try {
			db = (SurveyVendor) s.get(SurveyVendor.class, sv.getId());
			db.setStatus(SurveyVendor.AKTIF);
			s.update(db);
			for (SurveyVendorPengguna p : (List<SurveyVendorPengguna>) s.createCriteria(SurveyVendorPengguna.class)
					.add(Restrictions.eq("surveyVendor", db)).list()) {
				p.setSudahNotifikasi(true);
				s.update(p);
				if (p.getPengguna() != null && SurveyVendorPengguna.PENILAI.equals(p.getPeran())) {
					penilai.add(p.getPengguna());
				}
			}
			tx.commit();
		} catch (Exception ex) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			warn("Gagal: " + ex.getMessage());
			return;
		}
		// Notifikasi NYATA ke penilai (setelah commit; util membuka sesi sendiri) -> masuk lonceng "Info".
		kirimNotifikasi(db, penilai);
		info("Survey diaktifkan. " + penilai.size() + " penilai dinotifikasi.");
		buildSetup();
		buildPenilaian();
	}

	/** Terbitkan pemberitahuan ke daftar penilai lewat mekanisme notifikasi app (CommonNotifikasi). */
	private void kirimNotifikasi(SurveyVendor sv, List<Tbmuser> penilai) {
		try {
			if (penilai == null || penilai.isEmpty()) {
				return;
			}
			java.util.LinkedHashMap<String, String> r = new java.util.LinkedHashMap<String, String>();
			r.put("Jenis Pemberitahuan", "Survey Penilaian Vendor");
			r.put("Judul Survey", nvl(sv.getJudul()));
			r.put("Jenis Barang/Jasa", nvl(sv.getJenisBarangJasa()));
			r.put("Kode", nvl(sv.getKode()));
			String[] p = new String[] {
					"Anda ditugaskan sebagai penilai pada survey pemilihan vendor.",
					"Silakan buka menu \"Survey Penilaian Vendor\" -> tab \"Penilaian Saya\" untuk mengisi skor tiap vendor." };
			ais.common.CommonNotifikasi.terbitkanKeBanyak(penilai, "Tugas Penilaian Vendor: " + nvl(sv.getJudul()),
					"Anda diminta menilai vendor pada survey \"" + nvl(sv.getJudul()) + "\".", r, p, sv,
					"/pages/master/library/survey_vendor.zul", null, "Penilaian Vendor");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit SurveyVendorAction.kirimNotifikasi");
		}
	}

	private void hapusSurvey(final SurveyVendor sv) throws Exception {
		MyMessageboxConfig.showFormatCb("Hapus survey \"{V1}\" beserta semua penilaiannya?", "Konfirmasi",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					public void onEvent(Event e) throws Exception {
						if (Integer.parseInt(e.getData().toString()) != MyMessageboxConfig.OK) {
							return;
						}
						Session s = HibernateUtil.currentSession();
						Transaction tx = s.beginTransaction();
						try {
							SurveyVendor db = (SurveyVendor) s.get(SurveyVendor.class, sv.getId());
							hapusChildren(s, db);
							s.delete(db);
							tx.commit();
							buildSetup();
						} catch (Exception ex) {
							if (tx != null && tx.isActive()) {
								tx.rollback();
							}
							warn("Gagal hapus: " + ex.getMessage());
						}
					}
				}, nvl(sv.getJudul()));
	}

	private void hapusChildren(Session s, SurveyVendor sv) {
		for (SurveyVendorPenilaian pn : (List<SurveyVendorPenilaian>) s.createCriteria(SurveyVendorPenilaian.class)
				.add(Restrictions.eq("surveyVendor", sv)).list()) {
			for (Object d : s.createCriteria(SurveyVendorPenilaianDetail.class).add(Restrictions.eq("penilaian", pn)).list()) {
				s.delete(d);
			}
			s.delete(pn);
		}
		for (Object o : s.createCriteria(SurveyVendorKriteria.class).add(Restrictions.eq("surveyVendor", sv)).list()) {
			s.delete(o);
		}
		for (Object o : s.createCriteria(SurveyVendorPengguna.class).add(Restrictions.eq("surveyVendor", sv)).list()) {
			s.delete(o);
		}
		for (Object o : s.createCriteria(SurveyVendorVendor.class).add(Restrictions.eq("surveyVendor", sv)).list()) {
			s.delete(o);
		}
		s.flush();
	}

	// ==================================================================================
	// TAB 2 — PENILAIAN SAYA (Tahap 3)
	// ==================================================================================

	private void buildPenilaian() {
		Common.clear(panelPenilaian);
		new Label("Survey yang ditugaskan kepada Anda untuk dinilai:").setParent(panelPenilaian);
		Grid g = grid(panelPenilaian);
		kolom(g, "Kode", "14%", "Judul", "34%", "Jenis", "18%", "Status Penilaian Saya", "16%", "Aksi", "18%");
		Rows rs = new Rows();
		rs.setParent(g);
		Session s = HibernateUtil.currentSession();
		List<SurveyVendorPengguna> ps = s.createCriteria(SurveyVendorPengguna.class)
				.add(Restrictions.eq("pengguna", user)).add(Restrictions.eq("peran", SurveyVendorPengguna.PENILAI)).list();
		for (SurveyVendorPengguna p : ps) {
			final SurveyVendor sv = p.getSurveyVendor();
			if (sv == null || !SurveyVendor.AKTIF.equals(sv.getStatus())) {
				continue;
			}
			SurveyVendorPenilaian pn = ambilPenilaianku(sv);
			Row r = new Row();
			r.setParent(rs);
			new Label(nvl(sv.getKode())).setParent(r);
			new Label(nvl(sv.getJudul())).setParent(r);
			new Label(nvl(sv.getJenisBarangJasa())).setParent(r);
			new Label(pn == null ? SurveyVendorPenilaian.BELUM : pn.getStatus()).setParent(r);
			Hbox aksi = new Hbox();
			aksi.setParent(r);
			tombol(aksi, pn != null && SurveyVendorPenilaian.SELESAI.equals(pn.getStatus()) ? "Ralat/Revisi" : "Nilai",
					"#4f46e5", new EventListener() {
						public void onEvent(Event e) throws Exception { formPenilaian(sv); }
					});
		}
	}

	private SurveyVendorPenilaian ambilPenilaianku(SurveyVendor sv) {
		return (SurveyVendorPenilaian) HibernateUtil.currentSession().createCriteria(SurveyVendorPenilaian.class)
				.add(Restrictions.eq("surveyVendor", sv)).add(Restrictions.eq("pengguna", user)).setMaxResults(1).uniqueResult();
	}

	private void formPenilaian(final SurveyVendor svArg) throws Exception {
		Session s = HibernateUtil.currentSession();
		final SurveyVendor sv = (SurveyVendor) s.get(SurveyVendor.class, svArg.getId());
		final List<SurveyVendorKriteria> kris = s.createCriteria(SurveyVendorKriteria.class)
				.add(Restrictions.eq("surveyVendor", sv)).add(Restrictions.eq("aktif", Boolean.TRUE))
				.addOrder(Order.asc("urutan")).list();
		final List<SurveyVendorVendor> vends = s.createCriteria(SurveyVendorVendor.class)
				.add(Restrictions.eq("surveyVendor", sv)).addOrder(Order.asc("urutan")).list();
		if (kris.isEmpty() || vends.isEmpty()) {
			warn("Survey belum lengkap (kriteria/vendor kosong).");
			return;
		}
		SurveyVendorPenilaian pnAda = ambilPenilaianku(sv);
		final SurveyVendorPenilaian pn = pnAda != null ? pnAda : new SurveyVendorPenilaian();
		final MyIntbox[][] cell = new MyIntbox[kris.size()][vends.size()];
		// existing detail map
		List<SurveyVendorPenilaianDetail> existing = pn.getId() == null ? new ArrayList<SurveyVendorPenilaianDetail>()
				: s.createCriteria(SurveyVendorPenilaianDetail.class).add(Restrictions.eq("penilaian", pn)).list();

		final MyWindow w = popup("Penilaian: " + nvl(sv.getJudul()), "780px");
		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;max-height:74vh;overflow:auto;");
		isi.setParent(w);
		new Label("Beri skor 1 (kurang) - 5 (sangat baik) untuk tiap kriteria per vendor.").setParent(isi);

		Grid g = grid(isi);
		Columns cs = new Columns();
		cs.setParent(g);
		Column c0 = new Column();
		c0.setLabel("Kriteria (bobot%)");
		c0.setWidth("40%");
		c0.setParent(cs);
		for (SurveyVendorVendor v : vends) {
			Column c = new Column();
			c.setLabel(v.getNamaVendor());
			c.setParent(cs);
		}
		Rows rs = new Rows();
		rs.setParent(g);
		for (int ki = 0; ki < kris.size(); ki++) {
			SurveyVendorKriteria k = kris.get(ki);
			Row r = new Row();
			r.setParent(rs);
			new Label(k.getNama() + " (" + fmt(k.getBobot()) + "%)").setParent(r);
			for (int vi = 0; vi < vends.size(); vi++) {
				MyIntbox ib = new MyIntbox();
				ib.setWidth("55px");
				ib.setConstraint("no negative,max 5");
				Integer val = cariNilai(existing, k, vends.get(vi));
				ib.setValue(val);
				cell[ki][vi] = ib;
				ib.setParent(r);
			}
		}
		final MyTextbox catatan = tb(pn.getCatatan());
		catatan.setMultiline(true);
		catatan.setRows(2);
		catatan.setWidth("98%");
		lbl(isi, "Catatan:", "margin-top:6px;");
		catatan.setParent(isi);

		Hbox foot = new Hbox();
		foot.setStyle("margin-top:10px;");
		foot.setParent(isi);
		tombol(foot, "Simpan Penilaian", "#059669", new EventListener() {
			public void onEvent(Event e) throws Exception {
				Session ses = HibernateUtil.currentSession();
				Transaction tx = ses.beginTransaction();
				try {
					if (pn.getId() == null) {
						pn.setSurveyVendor(sv);
						pn.setPengguna(user);
					}
					pn.setStatus(SurveyVendorPenilaian.SELESAI);
					pn.setTanggalPenilaian(new Date());
					pn.setCatatan(catatan.getValue());
					ses.saveOrUpdate(pn);
					ses.flush();
					// hapus detail lama, tulis ulang
					for (Object o : ses.createCriteria(SurveyVendorPenilaianDetail.class)
							.add(Restrictions.eq("penilaian", pn)).list()) {
						ses.delete(o);
					}
					ses.flush();
					for (int ki = 0; ki < kris.size(); ki++) {
						for (int vi = 0; vi < vends.size(); vi++) {
							Integer val = cell[ki][vi].getValue();
							if (val == null) {
								continue;
							}
							SurveyVendorPenilaianDetail d = new SurveyVendorPenilaianDetail();
							d.setPenilaian(pn);
							d.setKriteria(kris.get(ki));
							d.setVendor(vends.get(vi));
							d.setNilai(val);
							ses.save(d);
						}
					}
					// tandai sudah menilai
					SurveyVendorPengguna pg = (SurveyVendorPengguna) ses.createCriteria(SurveyVendorPengguna.class)
							.add(Restrictions.eq("surveyVendor", sv)).add(Restrictions.eq("pengguna", user))
							.setMaxResults(1).uniqueResult();
					if (pg != null) {
						pg.setSudahMenilai(true);
						ses.update(pg);
					}
					tx.commit();
					info("Penilaian tersimpan.");
					w.detach();
					buildPenilaian();
				} catch (Exception ex) {
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
					Common.tampilErrorJikaAdmin(ex);
					warn("Gagal: " + ex.getMessage());
				}
			}
		});
		tombol(foot, "Batal", "#6b7280", new EventListener() {
			public void onEvent(Event e) throws Exception { w.detach(); }
		});
		w.doModal();
	}

	private Integer cariNilai(List<SurveyVendorPenilaianDetail> ds, SurveyVendorKriteria k, SurveyVendorVendor v) {
		for (SurveyVendorPenilaianDetail d : ds) {
			if (d.getKriteria() != null && d.getVendor() != null && eq(d.getKriteria().getId(), k.getId())
					&& eq(d.getVendor().getId(), v.getId())) {
				return d.getNilai();
			}
		}
		return null;
	}

	// ==================================================================================
	// TAB 3 — DASHBOARD HASIL (Tahap 4)
	// ==================================================================================

	private void buildDashboard() {
		Common.clear(panelDashboard);
		final Combobox pilih = new Combobox();
		pilih.setReadonly(true);
		pilih.setWidth("420px");
		for (SurveyVendor sv : daftarSurvey()) {
			Comboitem it = pilih.appendItem(nvl(sv.getKode()) + " - " + nvl(sv.getJudul()));
			it.setValue(sv);
		}
		final Div hasil = new Div();
		hasil.setWidth("100%");
		pilih.addEventListener(Events.ON_SELECT, new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (pilih.getSelectedItem() != null) {
					renderHasil(hasil, (SurveyVendor) pilih.getSelectedItem().getValue());
				}
			}
		});
		new Label("Survey: ").setParent(panelDashboard);
		pilih.setParent(panelDashboard);
		hasil.setParent(panelDashboard);
	}

	private void renderHasil(final Div wrap, final SurveyVendor svArg) throws Exception {
		Common.clear(wrap);
		Session s = HibernateUtil.currentSession();
		final SurveyVendor sv = (SurveyVendor) s.get(SurveyVendor.class, svArg.getId());
		List<SurveyVendorKriteria> kris = s.createCriteria(SurveyVendorKriteria.class)
				.add(Restrictions.eq("surveyVendor", sv)).add(Restrictions.eq("aktif", Boolean.TRUE))
				.addOrder(Order.asc("urutan")).list();
		final List<SurveyVendorVendor> vends = s.createCriteria(SurveyVendorVendor.class)
				.add(Restrictions.eq("surveyVendor", sv)).addOrder(Order.asc("urutan")).list();
		List<SurveyVendorPenilaian> pns = s.createCriteria(SurveyVendorPenilaian.class)
				.add(Restrictions.eq("surveyVendor", sv)).add(Restrictions.eq("status", SurveyVendorPenilaian.SELESAI)).list();
		List<SurveyVendorPenilaianDetail> allDetail = new ArrayList<SurveyVendorPenilaianDetail>();
		for (SurveyVendorPenilaian pn : pns) {
			allDetail.addAll(s.createCriteria(SurveyVendorPenilaianDetail.class).add(Restrictions.eq("penilaian", pn)).list());
		}
		if (kris.isEmpty() || vends.isEmpty()) {
			new Label("Survey belum lengkap.").setParent(wrap);
			return;
		}
		lbl(wrap, "Jumlah penilai selesai: " + pns.size(), "font-weight:bold;");

		// matriks rata-rata
		Grid g = grid(wrap);
		Columns cs = new Columns();
		cs.setParent(g);
		Column ck = new Column();
		ck.setLabel("Kriteria (bobot%)");
		ck.setWidth("34%");
		ck.setParent(cs);
		for (SurveyVendorVendor v : vends) {
			Column c = new Column();
			c.setLabel(v.getNamaVendor());
			c.setParent(cs);
		}
		Rows rs = new Rows();
		rs.setParent(g);
		double[] skor = new double[vends.size()];
		double totalBobot = 0;
		for (SurveyVendorKriteria k : kris) {
			totalBobot += k.getBobot();
		}
		if (totalBobot <= 0) {
			totalBobot = 1;
		}
		for (SurveyVendorKriteria k : kris) {
			Row r = new Row();
			r.setParent(rs);
			new Label(k.getNama() + " (" + fmt(k.getBobot()) + "%)").setParent(r);
			for (int vi = 0; vi < vends.size(); vi++) {
				double avg = rataRata(allDetail, k, vends.get(vi));
				new Label(avg <= 0 ? "-" : fmt2(avg)).setParent(r);
				skor[vi] += avg * k.getBobot();
			}
		}
		// skor tertimbang (0-100)
		Row rTot = new Row();
		rTot.setStyle("background:#f0fdf4;font-weight:bold;");
		rTot.setParent(rs);
		new Label("SKOR TERTIMBANG (0-100)").setParent(rTot);
		int winner = -1;
		double best = -1;
		final double[] skor100 = new double[vends.size()];
		for (int vi = 0; vi < vends.size(); vi++) {
			skor100[vi] = Math.round((skor[vi] / totalBobot) / 5.0 * 100.0 * 100.0) / 100.0;
			new Label("" + skor100[vi]).setParent(rTot);
			if (skor100[vi] > best) {
				best = skor100[vi];
				winner = vi;
			}
		}
		Row rRank = new Row();
		rRank.setStyle("background:#fefce8;font-weight:bold;");
		rRank.setParent(rs);
		new Label("PERINGKAT").setParent(rRank);
		for (int vi = 0; vi < vends.size(); vi++) {
			int rank = 1;
			for (int w = 0; w < vends.size(); w++) {
				if (skor100[w] > skor100[vi]) {
					rank++;
				}
			}
			new Label(skor100[vi] > 0 ? "#" + rank : "-").setParent(rRank);
		}

		// audit + pemilihan akhir
		final int autoWinner = winner;
		new Label(" ").setParent(wrap);
		Label lw = new Label("Pemenang otomatis (skor tertinggi): "
				+ (autoWinner >= 0 && best > 0 ? vends.get(autoWinner).getNamaVendor() + " (" + best + ")" : "-"));
		lw.setStyle("font-weight:bold;color:#065f46;");
		lw.setParent(wrap);

		if (isStaf && SurveyVendor.AKTIF.equals(sv.getStatus()) || (isStaf && SurveyVendor.SELESAI.equals(sv.getStatus()))) {
			lbl(wrap, "Penetapan Akhir (staf pengadaan):", "font-weight:bold;margin-top:8px;color:#4f46e5;");
			Grid gf = grid(wrap);
			kolom(gf, "", "26%", "", "74%");
			Rows fr = new Rows();
			fr.setParent(gf);
			final Combobox pilihVendor = new Combobox();
			pilihVendor.setReadonly(true);
			pilihVendor.setWidth("360px");
			for (SurveyVendorVendor v : vends) {
				Comboitem it = pilihVendor.appendItem(v.getNamaVendor());
				it.setValue(v);
				if (sv.getVendorTerpilih() != null && eq(sv.getVendorTerpilih().getId(), v.getId())) {
					pilihVendor.setSelectedItem(it);
				}
			}
			labelRow(fr, "Vendor Terpilih", pilihVendor);
			final Combobox rek = new Combobox();
			rek.setReadonly(true);
			rek.setWidth("280px");
			for (String x : new String[] { SurveyVendor.REKOM_DIREKOMENDASIKAN, SurveyVendor.REKOM_PERTIMBANGAN_ULANG,
					SurveyVendor.REKOM_TIDAK }) {
				Comboitem it = rek.appendItem(x);
				it.setValue(x);
				if (x.equals(sv.getRekomendasi())) {
					rek.setSelectedItem(it);
				}
			}
			labelRow(fr, "Rekomendasi", rek);
			final MyTextbox alasan = tbRow(fr, "Alasan Utama (jika beda dari pemenang otomatis, wajib jelaskan)",
					sv.getAlasanUtama(), true);
			final MyTextbox namaPenilai = tbRow(fr, "Nama Penilai", sv.getNamaPenilai() == null
					? (user == null ? "" : user.getUserNama()) : sv.getNamaPenilai(), false);
			final MyTextbox jabatan = tbRow(fr, "Jabatan", sv.getJabatanPenilai(), false);
			Hbox foot = new Hbox();
			foot.setStyle("margin-top:8px;");
			foot.setParent(wrap);
			tombol(foot, "Simpan Hasil Akhir & Selesai", "#059669", new EventListener() {
				public void onEvent(Event e) throws Exception {
					Session ses = HibernateUtil.currentSession();
					Transaction tx = ses.beginTransaction();
					try {
						SurveyVendor db = (SurveyVendor) ses.get(SurveyVendor.class, sv.getId());
						db.setVendorTerpilih(pilihVendor.getSelectedItem() == null ? null
								: (SurveyVendorVendor) pilihVendor.getSelectedItem().getValue());
						db.setRekomendasi(rek.getSelectedItem() == null ? null : (String) rek.getSelectedItem().getValue());
						db.setAlasanUtama(alasan.getValue());
						db.setNamaPenilai(namaPenilai.getValue());
						db.setJabatanPenilai(jabatan.getValue());
						db.setTanggalPenilaian(new Date());
						db.setStatus(SurveyVendor.SELESAI);
						ses.update(db);
						tx.commit();
						info("Hasil akhir tersimpan. Survey selesai.");
						buildSetup();
						renderHasil(wrap, db);
					} catch (Exception ex) {
						if (tx != null && tx.isActive()) {
							tx.rollback();
						}
						warn("Gagal: " + ex.getMessage());
					}
				}
			});
		} else if (sv.getVendorTerpilih() != null) {
			Label lf = new Label("Vendor terpilih (final): " + sv.getVendorTerpilih().getNamaVendor()
					+ " | Rekomendasi: " + nvl(sv.getRekomendasi()) + " | Alasan: " + nvl(sv.getAlasanUtama()));
			lf.setStyle("font-weight:bold;color:#2563eb;margin-top:8px;");
			lf.setParent(wrap);
		}
	}

	private double rataRata(List<SurveyVendorPenilaianDetail> ds, SurveyVendorKriteria k, SurveyVendorVendor v) {
		int sum = 0, n = 0;
		for (SurveyVendorPenilaianDetail d : ds) {
			if (d.getKriteria() != null && d.getVendor() != null && d.getNilai() != null
					&& eq(d.getKriteria().getId(), k.getId()) && eq(d.getVendor().getId(), v.getId())) {
				sum += d.getNilai();
				n++;
			}
		}
		return n == 0 ? 0 : (double) sum / n;
	}

	// ==================================================================================
	// util
	// ==================================================================================

	private List<SurveyVendorVendor> muatVendor(SurveyVendor sv) {
		if (sv.getId() == null) {
			return new ArrayList<SurveyVendorVendor>();
		}
		return HibernateUtil.currentSession().createCriteria(SurveyVendorVendor.class)
				.add(Restrictions.eq("surveyVendor", sv)).addOrder(Order.asc("urutan")).list();
	}

	private List<SurveyVendorKriteria> muatKriteria(SurveyVendor sv) {
		if (sv.getId() == null) {
			return new ArrayList<SurveyVendorKriteria>();
		}
		return HibernateUtil.currentSession().createCriteria(SurveyVendorKriteria.class)
				.add(Restrictions.eq("surveyVendor", sv)).addOrder(Order.asc("urutan")).list();
	}

	private List<SurveyVendorPengguna> muatPengguna(SurveyVendor sv) {
		if (sv.getId() == null) {
			return new ArrayList<SurveyVendorPengguna>();
		}
		return HibernateUtil.currentSession().createCriteria(SurveyVendorPengguna.class)
				.add(Restrictions.eq("surveyVendor", sv)).addOrder(Order.asc("id")).list();
	}

	private int hitung(Class clazz, String prop, Object val) {
		Number n = (Number) HibernateUtil.currentSession().createCriteria(clazz).add(Restrictions.eq(prop, val))
				.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	private Grid grid(Component parent) {
		Grid g = new Grid();
		g.setSclass("dgrid");
		g.setWidth("100%");
		g.setParent(parent);
		return g;
	}

	private void kolom(Grid g, String... labelWidth) {
		Columns cs = new Columns();
		cs.setSizable(true);
		cs.setParent(g);
		for (int i = 0; i + 1 < labelWidth.length; i += 2) {
			Column c = new Column();
			c.setLabel(labelWidth[i]);
			if (labelWidth[i + 1] != null && !labelWidth[i + 1].isEmpty()) {
				c.setWidth(labelWidth[i + 1]);
			}
			c.setParent(cs);
		}
	}

	private MyTextbox tb(String v) {
		MyTextbox t = new MyTextbox();
		t.setWidth("95%");
		t.setValue(v == null ? "" : v);
		return t;
	}

	private MyTextbox tbRow(Rows rs, String label, String val, boolean multiline) {
		MyTextbox t = tb(val);
		if (multiline) {
			t.setMultiline(true);
			t.setRows(2);
			t.setWidth("98%");
		}
		labelRow(rs, label, t);
		return t;
	}

	private void labelRow(Rows rs, String label, Component control) {
		Row r = new Row();
		r.setValign("top");
		r.setParent(rs);
		new Label(label).setParent(r);
		control.setParent(r);
	}

	private void tombol(Component parent, String label, String warna, EventListener onClick) {
		Button b = new Button(label);
		b.setStyle("background:" + warna + ";color:#fff;border:none;padding:3px 10px;border-radius:5px;cursor:pointer;margin:1px;");
		b.addEventListener(Events.ON_CLICK, onClick);
		b.setParent(parent);
	}

	private MyWindow popup(String title, String width) {
		MyWindow w = new MyWindow();
		window.getPage().getFirstRoot().appendChild(w);
		w.setTitle(title);
		w.setWidth(width);
		w.setBorder("normal");
		w.setClosable(true);
		w.setSizable(true);
		w.setMaximizable(true);
		return w;
	}

	private void info(String m) throws Exception { MyMessageboxConfig.show(m, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION); }
	private void warn(String m) throws Exception { MyMessageboxConfig.show(m, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); }
	private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
	private static String nvl(String s) { return s == null || s.trim().isEmpty() ? "-" : s; }
	private static boolean eq(Long a, Long b) { return a != null && a.equals(b); }
	private static String fmt(Double d) { return d == null ? "0" : (Math.round(d * 100.0) / 100.0) + ""; }
	private static String fmt2(double d) { return (Math.round(d * 100.0) / 100.0) + ""; }
}
