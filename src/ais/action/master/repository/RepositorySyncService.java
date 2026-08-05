package ais.action.master.repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceInformation;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;
import ais.ui.dspace.DspaceCommon;

public class RepositorySyncService {

	public static final String STATUS_SYNCED = "SYNCED";
	public static final String STATUS_FAILED = "FAILED";
	public static final String STATUS_DRAFT = "DRAFT";

	private static final int DEFAULT_BATCH_SIZE = 25;

	private static class SourceDescriptor {
		private String label;
		private String collectionCode;
		private String collectionName;
		private String modelClassName;
		private String actionClassName;
		private String documentType;

		SourceDescriptor(String label, String collectionCode, String collectionName, String modelClassName,
				String actionClassName, String documentType) {
			this.label = label;
			this.collectionCode = collectionCode;
			this.collectionName = collectionName;
			this.modelClassName = modelClassName;
			this.actionClassName = actionClassName;
			this.documentType = documentType;
		}
	}

	public static class SyncSummary {
		private int scanned;
		private int synced;
		private int failed;
		private String message = "";

		public int getScanned() { return scanned; }
		public int getSynced() { return synced; }
		public int getFailed() { return failed; }
		public String getMessage() { return message == null ? "" : message; }
	}

	private static List<SourceDescriptor> getDefaultSources() {
		List<SourceDescriptor> sources = new ArrayList<SourceDescriptor>();
		sources.add(new SourceDescriptor("Skripsi/Tugas Akhir", "SKRIPSI", "Skripsi, Thesis, Disertasi, dan Tugas Akhir",
				"ais.database.model.Skripsi", "ais.action.master.SkripsiAction", "Thesis"));
		sources.add(new SourceDescriptor("Perpustakaan", "LIBRARY_ITEM", "Koleksi Perpustakaan",
				"ais.database.model.library.Item", "ais.action.master.library.ItemAction", "Book"));
		sources.add(new SourceDescriptor("Buku Bahan Ajar", "BUKU_BAHAN_AJAR", "Buku Bahan Ajar",
				"ais.database.model.BukuBahanAjar", "ais.action.master.BukuBahanAjarAction", "Book"));
		sources.add(new SourceDescriptor("Artikel", "ARTIKEL", "Artikel Penelitian dan Pengabdian",
				"ais.database.model.penelitiandanpengabdian.Artikel",
				"ais.action.master.penelitiandanpengabdian.ArtikelAction", "Article"));
		return sources;
	}

	public static SyncSummary synchronizeAll(boolean pushToDspace, boolean updateDspace) {
		return synchronizeAll(pushToDspace, updateDspace, null);
	}

	public static SyncSummary synchronizeAll(boolean pushToDspace, boolean updateDspace,
			ais.common.LaporanUpload laporan) {
		SyncSummary summary = new SyncSummary();
		String cookie = "";
		if (pushToDspace) {
			try {
				cookie = DspaceCommon.login();
			} catch (Exception e) {
				pushToDspace = false;
				summary.message = "Login DSpace gagal, sinkron lokal tetap diproses: " + e.getMessage();
				if (laporan != null) {
					laporan.tambahCatatan("Login DSpace gagal, sinkron lokal tetap diproses: "
							+ ais.common.LaporanUpload.detailTeknisException(e));
				}
			}
		}

		int[] baris = new int[] { 0 };
		List<SourceDescriptor> sources = getDefaultSources();
		for (SourceDescriptor source : sources) {
			SyncSummary part = synchronizeSource(source, cookie, pushToDspace, updateDspace, laporan, baris);
			summary.scanned += part.scanned;
			summary.synced += part.synced;
			summary.failed += part.failed;
			if (part.message != null && part.message.trim().length() > 0) {
				summary.message += (summary.message.length() == 0 ? "" : "\n") + part.message;
			}
		}
		return summary;
	}

	@SuppressWarnings("unchecked")
	private static SyncSummary synchronizeSource(SourceDescriptor source, String cookie, boolean pushToDspace,
			boolean updateDspace, ais.common.LaporanUpload laporan, int[] baris) {
		SyncSummary summary = new SyncSummary();
		Session session = HibernateUtil.currentSession();
		try {
			Class<?> modelClass = Class.forName(source.modelClassName);
			Long collectionId = ensureCollection(session, source).getId();
			Number count = (Number) session.createCriteria(modelClass).setProjection(Projections.rowCount())
					.uniqueResult();
			int total = count == null ? 0 : count.intValue();
			for (int first = 0; first < total; first += DEFAULT_BATCH_SIZE) {
				Criteria criteria = session.createCriteria(modelClass).setFirstResult(first)
						.setMaxResults(DEFAULT_BATCH_SIZE);
				List<Object> data = criteria.list();
				for (Object obj : data) {
					summary.scanned++;
					String kunci = "[" + source.label + "] " + String.valueOf(obj);
					try {
						RepoItem repoItem = syncOne(session, source, collectionId, obj, cookie, pushToDspace, updateDspace);
						if (repoItem != null && STATUS_FAILED.equals(repoItem.getSyncStatus())) {
							summary.failed++;
							if (laporan != null) {
								laporan.catatGagal(baris[0], kunci, repoItem.getSyncMessage());
							}
						} else {
							summary.synced++;
							if (laporan != null) {
								laporan.catatBerhasil(baris[0], kunci, "Sinkronisasi berhasil");
							}
						}
					} catch (Exception e) {
						summary.failed++;
						Common.tampilErrorJikaAdmin(e);
						if (laporan != null) {
							laporan.catatGagalDetail(baris[0], kunci, e);
						}
					}
					baris[0]++;
				}
				session.flush();
				session.clear();
			}
		} catch (Exception e) {
			summary.message = source.label + " gagal diproses: " + e.getMessage();
			Common.tampilErrorJikaAdmin(e);
			if (laporan != null) {
				laporan.tambahCatatan("Sumber " + source.label + " gagal diproses total: "
						+ ais.common.LaporanUpload.detailTeknisException(e));
			}
		}
		return summary;
	}

	private static RepoItem syncOne(Session session, SourceDescriptor source, Long collectionId, Object obj,
			String cookie, boolean pushToDspace, boolean updateDspace) throws Exception {
		Long sourceId = getLong(obj, "getId");
		if (sourceId == null) {
			return null;
		}
		RepoItem item = findRepoItem(session, source.modelClassName, sourceId);
		if (item == null) {
			item = new RepoItem();
			item.setSourceClass(source.modelClassName);
			item.setSourceId(sourceId);
			item.setCollectionId(collectionId);
			item.setSubmittedAt(new Date());
		}

		item.setSourceLabel(source.label);
		item.setDocumentType(source.documentType);
		item.setTitle(firstNotEmpty(invokeString(obj, "getJudul"), invokeString(obj, "getNama"), obj.toString()));
		item.setAbstractText(firstNotEmpty(invokeString(obj, "getAbstrack"), invokeString(obj, "getAbstrak"),
				invokeString(obj, "getAbstractText")));
		item.setSubjects(firstNotEmpty(invokeString(obj, "getKeyword"), invokeString(obj, "getKewords"),
				invokeString(obj, "getKategories"), invokeString(obj, "getSubjects")));
		item.setAuthors(firstNotEmpty(invokeString(obj, "getPengarangs"), invokeNestedString(obj, "getMahasiswa", "getNama"),
				invokeNestedString(obj, "getTbmuser", "getUserName")));
		item.setPublisher(firstNotEmpty(invokeNestedString(obj, "getMahasiswa", "getJurusan", "getNama"),
				invokeString(obj, "getPenerbit")));
		item.setIssuedAt(firstDate(obj, "getTanggalSidang", "getTanggalPublikasi", "getTanggalterbit", "getTanggal"));
		item.setAccessPolicy(Common.getKonfigurasi("repository_default_access_policy", "OPEN_ACCESS").getNilai());
		item.setTurnitinIndexed(Common.getKonfigurasi("repository_turnitin_index_default", "Aktif").getNilai()
				.equalsIgnoreCase("Aktif"));
		if (item.getTurnitinIndexed() && item.getTurnitinIndexedAt() == null) {
			item.setTurnitinIndexedAt(new Date());
		}

		if (pushToDspace) {
			try {
				DspaceInformation info = invokeDspace(source.actionClassName, cookie, obj, updateDspace);
				if (info != null) {
					item.setDspaceUuid(info.getUuid());
					item.setDspaceHandle(info.getLink());
					item.setOaiIdentifier(info.getLink());
				}
				item.setSyncStatus(STATUS_SYNCED);
				item.setSyncMessage("Sinkron repository berhasil.");
			} catch (Exception e) {
				item.setSyncStatus(STATUS_FAILED);
				item.setSyncMessage(e.getMessage());
			}
		} else {
			item.setSyncStatus(STATUS_SYNCED);
			item.setSyncMessage("Sinkron lokal berhasil. Push DSpace tidak dijalankan.");
		}
		item.setLastSyncAt(new Date());
		Common.refreshSaveOrUpdate(session, item);
		syncMetadata(session, item);
		return item;
	}

	private static RepoCollection ensureCollection(Session session, SourceDescriptor source) {
		RepoCollection collection = (RepoCollection) session.createCriteria(RepoCollection.class)
				.add(Restrictions.eq("kode", source.collectionCode)).setMaxResults(1).uniqueResult();
		if (collection == null) {
			collection = new RepoCollection();
			collection.setKode(source.collectionCode);
		}
		collection.setNama(source.collectionName);
		collection.setDeskripsi("Koleksi otomatis dari " + source.label);
		collection.setSourceSystem("AIS");
		collection.setTipe("COLLECTION");
		Common.refreshSaveOrUpdate(session, collection);
		return collection;
	}

	private static RepoItem findRepoItem(Session session, String sourceClass, Long sourceId) {
		return (RepoItem) session.createCriteria(RepoItem.class).add(Restrictions.eq("sourceClass", sourceClass))
				.add(Restrictions.eq("sourceId", sourceId)).setMaxResults(1).uniqueResult();
	}

	@SuppressWarnings("unchecked")
	private static void syncMetadata(Session session, RepoItem item) {
		if (item.getId() == null) {
			return;
		}
		List<RepoItemMetadata> old = session.createCriteria(RepoItemMetadata.class)
				.add(Restrictions.eq("itemId", item.getId())).list();
		for (RepoItemMetadata metadata : old) {
			session.delete(metadata);
		}
		saveMetadata(session, item.getId(), "dc.title", item.getTitle(), 0);
		saveMetadata(session, item.getId(), "dc.contributor.author", item.getAuthors(), 1);
		saveMetadata(session, item.getId(), "dc.description.abstract", item.getAbstractText(), 2);
		saveMetadata(session, item.getId(), "dc.subject", item.getSubjects(), 3);
		saveMetadata(session, item.getId(), "dc.publisher", item.getPublisher(), 4);
		saveMetadata(session, item.getId(), "dc.type", item.getDocumentType(), 5);
		saveMetadata(session, item.getId(), "dc.rights.access", item.getAccessPolicy(), 6);
		saveMetadata(session, item.getId(), "repository.turnitin.indexed", item.getTurnitinIndexed().toString(), 7);
		if (item.getIssuedAt() != null) {
			saveMetadata(session, item.getId(), "dc.date.issued", Common.databaseDateFormat.get().format(item.getIssuedAt()), 8);
		}
	}

	private static void saveMetadata(Session session, Long itemId, String field, String value, int place) {
		if (value == null || value.trim().length() == 0) {
			return;
		}
		RepoItemMetadata metadata = new RepoItemMetadata();
		metadata.setItemId(itemId);
		metadata.setMetadataField(field);
		metadata.setMetadataValue(value);
		metadata.setLanguage("id");
		metadata.setPlace(Integer.valueOf(place));
		Common.refreshSaveOrUpdate(session, metadata);
	}

	private static DspaceInformation invokeDspace(String actionClassName, String cookie, Object obj, boolean update)
			throws Exception {
		Class<?> actionClass = Class.forName(actionClassName);
		Method[] methods = actionClass.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (!"getDspace".equals(method.getName())) {
				continue;
			}
			Object[] args = buildDspaceArgs(method, cookie, obj, update);
			if (args != null) {
				Object result = method.invoke(null, args);
				return result instanceof DspaceInformation ? (DspaceInformation) result : null;
			}
		}
		return null;
	}

	private static Object[] buildDspaceArgs(Method method, String cookie, Object obj, boolean update) {
		Class<?>[] types = method.getParameterTypes();
		Object[] args = new Object[types.length];
		boolean hasSourceObject = false;
		for (int i = 0; i < types.length; i++) {
			if (String.class.equals(types[i])) {
				args[i] = cookie;
			} else if (Boolean.TYPE.equals(types[i]) || Boolean.class.equals(types[i])) {
				args[i] = Boolean.valueOf(update);
			} else if (types[i].isAssignableFrom(obj.getClass())) {
				args[i] = obj;
				hasSourceObject = true;
			} else {
				return null;
			}
		}
		return hasSourceObject ? args : null;
	}

	private static Long getLong(Object obj, String methodName) {
		Object value = invoke(obj, methodName);
		return value instanceof Number ? Long.valueOf(((Number) value).longValue()) : null;
	}

	private static String invokeString(Object obj, String methodName) {
		Object value = invoke(obj, methodName);
		return value == null ? "" : value.toString();
	}

	private static String invokeNestedString(Object obj, String method1, String method2) {
		Object value = invoke(obj, method1);
		return value == null ? "" : invokeString(value, method2);
	}

	private static String invokeNestedString(Object obj, String method1, String method2, String method3) {
		Object value = invoke(obj, method1);
		value = value == null ? null : invoke(value, method2);
		return value == null ? "" : invokeString(value, method3);
	}

	private static Date firstDate(Object obj, String m1, String m2, String m3, String m4) {
		Object value = invoke(obj, m1);
		if (value instanceof Date) return (Date) value;
		value = invoke(obj, m2);
		if (value instanceof Date) return (Date) value;
		value = invoke(obj, m3);
		if (value instanceof Date) return (Date) value;
		value = invoke(obj, m4);
		return value instanceof Date ? (Date) value : null;
	}

	private static Object invoke(Object obj, String methodName) {
		try {
			if (obj == null || methodName == null) {
				return null;
			}
			Method method = obj.getClass().getMethod(methodName, new Class[0]);
			return method.invoke(obj, new Object[0]);
		} catch (Exception e) {
			return null;
		}
	}

	private static String firstNotEmpty(String... values) {
		if (values == null) {
			return "";
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null && values[i].trim().length() > 0) {
				return values[i].trim();
			}
		}
		return "";
	}
}
