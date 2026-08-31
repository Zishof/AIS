package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.DiagnosaPenyakitPunyaPemeriksaan;
import ais.database.model.sirs.Pemeriksaan;

/**
 * Helper UI ZK modul SIRS (Sistem Informasi Rumah Sakit) yang membangun formulir pemeriksaan
 * dinamis (form builder) berdasarkan definisi {@link Pemeriksaan} yang dikonfigurasi admin, lalu
 * menyimpan hasilnya sebagai {@link DiagnosaPenyakitPunyaPemeriksaan} terkait satu
 * {@link DiagnosaPenyakit}. Item pemeriksaan berjenjang (memiliki {@code parent}/anak) dirender
 * sebagai grup tab per kategori tingkat pertama ({@link ais.ui.util.MyButtonTabbox}), dengan
 * item bertingkat lebih dalam ditampilkan sebagai baris indentasi bersarang (rekursif lewat
 * {@link #createRowPemeriksaan}) di bawah judul kategorinya.
 *
 * <p>
 * Tipe input per item pemeriksaan ditentukan dari format field {@code nilaiYangMungkin} pada
 * {@link Pemeriksaan}: dipisah {@code ";"} untuk combobox pilihan tunggal, dipisah {@code "|"}
 * untuk sekelompok checkbox pilihan ganda, atau textbox polos bila tidak mengandung pemisah
 * tersebut. Setiap kategori memiliki checkbox "Aktifkan formulir ini" yang mengunci/membuka
 * kunci ({@code Common#freeze}) seluruh baris di bawahnya. Data tersimpan sebelumnya (bila ada)
 * dimuat ulang ke komponen form lewat {@link DiagnosaPenyakitPunyaPemeriksaan}; {@link
 * #simpan(DiagnosaPenyakit)} menuliskan kembali nilai form ke database, dan {@link #check()}
 * memvalidasi field yang ditandai wajib isi ({@code harusDiisi}) sebelum data boleh disimpan.
 * </p>
 */
public class PemeriksaanHelper {

	private String jenis;

	/** Membangun helper yang membatasi item pemeriksaan yang dimuat hanya untuk {@code jenis} (kategori pemeriksaan) tertentu. */
	public PemeriksaanHelper(String jenis) {
		this.jenis = jenis;
	}

	private List<Rows> rowsPemeriksaans;

	/** Membangun panel formulir pemeriksaan untuk {@code diagnosaPenyakit}, menginisialisasi daftar {@link Rows} internal untuk keperluan {@link #simpan} dan {@link #check} nanti. */
	@SuppressWarnings({})
	public Borderlayout createPemeriksaan(DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		rowsPemeriksaans = new ArrayList<Rows>();
		return createPanelPemeriksaan(diagnosaPenyakit);
	}

	@SuppressWarnings({ "unchecked" })
	/** Membangun grup tab (satu tab per kategori pemeriksaan tingkat pertama dari {@link #jenis}), memuat baris formulir untuk setiap tab, dan memilih tab pertama sebagai default. */
	private Borderlayout createPanelPemeriksaan(DiagnosaPenyakit diagnosaPenyakit) {

		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Session session = HibernateUtil.currentSession();
		List<Pemeriksaan> pemeriksaans = ConstantValues.simpleList(session.createCriteria(Pemeriksaan.class)
				.add(Restrictions.isNull("parent")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jenis", jenis)).addOrder(Order.asc("nama")), Pemeriksaan.class);

		// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab per
		// jenis pemeriksaan ini data-driven, sama seperti pola "Ke-1".."Ke-N" di
		// SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai Tab/Tabpanel
		// bawaan ZK. Konten tetap eager seperti semula.
		ais.ui.util.MyButtonTabbox tabboxPemeriksaan = ais.ui.util.MyButtonTabbox.buat(center, "100%", null);
		int indexPemeriksaan = 1;
		for (Pemeriksaan pemeriksaan : pemeriksaans) {
			org.zkoss.zul.Div panelPemeriksaan = tabboxPemeriksaan.tambahTab(indexPemeriksaan,
					pemeriksaan.getNama());
			mulaiRowPemeriksaan(diagnosaPenyakit, pemeriksaan, panelPemeriksaan);
			indexPemeriksaan++;
		}
		tabboxPemeriksaan.pilih(1);

		return borderlayout;
	}

	/** Membangun grid baris formulir untuk satu kategori (tab) pemeriksaan {@code parent} di dalam {@code tabpanel}, beserta checkbox "Aktifkan formulir ini" yang mengunci/membuka kunci seluruh baris. */
	private void mulaiRowPemeriksaan(DiagnosaPenyakit diagnosaPenyakit, Pemeriksaan parent, Component tabpanel) {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setOddRowSclass("non-odd");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("60%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		final Rows rowsPemeriksaan = new Rows();
		rowsPemeriksaans.add(rowsPemeriksaan);
		rowsPemeriksaan.setParent(grid);

		final Checkbox aktifkan = new Checkbox("Aktifkan formulir ini");
		createRowPemeriksaan(diagnosaPenyakit, parent, rowsPemeriksaan, aktifkan);

		North north = new North();
		north.setTitle(parent.getNama());
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		aktifkan.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.freeze(rowsPemeriksaan, !aktifkan.isChecked());
			}
		});
		north.appendChild(aktifkan);
	}

	/**
	 * Merender secara rekursif baris-baris item pemeriksaan anak dari {@code parent} ke
	 * {@code rowsPemeriksaan}: item yang masih punya anak dirender sebagai judul kategori
	 * beranak (indentasi bertambah, dipanggil ulang untuk anaknya), item daun dirender sebagai
	 * baris input (combobox/checkbox-ganda/textbox sesuai format {@code nilaiYangMungkin}) yang
	 * diisi dengan data tersimpan bila {@code diagnosaPenyakit} sudah memiliki
	 * {@link DiagnosaPenyakitPunyaPemeriksaan} terkait, atau nilai default bila belum.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void createRowPemeriksaan(DiagnosaPenyakit diagnosaPenyakit, Pemeriksaan parent, Rows rowsPemeriksaan,
			final Checkbox aktifkan) {

		Session session = HibernateUtil.currentSession();
		List<Pemeriksaan> pemeriksaans = ConstantValues.simpleList(session.createCriteria(Pemeriksaan.class)
				.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("jenis", jenis)).addOrder(Order.asc("nama")),
				Pemeriksaan.class);

		for (Pemeriksaan pemeriksaan : pemeriksaans) {

			String tambahanDepan = "";
			Pemeriksaan temPemeriksaan = pemeriksaan.getParent();
			while (temPemeriksaan != null) {
				tambahanDepan += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				temPemeriksaan = temPemeriksaan.getParent();
			}

			// System.out.println("tambahanDepan = " + tambahanDepan);

			int count = ((Number) session.createCriteria(Pemeriksaan.class).add(Restrictions.eq("parent", pemeriksaan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count != 0) {

				Row row = new Row();
				row.setValign("top");
				row.setParent(rowsPemeriksaan);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(new Html(
						"<div style=\"font-size:x-small;\">" + tambahanDepan + pemeriksaan.getNama() + "</div>"));

				createRowPemeriksaan(diagnosaPenyakit, pemeriksaan, rowsPemeriksaan, aktifkan);

			} else {

				Row row = new Row();
				row.setValign("top");
				row.setAttribute("pemeriksaan", pemeriksaan);
				row.setParent(rowsPemeriksaan);

				Combobox yaTidak = new Combobox();
				yaTidak.setDisabled(true);
				yaTidak.setCols(10);

				if (pemeriksaan.getNilaiYangMungkin().trim().contains(";")) {
					String[] nilais = pemeriksaan.getNilaiYangMungkin().trim().split(";");
					for (String nilai : nilais) {
						if (nilai != null && !nilai.trim().equals("")) {
							Comboitem ya = new Comboitem(nilai.trim());
							ya.setValue(nilai.trim());
							yaTidak.appendChild(ya);
						}
					}
				}

				Vbox pilihanGanda = new Vbox();
				if (pemeriksaan.getNilaiYangMungkin().trim().contains("|")) {
					String[] nilais = pemeriksaan.getNilaiYangMungkin().trim().split("\\|");
					for (String nilai : nilais) {
						if (nilai != null && !nilai.trim().equals("")) {
							Checkbox ya = new Checkbox(nilai.trim().trim());
							ya.setValue(nilai.trim());
							ya.setDisabled(true);
							pilihanGanda.appendChild(ya);
						}
					}
				}

				row.setAttribute("pilihanGanda", pilihanGanda);
				row.setAttribute("yaTidak", yaTidak);

				final Textbox keterangan = new Textbox();
				keterangan.setDisabled(true);
				keterangan.setRows(pemeriksaan.getJumlahBaris());
				keterangan.setCols(15);
				row.setAttribute("keterangan", keterangan);

				row.appendChild(new Html(
						"<div style=\"font-size:x-small;\">" + tambahanDepan + pemeriksaan.getNama() + "</div>"));
				if (pemeriksaan.getNilaiYangMungkin().trim().contains("|")) {

					final Checkbox ya = new Checkbox();
					ya.setDisabled(true);
					pilihanGanda.appendChild(new Hbox(new Component[] { ya, keterangan }));

					keterangan.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ya.setChecked(!keterangan.getValue().trim().equals(""));
						}
					});

					row.appendChild(new Hbox(new Component[] { pilihanGanda, new Label(pemeriksaan.getSatuan()),
							new Label(pemeriksaan.getNormal()) }));
				} else if (pemeriksaan.getNilaiYangMungkin().trim().contains(";")) {
					row.appendChild(new Hbox(new Component[] { yaTidak, keterangan, new Label(pemeriksaan.getSatuan()),
							new Label(pemeriksaan.getNormal()) }));
				} else {
					row.appendChild(new Hbox(new Component[] { keterangan, new Label(pemeriksaan.getSatuan()),
							new Label(pemeriksaan.getNormal()) }));
					keterangan.setCols(30);
				}

				if (diagnosaPenyakit != null && diagnosaPenyakit.getId() != null) {
					DiagnosaPenyakitPunyaPemeriksaan diagnosaPenyakitPunyaPemeriksaan = (DiagnosaPenyakitPunyaPemeriksaan) session
							.createCriteria(DiagnosaPenyakitPunyaPemeriksaan.class)
							.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit))
							.add(Restrictions.eq("pemeriksaan", pemeriksaan)).setMaxResults(1).uniqueResult();
					if (diagnosaPenyakitPunyaPemeriksaan != null) {
						row.setAttribute("diagnosaPenyakitPunyaPemeriksaan", diagnosaPenyakitPunyaPemeriksaan);
						Common.selectComboItem(yaTidak, diagnosaPenyakitPunyaPemeriksaan.getNama());
						keterangan.setValue(diagnosaPenyakitPunyaPemeriksaan.getKeterangan());

						Common.freeze(row, !diagnosaPenyakitPunyaPemeriksaan.getAktif());

						if (diagnosaPenyakitPunyaPemeriksaan.getAktif()) {
							aktifkan.setChecked(true);
						}

						String[] nilais = diagnosaPenyakitPunyaPemeriksaan.getPilihanGanda().trim().split("\\|");

						List<Component> checkboxs = pilihanGanda.getChildren();
						for (Component c : checkboxs) {
							try {
								if (c instanceof Checkbox) {
									Checkbox checkbox = (Checkbox) c;
									for (String nilai : nilais) {
										if (nilai != null && !nilai.trim().equals("")) {
											if (nilai.trim().equalsIgnoreCase(checkbox.getValue().toString().trim())) {
												checkbox.setChecked(true);
												break;
											}
										}

									}
								} else if (c instanceof Hbox) {
									Checkbox checkbox = (Checkbox) c.getChildren().get(0);
									if (!keterangan.getValue().trim().equals("")) {
										checkbox.setChecked(true);
										break;
									}
								}
							}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/PemeriksaanHelper.java:297");
								// TODO: handle exception
							}

						}

					} else {
						Common.selectComboItem(yaTidak, pemeriksaan.getNilaiDefault());
					}
				} else {
					Common.selectComboItem(yaTidak, pemeriksaan.getNilaiDefault());
				}
			}
		}
	}

	/**
	 * Menyimpan (create-or-update) hasil seluruh baris formulir pemeriksaan yang dibangun
	 * sebelumnya sebagai baris {@link DiagnosaPenyakitPunyaPemeriksaan}, mengaitkannya ke
	 * {@code diagnosaPenyakit}. Status aktif diambil dari kondisi terkunci/tidaknya kolom
	 * keterangan (kategori dinonaktifkan bila baris dikunci); tidak melakukan apa pun bila
	 * belum ada formulir yang dibangun.
	 */
	public void simpan(DiagnosaPenyakit diagnosaPenyakit) {
		if (rowsPemeriksaans == null || rowsPemeriksaans.isEmpty()) {
			return;
		}
		for (Rows rowsPemeriksaan : rowsPemeriksaans) {
			List<Row> rows = rowsPemeriksaan.getChildren();
			Session session = HibernateUtil.currentSession();
			for (Row row : rows) {
				Pemeriksaan pemeriksaan = (Pemeriksaan) row.getAttribute("pemeriksaan");
				if (pemeriksaan != null) {

					DiagnosaPenyakitPunyaPemeriksaan diagnosaPenyakitPunyaPemeriksaan = (DiagnosaPenyakitPunyaPemeriksaan) row
							.getAttribute("diagnosaPenyakitPunyaPemeriksaan");
					if (diagnosaPenyakitPunyaPemeriksaan == null) {
						diagnosaPenyakitPunyaPemeriksaan = new DiagnosaPenyakitPunyaPemeriksaan();
					}

					Vbox pilihanGanda = (Vbox) row.getAttribute("pilihanGanda");

					String pg = "";
					List<Component> checkboxs = pilihanGanda.getChildren();
					for (Component c : checkboxs) {
						if (c instanceof Checkbox) {
							try {
								Checkbox checkbox = (Checkbox) c;
								if (checkbox.isChecked()) {
									pg += pg.equals("") ? checkbox.getValue().toString().trim() : "|" + checkbox.getValue().toString().trim();
								}
							}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/detail/PemeriksaanHelper.java:341");
								// TODO: handle exception
							}
						}
					}

					Combobox yaTidak = (Combobox) row.getAttribute("yaTidak");
					Textbox keterangan = (Textbox) row.getAttribute("keterangan");
					diagnosaPenyakitPunyaPemeriksaan.setPilihanGanda(pg);
					diagnosaPenyakitPunyaPemeriksaan.setAktif(!keterangan.isDisabled());
					diagnosaPenyakitPunyaPemeriksaan.setDiagnosaPenyakit(diagnosaPenyakit);
					diagnosaPenyakitPunyaPemeriksaan.setPemeriksaan(pemeriksaan);
					diagnosaPenyakitPunyaPemeriksaan.setNama(
							(String) (yaTidak.getSelectedItem() == null ? "" : yaTidak.getSelectedItem().getValue()));
					diagnosaPenyakitPunyaPemeriksaan.setKeterangan(keterangan.getValue());
					session.saveOrUpdate(diagnosaPenyakitPunyaPemeriksaan);
				}
			}
		}
	}

	/**
	 * Memvalidasi seluruh baris formulir pemeriksaan yang aktif (tidak dikunci): item yang
	 * ditandai {@code harusDiisi} pada {@link Pemeriksaan} harus memiliki nilai (combobox
	 * terpilih, keterangan terisi, atau minimal satu checkbox tercentang). Menampilkan pesan
	 * peringatan dan memfokuskan kolom yang bermasalah pada pelanggaran pertama yang ditemukan.
	 *
	 * @return {@code true} bila seluruh item wajib terisi (atau tidak ada formulir dibangun), {@code false} bila ada pelanggaran
	 */
	public boolean check() throws Exception {
		if (rowsPemeriksaans == null || rowsPemeriksaans.isEmpty()) {
			return true;
		}
		for (Rows rowsPemeriksaan : rowsPemeriksaans) {
			List<Row> rows = rowsPemeriksaan.getChildren();
			for (Row row : rows) {
				Pemeriksaan pemeriksaan = (Pemeriksaan) row.getAttribute("pemeriksaan");
				if (pemeriksaan != null) {

					Combobox yaTidak = (Combobox) row.getAttribute("yaTidak");
					Textbox keterangan = (Textbox) row.getAttribute("keterangan");
					if (pemeriksaan.getHarusDiisi() && !keterangan.isDisabled()) {

						Vbox pilihanGanda = (Vbox) row.getAttribute("pilihanGanda");

						String pg = "";
						List<Component> checkboxs = pilihanGanda.getChildren();
						for (Component c : checkboxs) {
							if (c instanceof Checkbox) {
								Checkbox checkbox = (Checkbox) c;
								if (checkbox.isChecked()) {
									pg += pg.equals("") ? checkbox.getLabel() : "|" + checkbox.getLabel();
								}
							}
						}

						if (yaTidak.getSelectedItem() == null && keterangan.getValue().trim().equals("")
								&& pg.trim().equals("")) {
							MyMessageboxConfig.showFormat("Mohon maaf, formulir dengan isian \"{V1}\" wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) lengkapi isian tersebut pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, pemeriksaan.getNama());
							keterangan.focus();
							return false;
						}
					}
				}
			}
		}
		return true;
	}
}
