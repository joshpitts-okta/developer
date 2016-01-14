package com.pslcl.internal.app.mvnp.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.pslcl.chad.app.swing.FrameBase;
import com.pslcl.chad.app.swing.SwappedFocusListener;
import com.pslcl.internal.app.mvnp.MvnPesdca;

@SuppressWarnings("javadoc")
public class EnvPanel extends JPanel implements DocumentListener,  ActionListener, SwappedFocusListener
{
    private static final long serialVersionUID = -7249966694503896587L;

    private final int numberOfColumns = 52;
    
    private final MvnPesdca mvnp;
    
    private final JTextField checkoutBasePathField; 
    private final JTextField pomBasePathField;
        
    private final JButton applyButton;
    private final JButton cancelButton;
    
    public EnvPanel(MvnPesdca mvnp, boolean editable)
    {
        FrameBase.logWarningIfThreadIsNotAwt(getClass().getName() + ".constructor");
        this.mvnp = mvnp;
        
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new TitledBorder("Workstation Environment"));

        AtomicInteger maxWidth = new AtomicInteger();
        JLabel checkoutBasePathLabel = FrameBase.getLabel("SVN checkout base path:", maxWidth);
        JLabel pomBasePathLabel = FrameBase.getLabel("Shadow base path:", maxWidth);
        int width = maxWidth.get();
        
        checkoutBasePathField = new JTextField(mvnp.getMvnpData().getSvnCheckoutBasePath(), numberOfColumns);
        FrameBase.addTextParamToPanel(checkoutBasePathLabel, checkoutBasePathField, width, -1, "The workstations base root folder for SVN checkouts", mainPanel, this);
        checkoutBasePathField.setEditable(editable);
        
        pomBasePathField = new JTextField(mvnp.getMvnpData().getShadowPomBasePath(), numberOfColumns);
        FrameBase.addTextParamToPanel(pomBasePathLabel, pomBasePathField, width, -1, "The workstations base root folder for the Shadow pom structure", mainPanel, this);
        pomBasePathField.setEditable(editable);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(true);
        applyButton = FrameBase.addButtonToPanel("Apply", false, "Commit the current modifications", buttonPanel, this);
        cancelButton = FrameBase.addButtonToPanel("Cancel", true, "Cancel any edits and exit", buttonPanel, this);
        if(editable)
            mainPanel.add(buttonPanel);
        
        JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataPanel.add(mainPanel);
        
        add(dataPanel, BorderLayout.CENTER);
        mvnp.swapAndSetFocus(this, checkoutBasePathField);
    }
    
    @Override
    public void refresh()
    {
        checkoutBasePathField.setText(mvnp.getMvnpData().getSvnCheckoutBasePath());
        pomBasePathField.setText(mvnp.getMvnpData().getShadowPomBasePath());
    }
    
    @Override
    public void focusRequested(Object context)
    {
        // nothing to do here
    }
    
/* ************************************************************************
* ActionListener implementation
**************************************************************************/
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        if(src == applyButton)
        {
            mvnp.getMvnpData().setShadowPomBasePath(pomBasePathField.getText());
            mvnp.getMvnpData().setSvnCheckoutBasePath(checkoutBasePathField.getText());
            mvnp.swapContentPane(mvnp.getProjectsControl().getProjectsPanel());
            mvnp.refresh();
            return;
        }
        if(src == cancelButton)
        {
            mvnp.swapContentPane(mvnp.getProjectsControl().getProjectsPanel());
            return;
        }
    }

/* ************************************************************************
* DocumentListener implementation
**************************************************************************/
    //@Formatter:off
    @Override public void insertUpdate(DocumentEvent e){changedUpdate(e);}
    @Override public void removeUpdate(DocumentEvent e){changedUpdate(e);}
    //@Formatter:on
    @Override public void changedUpdate(DocumentEvent e)
    {
        applyButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }
}
