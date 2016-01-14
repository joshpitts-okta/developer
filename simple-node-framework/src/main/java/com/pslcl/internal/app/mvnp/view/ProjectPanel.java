package com.pslcl.internal.app.mvnp.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.pslcl.chad.app.StrH;
import com.pslcl.chad.app.swing.FrameBase;
import com.pslcl.chad.app.swing.SwappedFocusListener;
import com.pslcl.internal.app.mvnp.MvnPesdca;
import com.pslcl.internal.app.mvnp.ProjectsControl;
import com.pslcl.internal.app.mvnp.model.Project;
import com.pslcl.internal.app.mvnp.model.Repositories;
import com.pslcl.internal.app.mvnp.model.Repository;

@SuppressWarnings("javadoc")
public class ProjectPanel extends JPanel implements DocumentListener,  ActionListener, SwappedFocusListener
{
    private static final long serialVersionUID = 6892139251879301164L;
    public static final String packageVersionKey = "PACKAGE_VERSION:";
    public static final String emitBuildRepositoryKey = "EMIT_BUILD_REPOSITORY:";
    public static final String emitBuildRevisionkey = "EMIT_BUILD_REVISION:";
    public static final String emitBuildPathKey = "EMIT_BUILD_PATH:";
    public static final String emitBuildBuildNumKey = "EMIT_BUILD_BUILDNUM:";
    public static final String emitBuildDateKey = "EMIT_BUILD_DATE:";
    
    private final int numberOfColumns = 52;
    
    private final MvnPesdca mvnp;
    private final ProjectsControl control;
    private ProjectDialog dialog;
    
    private final JComboBox<Repository> repositoryComboBox;
    private final Document qbVersionDocument;
    private final JTextArea qbVersionTextArea;
    private final JTextField qbVersionFileUrlField;
    private final JTextField repositoryPathField; 
    private final JTextField groupField;
    private final JTextField artifactField;
    private final JTextField releaseVersionField;
    private final JTextField svnRevisionField;
    private final JTextField buildNumField;
    private final JTextField dateField;
    private final JTextField qbIdField;
    
    private final EnvPanel envPanel;
    
    private final JButton applyButton;
    private final JButton cancelButton;
    @SuppressWarnings("unused")
    private final boolean editable;
    
    public ProjectPanel(ProjectDialog dialog, MvnPesdca mvnp)
    {
        this(mvnp, true);
        this.dialog = dialog;
    }

    public ProjectPanel(MvnPesdca mvnp, boolean editable)
    {
        FrameBase.logWarningIfThreadIsNotAwt(getClass().getName() + ".constructor");
        this.mvnp = mvnp;
        control = mvnp.getProjectsControl();
        this.editable = editable;
        
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        envPanel = new EnvPanel(mvnp, false);
        mainPanel.add(envPanel);
        
        JPanel projectPanel = new JPanel();
        projectPanel.setLayout(new BoxLayout(projectPanel, BoxLayout.Y_AXIS));
        projectPanel.setBorder(new TitledBorder("Project information"));
        
        AtomicInteger maxWidth = new AtomicInteger();
        JLabel repositoryLabel = FrameBase.getLabel("repository:", maxWidth);
        JLabel qbVersionLabel = FrameBase.getLabel("qb version:", maxWidth);
        JLabel qbVersionFileUrlLabel = FrameBase.getLabel("qb version file url:", maxWidth);
        JLabel repositoryPathLabel = FrameBase.getLabel("repository path:", maxWidth);
        JLabel groupLabel = FrameBase.getLabel("group:", maxWidth);
        JLabel artifactLabel = FrameBase.getLabel("artifact:", maxWidth);
        JLabel svnRevisionLabel = FrameBase.getLabel("svn revision:", maxWidth);
        JLabel releaseVersionLabel = FrameBase.getLabel("release version:", maxWidth);
        JLabel buildNumLabel = FrameBase.getLabel("build number:", maxWidth);
        JLabel dateLabel = FrameBase.getLabel("release date:", maxWidth);
        JLabel qbIdLabel = FrameBase.getLabel("QB ID:", maxWidth);
        int width = maxWidth.get();
        
        repositoryComboBox = new JComboBox<Repository>(mvnp.getMvnpData().getRepositories().getRepositoryComboBoxModel());
        repositoryComboBox.addActionListener(this);
        
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.add(repositoryLabel);
        rowPanel.add(repositoryComboBox);
        projectPanel.add(rowPanel);
        repositoryComboBox.setEnabled(editable);
        
        qbVersionTextArea = new JTextArea();
        qbVersionTextArea.setRows(1);
        qbVersionDocument = qbVersionTextArea.getDocument(); 
        if(editable)
            FrameBase.addTextParamToPanel(qbVersionLabel, qbVersionTextArea, width, -1, "Copy the full contents of the Quickbuilds version file here", projectPanel, this);
                
        qbVersionFileUrlField = new JTextField("", numberOfColumns);
//        urlFieldDocument = qbVersionFileUrlField.getDocument(); 
        if(editable)
            FrameBase.addTextParamToPanel(qbVersionFileUrlLabel, qbVersionFileUrlField, width, -1, "URL of the quickbuilds Version file", projectPanel, this);
        
        repositoryPathField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(repositoryPathLabel, repositoryPathField, width, -1, "SVN URL base path leading to but not including Group", projectPanel, this);
        repositoryPathField.setEditable(editable);
        
        groupField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(groupLabel, groupField, width, -1, "Group level Component", projectPanel, this);
        groupField.setEditable(editable);
        
        artifactField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(artifactLabel, artifactField, width, -1, "Artifact level Component", projectPanel, this);
        artifactField.setEditable(editable);
        
        svnRevisionField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(svnRevisionLabel, svnRevisionField, width, -1, "SVN revision to checkout at", projectPanel, this);
        svnRevisionField.setEditable(editable);
        
        qbIdField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(qbIdLabel, qbIdField, width, -1, "Quick Build's build ID", projectPanel, this);
        qbIdField.setEditable(editable);
        
        buildNumField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(buildNumLabel, buildNumField, width, -1, "Quick Build's build number", projectPanel, this);
        buildNumField.setEditable(editable);
        
        dateField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(dateLabel, dateField, width, -1, "Release build promotion date", projectPanel, this);
        dateField.setEditable(editable);
        
        releaseVersionField = new JTextField("", numberOfColumns);
        FrameBase.addTextParamToPanel(releaseVersionLabel, releaseVersionField, width, -1, "Artifact version", projectPanel, this);
        releaseVersionField.setEditable(editable);
        mainPanel.add(projectPanel);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(true);
        applyButton = FrameBase.addButtonToPanel("Apply", false, "Commit the current modifications", buttonPanel, this);
        cancelButton = FrameBase.addButtonToPanel("Cancel", true, "Cancel any edits and exit", buttonPanel, this);
        if(editable)
            mainPanel.add(buttonPanel);
        
        JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataPanel.add(mainPanel);
        
        JScrollPane scrollPane = new JScrollPane(dataPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(600, 450));
        add(scrollPane, BorderLayout.CENTER);
        mvnp.swapAndSetFocus(this, repositoryComboBox);
    }
    
    public void setProject(Project project)
    {
        if(project == null)
            return;
        FrameBase.logWarningIfThreadIsNotAwt(getClass().getName() + ".setProject");
        Repositories repositories = mvnp.getMvnpData().getRepositories();
        repositories.setLastSelectedRepository(project.repository);
        
        ComboBoxModel<Repository> model = repositoryComboBox.getModel();
        for(int i=0; i < model.getSize(); i++)
        {
            if(model.getElementAt(i).equals(project.repository))
            {
                repositoryComboBox.setSelectedIndex(i);
                break;
            }
        }
        repositoryPathField.setText(project.repositoryPath);
        groupField.setText(project.group);
        artifactField.setText(project.artifact);
        svnRevisionField.setText(project.svnRevision);
        buildNumField.setText(project.buildNumber);
        qbIdField.setText(project.qbId);
        dateField.setText(project.releaseDate);
        releaseVersionField.setText(project.releaseVersion);
    }
    
    @Override
    public void refresh()
    {
        envPanel.refresh();
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
            //@formatter:off
            Project project = new Project(
                (Repository)repositoryComboBox.getSelectedItem(),
                repositoryPathField.getText(),
                groupField.getText(),
                artifactField.getText(),
                svnRevisionField.getText(),
                buildNumField.getText(),
                qbIdField.getText(),
                dateField.getText(),
                releaseVersionField.getText());
                //@formatter:on
            
            control.addProject(project);
            mvnp.refresh();
            dialog.dispose();
            return;
        }
        if(src == cancelButton)
        {
            dialog.dispose();
            return;
        }
    }
    
    private void checkToken(String token, String key, Component field, JTextField groupField)
    {
        if(token.contains(key))
        {
            token = StrH.stripTrailingSeparator(token);
            token = token.substring(key.length());
            if(groupField != null)
            {
                String group = StrH.getAtomicNameFromPath(token);
                groupField.setText(group);
                token = token.substring(0, token.indexOf(group));
            }
            
            if(field instanceof JTextField)
            {
                ((JTextField)field).setText(token);
                return;
            }
            if(field instanceof JTextArea)
            {
                ComboBoxModel<Repository> model = repositoryComboBox.getModel();
                for(int i=0; i < model.getSize(); i++)
                {
                    if(model.getElementAt(i).isRepository(token))
                    {
                        repositoryComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }
    
    private void calculateFromVersionFile()
    {
        try
        {
            String value = qbVersionDocument.getText(0, qbVersionDocument.getLength());
            StringTokenizer tokenizer = new StringTokenizer(value, "\n");
            while(tokenizer.hasMoreTokens())
            {
                String token = tokenizer.nextToken();
                checkToken(token, packageVersionKey, releaseVersionField, null);
                checkToken(token, emitBuildRepositoryKey, qbVersionTextArea, null);
                checkToken(token, emitBuildRevisionkey, svnRevisionField, null);
                checkToken(token, emitBuildPathKey, repositoryPathField, groupField);
                checkToken(token, emitBuildBuildNumKey, buildNumField, null);
                checkToken(token, emitBuildDateKey, dateField, null);
            }
            qbVersionDocument.remove(0, qbVersionDocument.getLength());
        } catch (Exception e)
        {
            FrameBase.displayException(null, mvnp, "QB Version contents error", "failed process the contents of the QB Version file", e);
        }
    }

    //TODO: figure out how to use certificates to access the url
    @SuppressWarnings("unused")
    private void calculateFromVersionUrl()
    {
        BufferedReader bir = null;
        try
        {
            URL version = new URL(qbVersionFileUrlField.getText());
            bir = new BufferedReader(new InputStreamReader(version.openStream()));
            String token;
            while ((token = bir.readLine()) != null)
            {
                checkToken(token, packageVersionKey, releaseVersionField, null);
                checkToken(token, emitBuildRepositoryKey, qbVersionTextArea, null);
                checkToken(token, emitBuildRevisionkey, svnRevisionField, null);
                checkToken(token, emitBuildPathKey, repositoryPathField, groupField);
                checkToken(token, emitBuildBuildNumKey, buildNumField, null);
                checkToken(token, emitBuildDateKey, dateField, null);
            }
        } catch (Exception e)
        {
            FrameBase.displayException(null, mvnp, "Invalid QB Version URL", "failed to read " + qbVersionFileUrlField.getText(), e);
        }
        finally
        {
            //@formatter:off
            try{if(bir != null)bir.close();} catch (IOException e){/* best try*/}
            //@formatter:on
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
        
        if(e.getDocument() != qbVersionDocument)
            return;
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                Runnable runner = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        calculateFromVersionFile();
                    }
                };
                SwingUtilities.invokeLater(runner);
                return null;
            }
        };
        worker.execute();
    }
}
