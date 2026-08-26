package ais.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;

import ais.ui.util.MyCheckboxConfig;

/** Filter tampilan pertanyaan angket tanpa mengubah atau menghapus jawaban. */
public final class AngketBelumDiisiFilter {

	private static final String ATTR_INPUTS = "angketFilterInputs";
	private static final String ATTR_SECTION = "angketFilterSection";
	private static final String ATTR_REQUIRE_ALL = "angketFilterRequireAll";

	private AngketBelumDiisiFilter() {
	}

	public static MyCheckboxConfig create(final Collection<Component> questions) {
		final MyCheckboxConfig filter = new MyCheckboxConfig("Tampilkan hanya yang belum saya isi");
		filter.setChecked(false);
		filter.setTooltiptext("Sembunyikan pertanyaan yang sudah memiliki jawaban");
		filter.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				apply(filter, questions);
			}
		});
		return filter;
	}

	public static void register(final Collection<Component> questions, final Component question,
			Component section, final Checkbox filter, Component... inputs) {
		registerInternal(questions, question, section, filter, false, inputs);
	}

	public static void registerAllRequired(final Collection<Component> questions, final Component question,
			Component section, final Checkbox filter, Component... inputs) {
		registerInternal(questions, question, section, filter, true, inputs);
	}

	private static void registerInternal(final Collection<Component> questions, final Component question,
			Component section, final Checkbox filter, boolean requireAll, Component... inputs) {
		if (questions == null || question == null) {
			return;
		}
		List<Component> answerInputs = new ArrayList<Component>();
		if (inputs != null) {
			for (Component input : inputs) {
				if (input != null) {
					answerInputs.add(input);
				}
			}
		}
		question.setAttribute(ATTR_INPUTS, answerInputs);
		question.setAttribute(ATTR_SECTION, section);
		question.setAttribute(ATTR_REQUIRE_ALL, Boolean.valueOf(requireAll));
		questions.add(question);

		EventListener refresh = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (filter != null && filter.isChecked()) {
					apply(filter, questions);
				}
			}
		};
		for (Component input : answerInputs) {
			if (input instanceof Radiogroup || input instanceof Checkbox) {
				input.addEventListener("onCheck", refresh);
			} else {
				input.addEventListener("onChange", refresh);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void apply(Checkbox filter, Collection<Component> questions) {
		if (questions == null) {
			return;
		}
		boolean onlyEmpty = filter != null && filter.isChecked();
		Set<Component> sections = new HashSet<Component>();
		for (Component question : questions) {
			if (question == null) {
				continue;
			}
			List<Component> inputs = (List<Component>) question.getAttribute(ATTR_INPUTS);
			boolean requireAll = Boolean.TRUE.equals(question.getAttribute(ATTR_REQUIRE_ALL));
			question.setVisible(!onlyEmpty || !isAnswered(inputs, requireAll));
			Component section = (Component) question.getAttribute(ATTR_SECTION);
			if (section != null) {
				sections.add(section);
			}
		}
		for (Component section : sections) {
			boolean hasVisibleQuestion = false;
			for (Component question : questions) {
				if (section.equals(question.getAttribute(ATTR_SECTION)) && question.isVisible()) {
					hasVisibleQuestion = true;
					break;
				}
			}
			section.setVisible(hasVisibleQuestion);
		}
	}

	private static boolean isAnswered(List<Component> inputs, boolean requireAll) {
		if (inputs == null) {
			return false;
		}
		boolean foundInput = false;
		for (Component input : inputs) {
			boolean answered = false;
			boolean recognized = false;
			if (input instanceof Radiogroup) {
				recognized = true;
				answered = ((Radiogroup) input).getSelectedItem() != null;
			} else if (input instanceof Checkbox) {
				recognized = true;
				answered = ((Checkbox) input).isChecked();
			} else if (input instanceof Textbox) {
				recognized = true;
				answered = ((Textbox) input).getValue() != null && !((Textbox) input).getValue().trim().isEmpty();
			} else if (input instanceof Intbox) {
				recognized = true;
				answered = ((Intbox) input).getValue() != null;
			}
			if (!recognized) {
				continue;
			}
			foundInput = true;
			if ((!requireAll && answered) || (requireAll && !answered)) {
				return !requireAll;
			}
		}
		return requireAll && foundInput;
	}
}
