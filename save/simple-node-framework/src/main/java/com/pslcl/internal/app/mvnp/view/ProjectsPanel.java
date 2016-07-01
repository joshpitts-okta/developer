package com.pslcl.internal.app.mvnp.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.pslcl.chad.app.swing.FrameBase;
import com.pslcl.chad.app.swing.SwappedFocusListener;
import com.pslcl.internal.app.mvnp.MvnPesdca;
import com.pslcl.internal.app.mvnp.ProjectsControl.ProjectsPopUpMenu;
import com.pslcl.internal.app.mvnp.model.Project;
import com.pslcl.internal.app.mvnp.model.Projects;

@SuppressWarnings("javadoc")
public class ProjectsPanel extends JPanel implements SwappedFocusListener, WindowFocusListener, ListSelectionListener
{
    private static final long serialVersionUID = -8115579928389996122L;

    private final Projects projects;

    private final JList<Project> projectList;
    private final ProjectPanel projectPanel;

    private final AtomicBoolean painted;

    public ProjectsPanel(MvnPesdca mvnp)
    {
        FrameBase.logWarningIfThreadIsNotAwt(getClass().getName() + ".constructor");
        projects = mvnp.getProjectsControl().getProjects();
        painted = new AtomicBoolean(false);

        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Projects"));

        projectList = new JList<Project>(projects.getProjectListModel());
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setVisibleRowCount(60);
        projectList.addListSelectionListener(this);
        projectList.setPreferredSize(new Dimension(640, 60));
        ProjectsPopUpMenu projectsPopup = mvnp.getProjectsControl().getProjectsPopupMenu();
        projectList.setComponentPopupMenu(projectsPopup);
        projectList.addMouseListener(projectsPopup);

        JScrollPane scrollPane = new JScrollPane(projectList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(640, 120));
        panel.add(scrollPane);

        mainPanel.add(panel);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Selected project"));

        projectPanel = new ProjectPanel(mvnp, false);
        panel.add(projectPanel);

        mainPanel.add(panel);

        JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataPanel.add(mainPanel);

        scrollPane = new JScrollPane(dataPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(575, 520));
        add(scrollPane, BorderLayout.CENTER);

        mvnp.swapAndSetFocus(this, projectList);
    }

    public Project getSelectedProject()
    {
        return projectList.getSelectedValue();
    }

    @Override
    public void refresh()
    {
        setProjectListModel(projects.getProjectListModel(), projects.getLastSelectedProject());
        projectPanel.refresh();
    }

    public void setProjectListModel(DefaultListModel<Project> model, Project selected)
    {
        FrameBase.logWarningIfThreadIsNotAwt(getClass().getName() + ".setProjectListModel");
        projectList.setModel(model);
        if (selected == null)
        {
            if (model.getSize() > 0)
            {
                projectList.setSelectedIndex(0);
                selected = projectList.getSelectedValue();
            }
        } else
            projectList.setSelectedValue(selected, true);
    }

    @Override
    public void focusRequested(Object context)
    {
        painted.set(true);
    }

//    @Override
//    public void actionPerformed(ActionEvent e)
//    {
//        Object src = e.getSource();
//        if (src == projectList)
//        {
//            Project project = projectList.getSelectedValue();
//            setProjectListModel(projectsControl.getProjects().getProjectListModel(), project);
//            return;
//        }
//    }

    @Override
    public void windowGainedFocus(WindowEvent e)
    {
        //        duster.refresh();
    }

    // @formatter:off
    @Override public void windowLostFocus(WindowEvent e){/* nothing to do here*/}
    // @formatter:on

    /* ************************************************************************
     * ListSelectionListener implementation
     **************************************************************************/
    @Override
    public void valueChanged(ListSelectionEvent event)
    {
        if (event.getValueIsAdjusting())
            return;
        Object src = event.getSource();
        if (src != projectList)
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
                        projectPanel.setProject(projectList.getSelectedValue());
                        projects.setLastSelectedProject(projectList.getSelectedValue());
                    }
                };
                SwingUtilities.invokeLater(runner);
                return null;
            }
        };
        worker.execute();
        return;
    }
}
