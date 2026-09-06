package ais.action.master.recruitment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.CalonPegawaiPunyaDokumen;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk calon pegawai punya dokumen. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnama}, {@code Textbox searchcalnama}, {@code Combobox searchstatus}, {@code
 * MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code ambilLinkDariKeterangan()}, {@code ambilLinkLampiran()},
 * {@code tampilkanLinkLihatPrint()}, {@code ambilLampiranDokumen()}, {@code ambilLampiranUtama()}, {@code
 * onSearchDefault()}); operasi domain lain ({@code nvl()}, {@code empty()}, {@code bukaLampiranDiWindowBaru()},
 * {@code filterDokumenUtamaSql()}, {@code keyDokumen()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class CalonPegawaiPunyaDokumenAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchcalnama;

	private Combobox searchstatus;

	private MyToolbarbuttonConfig find;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Comboitem comboitem = new Comboitem(CalonPegawaiPunyaDokumen.BELUM);
		if (comboitem != null) { comboitem.setValue(CalonPegawaiPunyaDokumen.BELUM); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(CalonPegawaiPunyaDokumen.VERIFIKASI);
		if (comboitem != null) { comboitem.setValue(CalonPegawaiPunyaDokumen.VERIFIKASI); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(CalonPegawaiPunyaDokumen.REVISI);
		if (comboitem != null) { comboitem.setValue(CalonPegawaiPunyaDokumen.REVISI); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }
		if (searchstatus != null) { searchstatus.setWidth("90%"); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "calonPegawai.nomorInduk", "calonPegawai.nama",
				"verifikasiKelengkapanCalonPegawai.nomorInduk", "verifikasiKelengkapanCalonPegawai.nama",
				"verifikasiKelengkapanCalonPegawai.wajib", "verifikasiKelengkapanCalonPegawai.status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<CalonPegawaiPunyaDokumen> calonPegawaiPunyaDokumens = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());
				File folderOut = new File(Common.REAL_PATH + "/media/");
				try {
					folderOut.mkdirs();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/recruitment/CalonPegawaiPunyaDokumenAction.java:126");
					// TODO: handle exception
				}
				for (CalonPegawaiPunyaDokumen calonPegawaiPunyaDokumen : calonPegawaiPunyaDokumens) {
					LampiranLain lam = LampiranLain.ambil(calonPegawaiPunyaDokumen.getId(),
							CalonPegawaiPunyaDokumen.class.getName());
					if (lam != null) {
						File file;
						if (lam.getGdrive() != null && !lam.getGdrive().trim().isEmpty()) {
							file = new File(folderOut.getAbsolutePath() + "/"
									+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
							FileUtils.writeStringToFile(file, lam.forwardGDriveUrl());
						} else if (lam.getLink() != null && !lam.getLink().trim().isEmpty()) {
							file = new File(folderOut.getAbsolutePath() + "/"
									+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
							FileUtils.writeStringToFile(file, lam.getLink().trim());
						} else {
							file = lam.ambilFile();
						}

						File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
								+ URLEncoder.encode(calonPegawaiPunyaDokumen.getCalonPegawai().getNomorInduk() + "_"
										+ calonPegawaiPunyaDokumen.getCalonPegawai().getNama(), "UTF-8")
								+ "_" + file.getName());
						System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
						FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
						FileInputStream fileInputStream = new FileInputStream(file);
						IOUtils.copyLarge(fileInputStream, fileOutputStream);
						fileInputStream.close();
						fileOutputStream.close();
					}
				}
				calonPegawaiPunyaDokumens.clear();
				calonPegawaiPunyaDokumens = null;
				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");
			}
		});
		Common.appendKeToolbar(downloadLampiran, find, comp);
	}


	private String nvl(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean empty(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String ambilLinkDariKeterangan(String keterangan) {
		keterangan = nvl(keterangan);
		int p = keterangan.indexOf("FILE:");
		if (p < 0) {
			return "";
		}
		int end = keterangan.indexOf("|", p);
		return end > p ? keterangan.substring(p + 5, end).trim() : keterangan.substring(p + 5).trim();
	}

	private String ambilLinkLampiran(LampiranLain lampiranLain, CalonPegawaiPunyaDokumen dokumen) {
		try {
			if (lampiranLain != null) {
				/*
				 * Cara standar mendapatkan URL LampiranLain adalah memakai createLinkUri().
				 * Ini dipakai agar link pada dataCalonPegawaiPunyaDokumen sama dengan
				 * pola media/lampiran bawaan sistem.
				 */
				try {
					String url = lampiranLain.createLinkUri();
					if (url != null && !url.trim().isEmpty()) {
						return url.trim();
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				if (lampiranLain.getLink() != null && !lampiranLain.getLink().trim().isEmpty()) {
					return lampiranLain.getLink().trim();
				}
				if (lampiranLain.getGdrive() != null && !lampiranLain.getGdrive().trim().isEmpty()) {
					return lampiranLain.forwardGDriveUrl();
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return ambilLinkDariKeterangan(dokumen == null ? "" : dokumen.getKeterangan());
	}

	private void tampilkanLinkLihatPrint(Vbox parent, LampiranLain lampiranLain, CalonPegawaiPunyaDokumen dokumen) {
		String link = ambilLinkLampiran(lampiranLain, dokumen);
		if (empty(link)) {
			return;
		}

		final String linkFinal = link.trim();
		A a = new A("Lihat / Print Dokumen");
		a.setStyle("display:inline-block;margin-top:6px;font-weight:bold;color:#2563eb;text-decoration:none;cursor:pointer;");
		a.setTooltiptext("Buka dokumen lampiran pada jendela baru");
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaLampiranDiWindowBaru(linkFinal);
			}
		});
		a.setParent(parent);
	}

	private void bukaLampiranDiWindowBaru(String link) {
		if (empty(link)) {
			return;
		}
		if (Common.isMobile()) {
			ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
		} else {
			String safeUrl = Common.jsEscape(link);
			Clients.evalJavaScript("popupCenter({url: '" + safeUrl + "', title: 'Lampiran', w: 1200, h: 650});");
		}
	}

	private String filterDokumenUtamaSql() {
		return "{alias}.id = (select min(d2.id) from public.calon_pegawai_punya_dokumen d2 "
				+ "where d2.calon_pegawai = {alias}.calon_pegawai "
				+ "and d2.verifikasi_kelengkapan_calon_pegawai = {alias}.verifikasi_kelengkapan_calon_pegawai)";
	}

	private LampiranLain ambilLampiranDokumen(CalonPegawaiPunyaDokumen dokumen) {
		if (dokumen == null || dokumen.getId() == null) {
			return null;
		}
		try {
			return LampiranLain.ambil(dokumen.getId(), CalonPegawaiPunyaDokumen.class.getName());
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private LampiranLain ambilLampiranUtama(CalonPegawaiPunyaDokumen dokumen) {
		LampiranLain lampiranLain = ambilLampiranDokumen(dokumen);
		if (lampiranLain != null) {
			return lampiranLain;
		}
		try {
			Session session = HibernateUtil.currentSession();
			CalonPegawai calonPegawai = dokumen.getCalonPegawai();
			Object template = dokumen.getVerifikasiKelengkapanCalonPegawai();
			if (calonPegawai == null || template == null) {
				return null;
			}
			List<CalonPegawaiPunyaDokumen> dokumens = session.createCriteria(CalonPegawaiPunyaDokumen.class)
					.add(Restrictions.eq("calonPegawai", calonPegawai))
					.add(Restrictions.eq("verifikasiKelengkapanCalonPegawai", template))
					.addOrder(Order.asc("id")).list();
			for (CalonPegawaiPunyaDokumen d : dokumens) {
				lampiranLain = ambilLampiranDokumen(d);
				if (lampiranLain != null) {
					return lampiranLain;
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	private String keyDokumen(CalonPegawaiPunyaDokumen dokumen) {
		try {
			Long calonId = dokumen.getCalonPegawai().getId();
			Long verifikasiId = dokumen.getVerifikasiKelengkapanCalonPegawai().getId();
			return calonId + "_" + verifikasiId;
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			return String.valueOf(dokumen == null || dokumen.getId() == null ? 0 : dokumen.getId());
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CalonPegawaiPunyaDokumenAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CalonPegawaiPunyaDokumenAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CalonPegawaiPunyaDokumenAction
	 */
	class CalonPegawaiPunyaDokumenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CalonPegawaiPunyaDokumen calonPegawaiPunyaDokumen = (CalonPegawaiPunyaDokumen) arg1;
			CalonPegawai calonPegawai = calonPegawaiPunyaDokumen.getCalonPegawai();
			if (calonPegawai == null || calonPegawai.getId() == null) {
				new Label(ais.common.Common.getBahasaConfig("Data calon pegawai tidak ditemukan")).setParent(arg0);
				return;
			}

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(arg0);

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(CalonPegawai.class, calonPegawai, calonPegawai.getNama()))
					.setParent(arg0);
			aa.appendChild(new MyLabelAgakKecilBold(calonPegawai.getKeterangan()));

			calonPegawai.tampilkanHp(aa);
			calonPegawai.tampilkanEmail(aa);

			String namaDokumen = calonPegawaiPunyaDokumen.getVerifikasiKelengkapanCalonPegawai() == null ? "Dokumen"
					: calonPegawaiPunyaDokumen.getVerifikasiKelengkapanCalonPegawai().getNama();
			(RevisiHelper.createNewRevisi(CalonPegawaiPunyaDokumen.class, calonPegawaiPunyaDokumen, namaDokumen))
					.setParent(arg0);

			final Vbox myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			hbox.setWidth("100%");

			Boolean tampilUpload = !calonPegawaiPunyaDokumen.getStatus()
					.equalsIgnoreCase(CalonPegawaiPunyaDokumen.VERIFIKASI);

			LampiranLain.createDownloadUploadFileLain(hbox, calonPegawaiPunyaDokumen.getId(),
					CalonPegawaiPunyaDokumen.class.getName(),
					namaDokumen, false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, tampilUpload);

			LampiranLain lampiranLain = ambilLampiranUtama(calonPegawaiPunyaDokumen);
			tampilkanLinkLihatPrint(myvbox, lampiranLain, calonPegawaiPunyaDokumen);

			final Textbox keterangan = new Textbox(calonPegawaiPunyaDokumen.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			new Label(calonPegawaiPunyaDokumen.getVerifikasiKelengkapanCalonPegawai() != null
					&& calonPegawaiPunyaDokumen.getVerifikasiKelengkapanCalonPegawai().getWajib() ? "Ya" : "Tidak")
					.setParent(arg0);

			Combobox combobox = new Combobox();
			Comboitem comboitem = new Comboitem(CalonPegawaiPunyaDokumen.BELUM);
			comboitem.setValue(CalonPegawaiPunyaDokumen.BELUM);
			combobox.appendChild(comboitem);
			comboitem = new Comboitem(CalonPegawaiPunyaDokumen.VERIFIKASI);
			comboitem.setValue(CalonPegawaiPunyaDokumen.VERIFIKASI);
			combobox.appendChild(comboitem);
			comboitem = new Comboitem(CalonPegawaiPunyaDokumen.REVISI);
			comboitem.setValue(CalonPegawaiPunyaDokumen.REVISI);
			combobox.appendChild(comboitem);
			Common.selectComboItem(combobox, calonPegawaiPunyaDokumen.getStatus());
			combobox.setWidth("90%");
			combobox.setReadonly(true);
			arg0.appendChild(combobox);

			combobox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Combobox combobox = (Combobox) arg0.getTarget();
					calonPegawaiPunyaDokumen.setStatus((String) combobox.getSelectedItem().getValue());
					if (calonPegawaiPunyaDokumen.getId() != null) {
						Common.refreshUpdate(calonPegawaiPunyaDokumen);
					}
				}
			});

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					calonPegawaiPunyaDokumen.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(calonPegawaiPunyaDokumen);

				}
			};

			keterangan.addEventListener("onChange", eventListener);
		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(CalonPegawaiPunyaDokumen.class)

				.add(searchstatus == null || searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.createAlias("calonPegawai", "calonPegawai")

				.createAlias("verifikasiKelengkapanCalonPegawai", "verifikasiKelengkapanCalonPegawai")

				.add(Restrictions.or(Restrictions.isNull("verifikasiKelengkapanCalonPegawai.aktif"),
						Restrictions.eq("verifikasiKelengkapanCalonPegawai.aktif", true)))

				.add(Restrictions.sqlRestriction(filterDokumenUtamaSql()))

		;

		if (order)
			criteria.addOrder(Order.asc("calonPegawai.nama")).addOrder(Order.asc("verifikasiKelengkapanCalonPegawai.nama")).addOrder(Order.asc("id"));
		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("verifikasiKelengkapanCalonPegawai.nama", searchnama.getValue().trim(),
						MatchMode.ANYWHERE))

				.add(searchcalnama == null || searchcalnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("calonPegawai.nama", searchcalnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("calonPegawai.nomorInduk", searchcalnama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("calonPegawai.nomorInduk", searchcalnama.getValue().trim(),
												MatchMode.ANYWHERE))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CalonPegawaiPunyaDokumen> calonPegawaiPunyaDokumenRaw = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE * 3)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		List<CalonPegawaiPunyaDokumen> calonPegawaiPunyaDokumen = new ArrayList<CalonPegawaiPunyaDokumen>();
		Set<String> keys = new HashSet<String>();
		for (CalonPegawaiPunyaDokumen dokumen : calonPegawaiPunyaDokumenRaw) {
			String key = keyDokumen(dokumen);
			if (keys.contains(key)) {
				continue;
			}
			keys.add(key);
			calonPegawaiPunyaDokumen.add(dokumen);
			if (calonPegawaiPunyaDokumen.size() >= Common.ROWS_COUNT_ON_PAGE) {
				break;
			}
		}
		ListModel strset = new SimpleListModel(calonPegawaiPunyaDokumen);
		grid.setRowRenderer(new CalonPegawaiPunyaDokumenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
