package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.JabatanOrganisasiDosen;
import ais.database.model.OrganisasiDosen;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk grid keanggotaan dosen pada organisasi dosen ({@link OrganisasiDosenPunyaDosen}).
 * Dapat dipakai dalam dua konteks: (1) menampilkan seluruh organisasi milik satu {@link Dosen}
 * (konstruktor {@link #DosenPunyaOrganisasiDosenHelper()}, dipanggil lewat {@link #display}), atau
 * (2) menampilkan seluruh dosen anggota satu organisasi/jabatan/tahun tertentu (konstruktor
 * {@link #DosenPunyaOrganisasiDosenHelper(OrganisasiDosen, JabatanOrganisasiDosen, Integer)}).
 * Setiap baris memuat foto+identitas dosen, nama organisasi, rentang mulai-sampai, jabatan, keterangan,
 * unggahan lampiran SK/surat keterangan, serta status persetujuan.
 *
 * <p>
 * Hak edit satu baris ditentukan oleh dua kondisi independen: dosen yang login adalah pemilik baris
 * dan datanya belum disetujui ({@code bolehEdit}), atau dosen yang login adalah atasan langsung dosen
 * pemilik baris ({@code merupakanAtasanLangsung}) — kombinasi ini memungkinkan alur persetujuan
 * berjenjang, di mana atasan langsung dapat mencentang "Setujui" untuk mengunci data bawahannya dari
 * perubahan lebih lanjut.
 * </p>
 */
public class DosenPunyaOrganisasiDosenHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Dosen dosen;
	private Textbox nama;

	private Paging paging;
	private Tbmuser tbmuser;
	private OrganisasiDosen organisasiDosen = null;
	private JabatanOrganisasiDosen jabatanOrganisasiDosen = null;
	private Integer tahun = null;
	private OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen;

	/** Membuat helper dalam mode "organisasi milik satu dosen" (filter organisasi/jabatan/tahun kosong). */
	public DosenPunyaOrganisasiDosenHelper() {

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/**
	 * Membuat helper dalam mode "anggota satu organisasi", menetapkan filter tetap organisasi/
	 * jabatan/tahun yang akan selalu diterapkan pada {@link #initCriteria(boolean)}.
	 *
	 * @param organisasiDosen        organisasi yang anggotanya ditampilkan, boleh {@code null}
	 * @param jabatanOrganisasiDosen filter jabatan, boleh {@code null}
	 * @param tahun                  filter tahun keanggotaan, boleh {@code null}
	 */
	public DosenPunyaOrganisasiDosenHelper(OrganisasiDosen organisasiDosen,
			JabatanOrganisasiDosen jabatanOrganisasiDosen, Integer tahun) {
		tbmuser = Common.getCurrentUser();
		this.organisasiDosen = organisasiDosen;
		this.jabatanOrganisasiDosen = jabatanOrganisasiDosen;
		this.tahun = tahun;
		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/** Row renderer grid: identitas dosen, nama organisasi, rentang tanggal/jabatan/keterangan (editable bagi pemilik data atau atasan langsungnya bila belum disetujui), unggahan SK, checkbox/label persetujuan, dan tombol hapus. */
	class DetailDosenRenderer extends ais.ui.util.MyRowRenderer {

		public DetailDosenRenderer() {
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) data;

			try {
				if (DosenPunyaOrganisasiDosenHelper.this.organisasiDosenPunyaDosen != null
						&& DosenPunyaOrganisasiDosenHelper.this.organisasiDosenPunyaDosen.getId()
								.equals(organisasiDosenPunyaDosen.getId())) {
					row.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DosenPunyaOrganisasiDosenHelper.java:121");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			A a = CommonMedia.tampilkanGambarKecil(organisasiDosenPunyaDosen.getDosen());
			a.setParent(vbox);
			vbox.appendChild(new MyLabelAgakKecil(organisasiDosenPunyaDosen.getDosen().getNama()));
			vbox.appendChild(new MyLabelAgakKecil(organisasiDosenPunyaDosen.getDosen().getNim()));
			vbox.appendChild(new MyLabelAgakKecil(organisasiDosenPunyaDosen.getDosen().getJurusan() == null ? ""
					: organisasiDosenPunyaDosen.getDosen().getJurusan().getNama()));

			Vbox aa = RevisiHelper.createNewRevisi(OrganisasiDosenPunyaDosen.class, organisasiDosenPunyaDosen,
					organisasiDosenPunyaDosen.getOrganisasiDosen().getNama());
			aa.setParent(row);
			aa.appendChild(new MyLabelAgakKecil(organisasiDosenPunyaDosen.getJabatanOrganisasiDosen() == null ? ""
					: organisasiDosenPunyaDosen.getJabatanOrganisasiDosen().getNama()));

			boolean bolehEdit = tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& tbmuser.getDosen().getId().equals(organisasiDosenPunyaDosen.getDosen().getId())
					&& !organisasiDosenPunyaDosen.getPersetujuan();

			boolean merupakanAtasanLangsung = (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& organisasiDosenPunyaDosen.getDosen() != null
					&& organisasiDosenPunyaDosen.getDosen().getAtasanlangsung() != null
					&& organisasiDosenPunyaDosen.getDosen().getAtasanlangsung().equals(tbmuser.getDosen().getId()));

			System.out.println("merupakanAtasanLangsung => " + merupakanAtasanLangsung);

			vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, organisasiDosenPunyaDosen.getId(),
					OrganisasiDosenPunyaDosen.class.getName(), "Surat Keputusan (SK) / Surat Keterangan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, bolehEdit || merupakanAtasanLangsung, null);
			hbox.setParent(vbox);

			if (bolehEdit || merupakanAtasanLangsung) {

				final MyDatebox mulai = new MyDatebox(organisasiDosenPunyaDosen.getMulai());
				mulai.setWidth("90%");
				final MyDatebox sampai = new MyDatebox(organisasiDosenPunyaDosen.getSampai());
				sampai.setWidth("90%");
				final MyTextbox keterangan = new MyTextbox(organisasiDosenPunyaDosen.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				mulai.setParent(row);
				sampai.setParent(row);

				final Combobox combobox = new Combobox();
				Common.insertCombo(combobox, "nama", JabatanOrganisasiDosen.class);
				Common.selectComboItem(combobox, organisasiDosenPunyaDosen.getJabatanOrganisasiDosen());
				combobox.setParent(row);
				combobox.setReadonly(true);
				combobox.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						organisasiDosenPunyaDosen.setMulai(mulai.getValue());
						organisasiDosenPunyaDosen.setSampai(sampai.getValue());
						organisasiDosenPunyaDosen.setKeterangan(keterangan.getValue());
						organisasiDosenPunyaDosen.setJabatanOrganisasiDosen(
								((JabatanOrganisasiDosen) (combobox.getSelectedItem() == null ? null
										: combobox.getSelectedItem().getValue())));
						Common.refreshUpdate(organisasiDosenPunyaDosen);
					}
				};

				combobox.addEventListener("onChange", eventListener);
				keterangan.addEventListener("onChange", eventListener);
				mulai.addEventListener("onChange", eventListener);
				sampai.addEventListener("onChange", eventListener);

				keterangan.setParent(row);

				final Hbox toolbar = new Hbox();
				toolbar.setVisible(!organisasiDosenPunyaDosen.getPersetujuan());
				combobox.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
				keterangan.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
				mulai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
				sampai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
				if ((tbmuser.ambilDosen() == null && tbmuser.ambilDosen().getAtasanlangsung() == null)
						|| merupakanAtasanLangsung) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(organisasiDosenPunyaDosen.getPersetujuan());
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							organisasiDosenPunyaDosen.setPersetujuan(checkbox.isChecked());
							Common.refreshSaveOrUpdate(organisasiDosenPunyaDosen);
							toolbar.setVisible(!organisasiDosenPunyaDosen.getPersetujuan());
							combobox.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
							keterangan.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
							mulai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
							sampai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
						}
					});
				} else {
					Label label;
					(label = new Label(organisasiDosenPunyaDosen.getPersetujuan() == null
							|| organisasiDosenPunyaDosen.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
					label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
					label.setParent(row);
				}

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setVisible(!organisasiDosenPunyaDosen.getPersetujuan());
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												try {
													if (DosenPunyaOrganisasiDosenHelper.this.organisasiDosenPunyaDosen != null
															&& DosenPunyaOrganisasiDosenHelper.this.organisasiDosenPunyaDosen
																	.getId()
																	.equals(organisasiDosenPunyaDosen.getId())) {
														DosenPunyaOrganisasiDosenHelper.this.organisasiDosenPunyaDosen = null;
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DosenPunyaOrganisasiDosenHelper.java:268");
													// TODO: handle exception
												}

												Common.refreshDelete(organisasiDosenPunyaDosen);
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(row);

			} else {
				new Label(organisasiDosenPunyaDosen.getMulai() == null ? ""
						: Common.dateFormat1.get().format(organisasiDosenPunyaDosen.getMulai())).setParent(row);
				new Label(organisasiDosenPunyaDosen.getSampai() == null ? ""
						: Common.dateFormat1.get().format(organisasiDosenPunyaDosen.getSampai())).setParent(row);
				new Label(organisasiDosenPunyaDosen.getJabatanOrganisasiDosen() == null ? ""
						: organisasiDosenPunyaDosen.getJabatanOrganisasiDosen().getNama()).setParent(row);

				new Label(organisasiDosenPunyaDosen.getKeterangan()).setParent(row);
				Label label;
				(label = new Label(
						organisasiDosenPunyaDosen.getPersetujuan() == null || organisasiDosenPunyaDosen.getPersetujuan()
								? "Ya"
								: "Belum"))
						.setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

		}

	}

	/**
	 * Membangun kriteria Hibernate {@link OrganisasiDosenPunyaDosen} sesuai filter tetap (organisasi/
	 * jabatan/tahun bila diberikan di konstruktor) ditambah filter dinamis (dosen, pencarian nama).
	 * Bila pengguna login adalah dosen, pencarian per-dosen juga mencakup baris bawahan langsungnya.
	 *
	 * @param order bila {@code true}, menambahkan pengurutan id menurun (terbaru dulu)
	 * @return kriteria siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Long loginAtasan = tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") ? tbmuser.getDosen().getId() : null;

		System.out.println("loginAtasan => " + loginAtasan);

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiDosenPunyaDosen.class);

		criteria.createAlias("organisasiDosen", "organisasiDosen").createAlias("dosen", "dosen")

				.add(jabatanOrganisasiDosen == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jabatanOrganisasiDosen", jabatanOrganisasiDosen))

				.add(organisasiDosen == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("organisasiDosen", organisasiDosen))

				.add(tahun == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahun", tahun))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("organisasiDosen.nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("dosen.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(dosen == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("dosen", dosen),
								Restrictions.eq("dosen.atasanlangsung", loginAtasan)));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat ulang halaman keanggotaan organisasi dosen saat ini. Bila field {@code organisasiDosenPunyaDosen}
	 * (baris yang sedang disorot, mis. hasil "Ambil Organisasi") sudah diisi, baris tersebut dipaksa
	 * tampil di posisi pertama daftar terlepas dari urutan pagingnya. Parameter {@code value} tidak
	 * dipakai.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<OrganisasiDosenPunyaDosen> myOrganisasiDosenPunyaDosens;

				if (organisasiDosenPunyaDosen != null) {
					myOrganisasiDosenPunyaDosens = new ArrayList<OrganisasiDosenPunyaDosen>();
					myOrganisasiDosenPunyaDosens.add(organisasiDosenPunyaDosen);
					myOrganisasiDosenPunyaDosens
							.addAll(initCriteria(true).add(Restrictions.ne("id", organisasiDosenPunyaDosen.getId()))
									.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
									.setFirstResult(
											Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
									.list());
				} else {
					myOrganisasiDosenPunyaDosens = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
				}

				ListModel strset = new SimpleListModel(myOrganisasiDosenPunyaDosens);
				grid.setRowRenderer(new DetailDosenRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	/** Seperti {@link #display(Dosen, Component, OrganisasiDosenPunyaDosen)} tanpa baris yang disorot khusus. */
	public void display(Dosen dosen, Component component) {
		display(dosen, component, null);
	}

	/**
	 * Membangun UI grid organisasi dosen (toolbar cari/ambil-organisasi/cetak/unduh) di dalam
	 * {@code component} dan memuat data awal.
	 *
	 * @param dosen                     dosen yang organisasinya ditampilkan (mode "organisasi milik
	 *                                  dosen"), boleh {@code null} bila helper dipakai mode "anggota
	 *                                  organisasi" (filter tetap dari konstruktor)
	 * @param component                 container ZK yang akan diisi
	 * @param organisasiDosenPunyaDosen baris yang perlu disorot/ditampilkan di posisi pertama, boleh
	 *                                  {@code null}
	 */
	public void display(final Dosen dosen, final Component component,
			OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen) {
		this.dosen = dosen;
		this.organisasiDosenPunyaDosen = organisasiDosenPunyaDosen;

		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 3200px;");
		groupbox.setParent(component);

		final boolean mobileTampil = Common.isMobile();
		org.zkoss.zk.ui.HtmlBasedComponent toolbar;
		if (mobileTampil) {
			org.zkoss.zul.Div barMobile = new org.zkoss.zul.Div();
			barMobile.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px 4px;width:100%;box-sizing:border-box;");
			toolbar = barMobile;
		} else {
			toolbar = new Toolbar();
		}
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Organisasi", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				MyWindow window = new MyWindow();
				window.setHeight("97%");
				window.setWidth("800px");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				AmbilDataOrganisasiForOrganisasiDosenHelper dataDosenHelper = new AmbilDataOrganisasiForOrganisasiDosenHelper(
						dosen);
				dataDosenHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		if (dosen != null) {

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Organisasi Dosen", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakOrganisasiDosen(dosen);
				}
			});
			cetak.setParent(toolbar);
		}

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("SK");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				/**
				 * Helper implementasi bersarang milik {@link DosenPunyaOrganisasiDosenHelper} untuk data adding helper. Kelas
				 * ini mengemas langkah lokal yang dipakai kelas induk dan bukan service domain alternatif.
				 *
				 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DosenPunyaOrganisasiDosenHelper} dan dapat
				 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
				 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code process}(). Aturan bisnis bersama
				 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
				 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
				 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
				 * tambahkan perilaku lintas domain pada service bersama.</p>
				 *
				 * @see DosenPunyaOrganisasiDosenHelper
				 */
				class DataAddingHelper {
					public void process(XSSFRow row, int index, OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen,
							String jenis) throws Exception {
						LampiranLain lam = LampiranLain.ambil(organisasiDosenPunyaDosen.getId(), jenis);
						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
									.createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}

						StreamingHibernateUtil.getInstance().closeSession();
					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 8, organisasiDosenPunyaDosen, OrganisasiDosenPunyaDosen.class.getName());

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(OrganisasiDosenPunyaDosen.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, "id", "organisasiDosen", "dosen",
				"jabatanOrganisasiDosen", "persetujuan", "mulai", "sampai", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		// SCROLL (pola Center->Grid->Rows->Row): grid dibungkus Borderlayout -> Center(autoscroll)
		// dgn tinggi terikat agar baris banyak / tabel lebar memunculkan scrollbar. Caption+toolbar
		// tetap di luar borderlayout (hindari North-collapse ZK5.5).
		ais.ui.util.MyBorderlayout blScroll = new ais.ui.util.MyBorderlayout();
		blScroll.setHeight("60vh");
		blScroll.setWidth("100%");
		blScroll.setStyle("min-height:280px;");
		blScroll.setParent(groupbox);
		org.zkoss.zul.Center centerScroll = new org.zkoss.zul.Center();
		centerScroll.setBorder("none");
		centerScroll.setAutoscroll(true);
		centerScroll.setParent(blScroll);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Organisasi");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

}
