package ais.common.inventory.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Dokumen cycle count. Hanya status APPROVED yang boleh mengubah ledger. */
public final class CycleCount {
	public static final String DRAFT = "DRAFT";
	public static final String COUNTED = "COUNTED";
	public static final String APPROVED = "APPROVED";
	public static final String POSTED = "POSTED";
	public static final String REJECTED = "REJECTED";

	private final String countId;
	private final String status;
	private final Date businessAt;
	private final List<CycleCountLine> lines;

	public CycleCount(String countId, String status, Date businessAt, List<CycleCountLine> lines) {
		this.countId = clean(countId);
		this.status = clean(status).toUpperCase(Locale.ENGLISH);
		this.businessAt = copy(businessAt);
		this.lines = lines == null ? Collections.<CycleCountLine>emptyList()
				: Collections.unmodifiableList(new ArrayList<CycleCountLine>(lines));
	}

	public String getCountId() { return countId; }
	public String getStatus() { return status; }
	public Date getBusinessAt() { return copy(businessAt); }
	public List<CycleCountLine> getLines() { return lines; }

	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (countId.length() == 0) errors.add("countId wajib diisi");
		if (businessAt == null) errors.add("businessAt wajib diisi");
		if (lines.isEmpty()) errors.add("baris cycle count wajib diisi");
		Set<String> ids = new HashSet<String>();
		for (int i = 0; i < lines.size(); i++) {
			CycleCountLine line = lines.get(i);
			if (line == null) errors.add("baris cycle count tidak boleh null");
			else {
				errors.addAll(line.validate());
				if (!ids.add(line.getLineId())) errors.add("lineId ganda: " + line.getLineId());
			}
		}
		return errors;
	}

	private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }
	private static String clean(String value) { return value == null ? "" : value.trim(); }
}

