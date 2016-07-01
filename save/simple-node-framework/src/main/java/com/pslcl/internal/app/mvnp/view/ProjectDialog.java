package com.pslcl.internal.app.mvnp.view;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import com.pslcl.internal.app.mvnp.MvnPesdca;

@SuppressWarnings("javadoc")
public class ProjectDialog extends JDialog 
{
    private static final long serialVersionUID = -5737525791938692161L;

    public ProjectDialog(MvnPesdca mvnp) 
    {
        super(mvnp, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(new ProjectPanel(this, mvnp));
        setLocationRelativeTo(mvnp);
        setMinimumSize(new Dimension(245, 160));
        pack();
        setVisible(true);
    }
}