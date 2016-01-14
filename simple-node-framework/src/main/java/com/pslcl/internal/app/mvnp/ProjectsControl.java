package com.pslcl.internal.app.mvnp;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.internal.app.mvnp.model.Project;
import com.pslcl.internal.app.mvnp.model.Projects;
import com.pslcl.internal.app.mvnp.view.EnvPanel;
import com.pslcl.internal.app.mvnp.view.NotDoneYetPanel;
import com.pslcl.internal.app.mvnp.view.ProjectDialog;
import com.pslcl.internal.app.mvnp.view.ProjectsPanel;

@SuppressWarnings("javadoc")
public class ProjectsControl
{
    private final MvnPesdca mvnp;

    private AddProjectAction addProjectAction;
    private DeleteProjectAction deleteProjectAction;
    private BuildProjectAction buildProjectAction;
    private EnvProjectAction envProjectAction;
    
    private JMenuItem addProjectMenuItem;
    private JMenuItem deleteProjectMenuItem;
    private JMenuItem buildProjectMenuItem;
    private JMenuItem envProjectMenuItem;

    @SuppressWarnings("unused")
    private final Logger log;
    private ProjectsPanel projectsPanel;
    private final Projects projects;
    private ProjectsPopUpMenu projectsPopupMenu;

    public ProjectsControl(MvnPesdca duster)
    {
        log = LoggerFactory.getLogger(getClass());
        this.mvnp = duster;
        projects = mvnp.getMvnpData().getProjects();
    }

    public void addProject(Project project)
    {
        projects.addProject(project);
        projects.setLastSelectedProject(project);
    }
    
    public synchronized Projects getProjects()
    {
        return projects;
    }
    
    public void refresh()
    {
        projectsPanel.refresh();
    }

    public ProjectsPanel getProjectsPanel()
    {
        if(projectsPanel == null)
            projectsPanel = new ProjectsPanel(mvnp);
        return projectsPanel;
    }
    
    public ProjectsPopUpMenu getProjectsPopupMenu()
    {
        if(projectsPopupMenu == null)
            projectsPopupMenu = new ProjectsPopUpMenu();
        return projectsPopupMenu;
    }

    public void addMenu(JMenuBar menuBar, JToolBar toolBar)
    {
        JMenu projectsMenu = new JMenu("Projects");
        projectsMenu.setMnemonic(MvnPesdca.ProjectsMenuMnemonic);
        addProjectAction = new AddProjectAction(projectsMenu);
        deleteProjectAction = new DeleteProjectAction(projectsMenu);
        buildProjectAction = new BuildProjectAction(projectsMenu);
        envProjectAction = new EnvProjectAction(projectsMenu);
        addProjectMenuItem = new JMenuItem(addProjectAction);
        deleteProjectMenuItem = new JMenuItem(deleteProjectAction);
        buildProjectMenuItem = new JMenuItem(buildProjectAction);
        envProjectMenuItem = new JMenuItem(envProjectAction);
        menuBar.add(projectsMenu);
    }

    public void close()
    {
    }

    public void setBusy(boolean busy)
    {
        addProjectAction.setEnabled(!busy);
        deleteProjectAction.setEnabled(!busy);
        buildProjectAction.setEnabled(!busy);
        envProjectAction.setEnabled(!busy);
    }

    final class AddProjectAction extends AbstractAction
    {
        private static final long serialVersionUID = 5901351724603473861L;
        public final String Image = "exit.gif";
        public static final String Label = "Add...";
        public static final String MouseOver = "Add a new Project";
        public final int Mnemonic = MvnPesdca.AddProjectActionMnemonic;
        public final int Accelerator = MvnPesdca.AddProjectActionAccelerator;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public AddProjectAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            JMenuItem item = menu.add(this);
            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @SuppressWarnings("unused")
        @Override
        public void actionPerformed(ActionEvent e)
        {
            new ProjectDialog(mvnp);
        }
    }

    final class DeleteProjectAction extends AbstractAction
    {
        private static final long serialVersionUID = -7411203598373093374L;
        public final String Image = "exit.gif";
        public static final String Label = "Delete";
        public static final String MouseOver = "Delete currently selected Project";
        public final int Mnemonic = MvnPesdca.DeleteProjectActionMnemonic;
        public final int Accelerator = MvnPesdca.DeleteProjectActionAccelerator;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public DeleteProjectAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            JMenuItem item = menu.add(this);
            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            mvnp.swapContentPane(getProjectsPanel());
            Project project = projectsPanel.getSelectedProject();
            if(project == null)
            {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            if(JOptionPane.showConfirmDialog(mvnp, "Delete Project " + project.toString(), "Delete Project", JOptionPane.YES_NO_OPTION) != JOptionPane.OK_OPTION)
                return;
            projects.deleteProject(project);
            DefaultListModel<Project> listModel = projects.getProjectListModel();
            projectsPanel.setProjectListModel(listModel, listModel.getElementAt(0));
        }
    }

    final class BuildProjectAction extends AbstractAction
    {
        private static final long serialVersionUID = 3216312770709024820L;
        public final String Image = "exit.gif";
        public static final String Label = "Build...";
        public static final String MouseOver = "Build a project or set of projects";
        public final int Mnemonic = MvnPesdca.BuildProjectActionMnemonic;
        public final int Accelerator = MvnPesdca.BuildProjectActionAccelerator;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public BuildProjectAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            JMenuItem item = menu.add(this);
            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            mvnp.swapContentPane(new NotDoneYetPanel("BuildProjectAction"));
        }
    }

    final class EnvProjectAction extends AbstractAction
    {
        private static final long serialVersionUID = -8215918142793379328L;
        public final String Image = "exit.gif";
        public static final String Label = "Environment...";
        public static final String MouseOver = "Set basic workstation/workspace variables";
        public final int Mnemonic = MvnPesdca.EnvProjectActionMnemonic;
        public final int Accelerator = MvnPesdca.EnvProjectActionAccelerator;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public EnvProjectAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            JMenuItem item = menu.add(this);
            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            mvnp.swapContentPane(new EnvPanel(mvnp, true));
        }
    }

    public class ProjectsPopUpMenu extends JPopupMenu implements MouseListener
    {
        private static final long serialVersionUID = -3477989787687436453L;
        public ProjectsPopUpMenu()
        {
            add(addProjectMenuItem);
            add(deleteProjectMenuItem);
            add(buildProjectMenuItem);
            add(envProjectMenuItem);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            maybeShow(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            maybeShow(e);
        }
        
        private void maybeShow(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                show(e.getComponent(), e.getX(), e.getY());
//                mainPanel.repaint();
            }
        }

        // @formatter:off
        @Override public void mouseClicked(MouseEvent e){/* nothing to do here*/}
        @Override public void mouseEntered(MouseEvent e){/* nothing to do here*/}
        @Override public void mouseExited(MouseEvent e) {/* nothing to do here*/}
        // @formatter:on
    }
}
