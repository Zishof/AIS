package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Komponen "banbox" (bandbox pencarian popup) untuk memilih satu {@link CalonSiswa} (calon
 * siswa PSB yang sudah memiliki gelombang pendaftaran) dari daftar bergaya modal, dengan filter
 * kode/nama/yayasan/sekolah dan paginasi 5 baris per halaman (dipilih kecil demi efisiensi
 * RAM/jaringan — lihat {@link #PAGE_SIZE_LIMA}). Bila user yang login adalah calon siswa itu
 * sendiri, komponen langsung terisi dan dinonaktifkan (tidak bisa memilih calon siswa lain). Bila
 * user adalah orang tua dengan anak siswa terdaftar, daftar dibatasi ke anak-anaknya saja. Setiap
 * pemanggilan {@link #onSearchDefault(Event)} membuka dan menutup sesi Hibernate sendiri secara
 * mandiri (independen dari sesi thread-local biasa).
 */
public class AmbilDataCalonSiswaBanbox extends Bandbox implements GetEventListener {

	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;
	private Paging paging;

	private EventListener eventListener;

	// Pembatasan data 5 per halaman untuk efisiensi RAM/Jaringan
	private static final int PAGE_SIZE_LIMA = 5;

	private Textbox kode;
	private Textbox nama;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	/** Membuat komponen dengan pengisian otomatis aktif bila user yang login adalah calon siswa (lihat {@link #AmbilDataCalonSiswaBanbox(Boolean)}). */
	public AmbilDataCalonSiswaBanbox() {
		this(true);
	}

	/**
	 * @param notDeafault bila {@code true} DAN user yang login memiliki data calon siswa,
	 *                     komponen langsung terisi dengan calon siswa tersebut dan dinonaktifkan
	 */
	public AmbilDataCalonSiswaBanbox(Boolean notDeafault) {
		super();
		setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getCalonSiswa() != null && notDeafault) {
			CalonSiswa calonSiswa = tbmuser.getCalonSiswa();
			setValue(calonSiswa.getNama());
			setAttribute("myValue", calonSiswa);
			setAttribute("calonSiswa", calonSiswa);
			setDisabled(true);
		}

		addEventListener("onOpen", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	/** Renderer baris hasil pencarian: radio pilih (memilih calon siswa, menutup popup, dan memicu {@code eventListener}), foto kecil, nomor induk, nama, tempat/tanggal lahir, dan gelombang+jurusan pendaftaran. */
	class CalonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CalonSiswa calonSiswa = (CalonSiswa) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataCalonSiswaBanbox.this.setOpen(false);
					AmbilDataCalonSiswaBanbox.this.setAttribute("calonSiswa", calonSiswa);
					AmbilDataCalonSiswaBanbox.this.setAttribute("myValue", calonSiswa);
					AmbilDataCalonSiswaBanbox.this.setValue(calonSiswa.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(arg0);
			new Label(calonSiswa.getNomorInduk()).setParent(arg0);

			new Label(calonSiswa.getNamaSiswa()).setParent(arg0);
			new Label(calonSiswa.getTempatLahir() + "," + (calonSiswa.getTanggalLahir() == null ? ""
					: Common.dateFormat1.get().format(calonSiswa.getTanggalLahir()))).setParent(arg0);

			new Label((calonSiswa.getGelombangPendaftaranPsb() == null ? ""
					: calonSiswa.getGelombangPendaftaranPsb().getNama())
					+ (calonSiswa.getPenjurusanSekolah() == null ? ""
							: " " + calonSiswa.getPenjurusanSekolah().getNama()))
					.setParent(arg0);
		}
	}

	/**
	 * Menyusun konten popup bandbox (dipanggil sekali saat pertama dibuka): panel pencarian
	 * (kode, nama, yayasan, sekolah) dengan tombol cari/bersihkan dan toggle filter, grid hasil
	 * dengan kolom pilih/foto/no. reg/nama/tempat-tanggal lahir/gelombang, serta kontrol paginasi,
	 * lalu memuat data awal lewat {@link #onSearchDefault(Event)}.
	 */
	public void display() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1050px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar CalonSiswa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		// Inisialisasi Paging
		paging = new Paging();
		paging.setPageSize(PAGE_SIZE_LIMA);
		paging.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		EventListener searchEvent = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				paging.setActivePage(0); // Reset ke Hal-1 tiap pencarian baru
				onSearchDefault(null);
			}
		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");
		kode.addEventListener("onOK", searchEvent);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", searchEvent);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", searchEvent);
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		// Layout grid + Paging diletakkan pada South Container
		Borderlayout gridLayout = new ais.ui.util.MyBorderlayout();
		gridLayout.setParent(center);
		
		Center centerGrid = new Center();
		ais.ui.util.ZkCompat.setFlex(centerGrid, true);
		centerGrid.setParent(gridLayout);
		
		South southGrid = new South();
		southGrid.setParent(gridLayout);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(centerGrid);
		
		paging.setParent(southGrid); // Menempelkan Paging control di bagian bawah tabel

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Reg");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tempat, Tanggal Lahir");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Gelombang");
		column.setWidth("20%");

		onSearchDefault(null);
	}

	/**
	 * @param session sesi Hibernate aktif
	 * @param isOrder tambahkan pengurutan (tahun masuk menurun, nomor induk menaik) bila {@code true}
	 * @return kriteria pencarian {@link CalonSiswa} (harus memiliki gelombang pendaftaran, nama
	 *         terisi, aktif), dibatasi ke anak-anak user bila user login adalah orang tua, dan
	 *         difilter sesuai isian kode/nama/yayasan/sekolah pada formulir
	 */
	public Criteria initCriteria(Session session, boolean isOrder) {
		Criteria criteria = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
				.add(Restrictions.isNotNull("nama"))
				.add(Restrictions.ne("nama", ""))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (isOrder) {
			criteria.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nomorInduk"));
		}

		criteria.add(nama == null || nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.ilike("namaSiswa", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(kode == null || kode.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.ilike("nomorInduk", kode.getText().trim(), MatchMode.ANYWHERE))
				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	/**
	 * Memuat ulang grid hasil pencarian sesuai halaman aktif dan filter formulir saat ini,
	 * membuka sesi Hibernate baru secara mandiri (bukan sesi thread-local) dan selalu menutupnya
	 * di {@code finally}. Menghitung total baris lebih dulu untuk mengatur kontrol paginasi,
	 * lalu mengambil hanya {@link #PAGE_SIZE_LIMA} baris untuk halaman aktif.
	 *
	 * @param event event pemicu (boleh {@code null}, tidak dipakai)
	 */
	@SuppressWarnings({ })
	public void onSearchDefault(Event event) {

		Session session = null;
		try {
			// WAJIB buka session baru dan tutup di finally
			session = HibernateUtil.getSessionFactory().openSession();

			// 1. Dapatkan Total Data untuk Setup Pagination
			Criteria countCriteria = initCriteria(session, false);
			countCriteria.setProjection(Projections.rowCount());
			Long totalData = (Long) countCriteria.uniqueResult();
			
			paging.setTotalSize(totalData != null ? totalData.intValue() : 0);
			paging.setMold("os");
			paging.setDetailed(true);
			// 2. Tentukan Offset Paging dan Tarik Hanya 5 Data Per Halaman
			int activePage = paging.getActivePage() < 0 ? 0 : paging.getActivePage();
			int startOffset = activePage * PAGE_SIZE_LIMA;

			Criteria listCriteria = initCriteria(session, true);
			List<CalonSiswa> calonSiswa =  ConstantValues.simpleList(listCriteria
					.setFirstResult(startOffset)
					.setMaxResults(PAGE_SIZE_LIMA), CalonSiswa.class);
			ListModel strset = new SimpleListModel(calonSiswa);
			grid.setRowRenderer(new CalonSiswaRenderer());
			grid.setModelCheckMobile(strset);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AmbilDataCalonSiswaBanbox.java:343");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataCalonSiswaBanbox.java:346");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataCalonSiswaBanbox.java:347");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataCalonSiswaBanbox.java:348");}
			}
		}
	}

	/** @param eventListener dipanggil setiap kali user memilih satu calon siswa dari daftar */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan calon siswa yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}