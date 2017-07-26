package net.imglib2.algorithm.features.gui;

import net.imglib2.algorithm.features.*;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import net.imglib2.algorithm.features.ops.FeatureOp;
import net.miginfocom.swing.MigLayout;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.*;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.widget.InputHarvester;

/**
 * GUI to select a list of features and their settings.
 *
 * @author Matthias Arzt
 */
public class FeatureSettingsGui {

	private final JPanel content = new JPanel();

	private final ListModel model = new ListModel();

	private final JList<Holder> list = new JList<>();

	private final Context context = new Context();

	private final FeatureSettingsDialog featureSettingsDialog = new FeatureSettingsDialog(context);

	private GlobalsPanel globalsPanel;

	public FeatureSettingsGui() {
		initGui();
	}

	private void initGui() {
		list.setModel(model);
		content.setLayout(new MigLayout("insets 0", "[grow]","[][grow][]"));
		globalsPanel = new GlobalsPanel(GlobalSettings.defaultSettings());
		content.add(globalsPanel, "wrap");
		content.add(new JScrollPane(list), "split 2, grow");
		JPopupMenu menu = initMenu();
		JButton addButton = new JButton("add");
		addButton.addActionListener(a ->
			menu.show(addButton, 0, addButton.getHeight()));
		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(new MigLayout("insets 0", "[]","[][][][grow]"));
		sidePanel.add(addButton, "grow, wrap");
		sidePanel.add(addButton("remove", this::removePressed), "grow, wrap");
		sidePanel.add(addButton("edit", this::editPressed), "grow, wrap");
		content.add(sidePanel, "top, wrap");
	}

	private static class GlobalsPanel extends JPanel {

		private final JFormattedTextField minSigmaField;
		private final JFormattedTextField maxSigmaField;
		private final JFormattedTextField thicknessField;

		public GlobalsPanel(GlobalSettings globalSettings) {
			setLayout(new MigLayout("insets 0", "[]20pt[50pt]", "[][]"));
			add(new JLabel("min sigma"));
			minSigmaField = initFormattedTextField();
			minSigmaField.setValue(globalSettings.minSigma());
			add(minSigmaField, "grow, wrap");
			add(new JLabel("min sigma"));
			maxSigmaField = initFormattedTextField();
			maxSigmaField.setValue(globalSettings.maxSigma());
			add(maxSigmaField, "grow, wrap");
			add(new JLabel("membrane thickness"));
			thicknessField = initFormattedTextField();
			thicknessField.setValue(globalSettings.membraneThickness());
			add(thicknessField, "grow, wrap");
		}

		private JFormattedTextField initFormattedTextField() {
			return new JFormattedTextField(new NumberFormatter(NumberFormat.getInstance()));
		}

		GlobalSettings get() {
			return new GlobalSettings(
					(Double) minSigmaField.getValue(),
					(Double) maxSigmaField.getValue(),
					(Double) thicknessField.getValue()
			);
		}
	}

	private JPopupMenu initMenu() {
		JPopupMenu menu = new JPopupMenu();
		List<PluginInfo<FeatureOp>> pi = context.service(PluginService.class).getPluginsOfType(FeatureOp.class);
		List<Class<? extends FeatureOp>> choices = pi.stream().map(PluginInfo::getPluginClass).collect(Collectors.toList());
		for(Class<? extends FeatureOp> choice : choices) {
			JMenuItem item = new JMenuItem(choice.getSimpleName());
			item.addActionListener(l -> addPressed(choice));
			menu.add(item);
		}
		return menu;
	}

	private void removePressed() {
		int index = list.getSelectedIndex();
		if(index < 0)
			return;
		model.remove(index);
		model.update();
	}

	private void editPressed() {
		int index = list.getSelectedIndex();
		if(index < 0)
			return;
		model.getElementAt(index).edit();
		model.update();
	}

	private void addPressed(Class<? extends FeatureOp> featureClass) {
		model.add(new Holder(featureClass));
	}

	private JButton addButton(String add, Runnable runnable) {
		JButton button = new JButton(add);
		button.addActionListener(e -> runnable.run());
		return button;
	}

	public static Optional<FeatureGroup> show() {
		FeatureSettingsGui featureSettingsGui = new FeatureSettingsGui();
		return featureSettingsGui.showInternal();
	}

	private Optional<FeatureGroup> showInternal() {
		boolean ok = showResizeableOkCancelDialog("Select Pixel Features", content);
		if(ok) {
			GlobalSettings globalSettings = globalsPanel.get();
			return Optional.of(model.features(globalSettings));
		} else
			return Optional.empty();
	}

	public static boolean showResizeableOkCancelDialog(String title, JPanel content) {
		JDialog dialog = new JDialog((Frame) null, title, true);
		JOptionPane optionPane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		dialog.setContentPane(optionPane);
		dialog.setResizable(true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		optionPane.addPropertyChangeListener( e -> {
				String prop = e.getPropertyName();
				if (dialog.isVisible() && (e.getSource() == optionPane) && (JOptionPane.VALUE_PROPERTY.equals(prop)))
					dialog.dispose();
		});
		dialog.pack();
		dialog.setVisible(true);
		return optionPane.getValue().equals(JOptionPane.OK_OPTION);
	}

	public static void main(String... args) {
		System.out.println(FeatureSettingsGui.show());
		System.out.println("finished");
	}

	private class Holder {

		private FeatureSetting value;

		public Holder(Class<? extends FeatureOp> featureClass) {
			this.value = FeatureSetting.fromClass(featureClass);
		}

		void edit() {
			value = featureSettingsDialog.show(value);
		}

		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner(", ");
			for(String parameter : value.parameters())
				joiner.add(parameter + " = " + value.getParameter(parameter));
			return value.getName() + " " + joiner;
		}

		public FeatureOp get(GlobalSettings globalSettings) {
			return value.newInstance(RevampUtils.ops(), globalSettings);
		}
	}

	static class ListModel extends AbstractListModel<Holder> {

		List<Holder> items = new ArrayList();

		@Override
		public int getSize() {
			return items.size();
		}

		@Override
		public Holder getElementAt(int index) {
			return items.get(index);
		}

		public void add(Holder holder) {
			items.add(holder);
			update();
		}

		public void remove(int index) {
			items.remove(index);
			update();
		}

		public void update() {
			fireContentsChanged(items, 0, items.size());
		}

		public FeatureGroup features(GlobalSettings globalSettings) {
			return Features.group( items.stream().map(h -> h.get(globalSettings)).collect(Collectors.toList()) );
		}
	}

	static class FeatureSettingsDialog extends AbstractContextual {

		private final InputHarvester harvester;

		FeatureSettingsDialog(Context context) {
			harvester = getHarvester(context);
		}

		FeatureSetting show(FeatureSetting op) {
			try {
				Module module = op.asModule(GlobalSettings.defaultSettings());
				harvester.harvest(module);
				return FeatureSetting.fromModule(module);
			} catch (ModuleException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static InputHarvester<JPanel, JPanel> getHarvester(Context context) {
		List<InputHarvester> harvester1 = RevampUtils.filterForClass(InputHarvester.class,
				context.service(PluginService.class).createInstancesOfType(PreprocessorPlugin.class));
		List<SwingInputHarvester> swing = RevampUtils.filterForClass(SwingInputHarvester.class, harvester1);
		return swing.isEmpty() ? harvester1.get(0) : swing.get(0);
	}
}
