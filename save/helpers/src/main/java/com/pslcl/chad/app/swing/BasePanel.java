/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
abstract public class BasePanel extends JPanel
{
    private static final long serialVersionUID = 2950461477214335815L;

    public BasePanel()
    {
    }

    public static JButton addButtonToPanel(String label, boolean enabled, String toolTip, JPanel panel)
    {
        return addButtonToPanel(label, enabled, toolTip, panel, null);
    }

    public static JButton addButtonToPanel(String label, boolean enabled, String toolTip, JPanel panel, ActionListener listener)
    {
        JButton button = new JButton(label);
        if (listener != null)
            button.addActionListener(listener);
        button.setEnabled(enabled);
        button.setToolTipText(toolTip);
        panel.add(button);
        return button;
    }

    public static JCheckBox addCheckBoxToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel)
    {
        return addCheckBoxToPanel(label, value, enabled, width, toolTip, panel, null, null, null);
    }

    public static JCheckBox addCheckBoxToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener listener)
    {
        return addCheckBoxToPanel(label, value, enabled, width, toolTip, panel, null, listener, null);
    }
    
    public static JCheckBox addCheckBoxToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener alistener, MouseListener mlistener)
    {
        return addCheckBoxToPanel(label, value, enabled, width, toolTip, panel, null, alistener, mlistener);
    }
    
    public static JCheckBox addCheckBoxToPanel(
            JLabel label, boolean value, boolean enabled, int width, 
            String toolTip, JPanel panel, String layoutConstraint, ActionListener alistener, MouseListener mlistener)
    {
        JCheckBox cbox = new JCheckBox();
        cbox.setToolTipText(toolTip);
        cbox.setSelected(value);
        if (alistener != null)
            cbox.addActionListener(alistener);
        if (!enabled)
            cbox.setEnabled(false);
        addTextParamToPanel(label, cbox, width, -1, toolTip, panel, layoutConstraint, null, mlistener);
        return cbox;
    }

    public static JRadioButton addRadioButtonToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel)
    {
        return addRadioButtonToPanel(label, value, enabled, width, toolTip, panel, null, null, null);
    }

    public static JRadioButton addRadioButtonToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener listener)
    {
        return addRadioButtonToPanel(label, value, enabled, width, toolTip, panel, null, listener, null);
    }
    
    public static JRadioButton addRadioButtonToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener alistener, MouseListener mlistener)
    {
        return addRadioButtonToPanel(label, value, enabled, width, toolTip, panel, null, alistener, mlistener);
    }
    
    public static JRadioButton addRadioButtonToPanel(
            JLabel label, boolean value, boolean enabled, int width, 
            String toolTip, JPanel panel, String layoutConstraint, 
            ActionListener alistener, MouseListener mlistener)
    {
        JRadioButton radio = new JRadioButton();
        radio.setToolTipText(toolTip);
        radio.setSelected(value);
        if (alistener != null)
            radio.addActionListener(alistener);
        if (!enabled)
            radio.setEnabled(false);
        addTextParamToPanel(label, radio, width, -1, toolTip, panel, layoutConstraint, null, mlistener);
        return radio;
    }

    public static JPanel addTextParamToPanel(JLabel label, JComponent field, int width, int gap, String toolTip, JPanel container)
    {
        return addTextParamToPanel(label, field, width, gap, toolTip, container, null, null);
    }

    public static JPanel addTextParamToPanel(JLabel label, JComponent field, int width, int gap, String toolTip, JPanel container, DocumentListener docListener)
    {
        return addTextParamToPanel(label, field, width, gap, toolTip, container, null, docListener, null);
    }
    
    public static JPanel addTextParamToPanel(JLabel label, JComponent field, int width, int gap, String toolTip, JPanel container, DocumentListener docListener, MouseListener mouseListener)
    {
        return addTextParamToPanel(label, field, width, gap, toolTip, container, null, docListener, mouseListener);
    }
    
//    public static JPanel addTextParamToPanel(
//            JLabel label, JComponent field, int width, int gap, 
//            String toolTip, JPanel container, 
//            DocumentListener docListener, String layoutConstraint)
//    {
//        return addTextParamToPanel(label, field, width, gap, toolTip, container, layoutConstraint, docListener, null);
//    }
    
    public static JPanel addTextParamToPanel(
            JLabel label, JComponent field, int width, int gap, 
            String toolTip, JPanel container, String layoutConstraint, 
            DocumentListener docListener, MouseListener mouseListener)
    {
        label.setForeground(Color.black);
        label.setPreferredSize(new Dimension(width, (int) label.getPreferredSize().getHeight()));
        if (field instanceof JTextField)
            ((JTextField) field).getDocument().addDocumentListener(docListener);
        field.setToolTipText(toolTip);
        label.setToolTipText(toolTip);
        if(mouseListener != null)
        {
            field.addMouseListener(mouseListener);
            label.addMouseListener(mouseListener);
        }
        JPanel paramPanel = new JPanel(new BorderLayout(gap == -1 ? 5 : width, 0));
        paramPanel.add(label, BorderLayout.WEST);
        paramPanel.add(field, BorderLayout.CENTER);
        if (container != null)
        {
            if(layoutConstraint != null)
                container.add(paramPanel, layoutConstraint);
            else
                container.add(paramPanel);
        }
        if(mouseListener != null)
            paramPanel.addMouseListener(mouseListener);
        return paramPanel;
    }

    public static Point getLocation(Component component)
    {
        Point totalRelativeLocation = new Point();
        do
        {
            Point relativeLocation = component.getLocation();
            totalRelativeLocation.x += relativeLocation.x;
            totalRelativeLocation.y += relativeLocation.y;
            component = component.getParent();
        } while (component != null);
        return totalRelativeLocation;
    }
    
    public static void displayException(Logger logger, Component parentComponent, String title, String message, Throwable t)
    {
        StringWriter output = new StringWriter();
        if(message != null)
        {
            output.write(message);
            output.write("\n");
        }
        if(title == null)
            title = "Unexpected Exception";
        PrintWriter pw = new PrintWriter(output);
        if(t != null)
            t.printStackTrace(pw);
        if(logger != null)
            logger.error(message, t);
        JOptionPane.showMessageDialog(parentComponent, output.toString(), title, JOptionPane.ERROR_MESSAGE);
    }
}
