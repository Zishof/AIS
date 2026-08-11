package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.sql.Blob;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranPengumumanAkademis;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailPengumumanAkademisHelper {

	private PengumumanAkademis pengumumanAkademis;

	private Boolean readonly = false;
	private Component center;

	public void displayDetailPengumuman(final PengumumanAkademis pengumumanAkademis, final Component component) {

		Vbox vbox3 = new Vbox();
		final DiskusiPengumumanAkademisHelper data = new DiskusiPengumumanAkademisHelper(vbox3, pengumumanAkademis);

		this.pengumumanAkademis = pengumumanAkademis;
		Common.clear(component);

		MyGroupboxStyled groupbox = new MyGroupboxStyled();
		groupbox.appendChild(new MyCaptionStyled("Komentar Pengumuman Akademik"));
		groupbox.setWidth("95%");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				pengumumanAkademis.belum("diskusi");
				Common.createDefaultTimer(data);
			}

		});
		button.setParent(toolbar);

		vbox3.setParent(groupbox);

		Common.createDefaultTimer(data);

	}

	public void displayAttachment(final PengumumanAkademis pengumumanAkademis, final Component component,
			final MyWindow window) {
		this.pengumumanAkademis = pengumumanAkademis;

		// PENTING: dulu memakai Borderlayout (North=toolbar, Center=isi). Borderlayout ZK TIDAK ter-render
		// dengan benar bila DIBANGUN DINAMIS (setelah halaman tampil) lalu di-append ke Tabpanel — region-nya
		// kolaps ke tinggi 0 sehingga tab (Lampiran/Galeri saat dipilih) tampak KOSONG walau tombol & isi
		// sudah dibuat. Diganti Vbox sederhana (toolbar di atas, isi di bawah) yang ter-render normal walau
		// dibangun dinamis saat tab dipilih.
		Vbox wrap = new Vbox();
		wrap.setWidth("100%");
		wrap.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!readonly);
		toolbar.setParent(wrap);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Tambah Gambar / Lampiran" + Common.ukuranLabelFileUpload(), "/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				Blob blob = Common.getBlobFromMedia(media, session);
				LampiranPengumumanAkademis lampiranPengumumanAkademis = new LampiranPengumumanAkademis();
				lampiranPengumumanAkademis.setFoto(blob);
				lampiranPengumumanAkademis.setMimeType(media.getContentType());
				lampiranPengumumanAkademis.setNama(media.getName());
				lampiranPengumumanAkademis.setPengumumanAkademis(pengumumanAkademis);
				lampiranPengumumanAkademis.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.save(lampiranPengumumanAkademis);

				loadDataAttachment();
			}

		});
		button.setParent(toolbar);

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setParent(wrap);
		center = isi;

		loadDataAttachment();

	}

	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Common.clear(center);

		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanAkademis> lampiranPengumumanAkademis = new java.util.ArrayList<LampiranPengumumanAkademis>();
		if (pengumumanAkademis != null && pengumumanAkademis.getId() != null) {
			lampiranPengumumanAkademis = session.createCriteria(LampiranPengumumanAkademis.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).list();
		}

		if (lampiranPengumumanAkademis.isEmpty()) {
			// Belum ada lampiran (mis. pengumuman baru) — tampilkan petunjuk, jangan biarkan kosong.
			Label kosong = new Label(
					"Belum ada lampiran. Klik \"Tambah Gambar / Lampiran\" di atas untuk menambahkan.");
			kosong.setStyle("color:#64748b;padding:10px 4px;display:block;");
			kosong.setParent(center);
		} else if (lampiranPengumumanAkademis.size() == 1) {
			try {
				tampilkanKonten(center, lampiranPengumumanAkademis.get(0));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailPengumumanAkademisHelper.java:145");
			}
		} else {

			Tabbox tabbox = new Tabbox();
			if (Common.isMobile()) {
				tabbox.setMold("accordion");
			}
			tabbox.setParent(center);
			tabbox.setHeight("3000px");
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			int index = 0;
			for (final LampiranPengumumanAkademis pengumumanAkademis : lampiranPengumumanAkademis) {

				String n = pengumumanAkademis.getNama();

				Tab tab;
				tabs.appendChild(tab = new Tab(n.length() > 30 ? n.substring(0, 30) : n, FileFoto.icon(n)));
				final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanels.appendChild(tabpanelUtama);
				tabpanelUtama.setHeight("2000px");
				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelUtama.getChildren().isEmpty()) {
							tampilkanKonten(tabpanelUtama, pengumumanAkademis);
						}
					}
				};

				tab.addEventListener("onClick", eventListener);

				if (index == 0) {
					try {
						tab.setSelected(true);
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailPengumumanAkademisHelper.java:189");
					}
				}

				index++;
			}
		}
	}

	private void tampilkanKonten(Component tabpanelUtama, final LampiranPengumumanAkademis lampiranPengumumanAkademis)
			throws Exception {
		Vbox vbox = new Vbox();
		vbox.setParent(tabpanelUtama);

		vbox.setWidth("100%");

		new Label(Common.dateFormat.get().format(lampiranPengumumanAkademis.getUploadDate())).setParent(vbox);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig("Ditampilkan");
		checkbox.setChecked(lampiranPengumumanAkademis.getDitampilkan());
		checkbox.setParent(vbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String sql = "update lampiran_pengumuman_akademis set ditampilkan=" + checkbox.isChecked()
						+ " where id = " + lampiranPengumumanAkademis.getId();
				HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(lampiranPengumumanAkademis.getNama(),
				lampiranPengumumanAkademis.iconDonwload());
		toolbarbutton.setParent(hbox);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				LampiranPengumumanAkademis content = (LampiranPengumumanAkademis) HibernateUtil.currentSession()
						.createCriteria(LampiranPengumumanAkademis.class)
						.add(Restrictions.idEq(lampiranPengumumanAkademis.getId())).setMaxResults(1).uniqueResult();

				Filedownload.save(content.ambilFile(), lampiranPengumumanAkademis.getMimeType());
			}

		});

		toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		toolbarbutton.setTooltiptext("Hapus Data");
		toolbarbutton.setVisible(!readonly);
		toolbarbutton.setParent(hbox);
		toolbarbutton.addEventListener("onClick", new EventListener() {
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
										Common.refreshDelete(lampiranPengumumanAkademis);

										loadDataAttachment();
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});

		CommonMedia.preview(lampiranPengumumanAkademis, vbox);
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
