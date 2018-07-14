package ch.supertomcat.bh.gui.settings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ch.supertomcat.supertomcattools.guitools.GridBagLayoutTool;
import ch.supertomcat.supertomcattools.guitools.copyandpaste.JTextComponentCopyAndPaste;

/**
 * Editor-Component for Subdirs (Resolution)
 */
public class SubdirResolutionCellEditorComponent extends JPanel {
	/**
	 * UID
	 */
	private static final long serialVersionUID = -1695172104892889362L;

	/**
	 * DefaultValue
	 */
	private static String defaultVal = "0x0";

	/**
	 * TextField
	 */
	private JTextField txtValMin = new JTextField("", 5);

	/**
	 * TextField
	 */
	private JTextField txtValMax = new JTextField("", 5);

	/**
	 * GridBagLayout
	 */
	private GridBagLayout gbl = new GridBagLayout();

	/**
	 * GridBagLayoutTool
	 */
	private GridBagLayoutTool gblt = new GridBagLayoutTool(1, 1, 1, 1);

	/**
	 * Constructor
	 */
	public SubdirResolutionCellEditorComponent() {
		txtValMin.setHorizontalAlignment(JTextField.LEFT);
		txtValMax.setHorizontalAlignment(JTextField.LEFT);
		this.setLayout(new BorderLayout());

		JPanel pnlTextFields = new JPanel();
		pnlTextFields.setLayout(gbl);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc = gblt.getGBC(0, 0, 1, 1, 0.475, 0.0);
		GridBagLayoutTool.addItemToPanel(gbl, gbc, txtValMin, pnlTextFields);
		gbc = gblt.getGBC(1, 0, 1, 1, 0.05, 0.0);
		GridBagLayoutTool.addItemToPanel(gbl, gbc, new JLabel("x", JLabel.CENTER), pnlTextFields);
		gbc = gblt.getGBC(2, 0, 1, 1, 0.475, 0.0);
		GridBagLayoutTool.addItemToPanel(gbl, gbc, txtValMax, pnlTextFields);

		add(pnlTextFields, BorderLayout.CENTER);

		JTextComponentCopyAndPaste.addCopyAndPasteMouseListener(txtValMin);
		JTextComponentCopyAndPaste.addCopyAndPasteMouseListener(txtValMax);
	}

	/**
	 * Set-Method
	 * 
	 * @param val Value
	 */
	public void setText(String val) {
		String arr[] = val.split("x");
		if (arr.length == 2) {
			txtValMin.setText(arr[0]);
			txtValMax.setText(arr[1]);
		} else {
			txtValMin.setText(String.valueOf(0));
			txtValMax.setText(String.valueOf(0));
		}
	}

	/**
	 * Get-Method
	 * 
	 * @return Value
	 */
	public String getVal() {
		return parseIntVal(txtValMin.getText()) + "x" + parseIntVal(txtValMax.getText());
	}

	/**
	 * Returns the integer in the val-String or 0 if not possible
	 * 
	 * @param val Value
	 * @return Integer-Value
	 */
	public static int parseIntVal(String val) {
		try {
			int l = Integer.parseInt(val);
			return l;
		} catch (NumberFormatException nfe) {
		}
		try {
			int l = Integer.parseInt(defaultVal);
			return l;
		} catch (NumberFormatException nfe) {
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#toString()
	 */
	@Override
	public String toString() {
		return parseIntVal(txtValMin.getText()) + "x" + parseIntVal(txtValMax.getText());
	}
}
