package ais.common.inventory.accountspayable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Hasil pencocokan invoice terhadap PO dan penerimaan/BAST. */
public final class ThreeWayMatchResult {
	public static final String MATCHED = "MATCHED";
	public static final String EXCEPTION = "EXCEPTION";
	private final String status;
	private final List<String> messages;

	public ThreeWayMatchResult(String status, List<String> messages) {
		this.status = status;
		this.messages = Collections.unmodifiableList(new ArrayList<String>(messages));
	}
	public String getStatus() { return status; }
	public List<String> getMessages() { return messages; }
	public boolean isMatched() { return MATCHED.equals(status); }
}
